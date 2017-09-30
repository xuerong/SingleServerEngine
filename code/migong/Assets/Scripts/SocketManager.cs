using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System.Net.Sockets;  
using System.Net;  
using System.IO;
using System;
using System.Threading;
using Example;
using com.protocol;

public delegate void ActionForReceive(int opcode,byte[] data);

public class SyncObject{
	public volatile int Opcode;
	public volatile byte[] Data;
}

public class SocketManager : MonoBehaviour {
	/**
	 * BlockingQueue 用来发包
	 * Queue queue = Queue.Synchronized (new Queue ());
	 * Queue 当count>0的时候加锁取值，去锁执行
	 */

	private static int idIndex = 0;

	private static Socket clientSocket;  
	//是否已连接的标识  
	public static bool IsConnected = false;


	static volatile Dictionary<int,ActionForReceive> dic = new Dictionary<int,ActionForReceive>();
//	static volatile Dictionary<int,ActionForReceive> invoke = new Dictionary<int,ActionForReceive>();
	static volatile Dictionary<int,SyncObject> syncObjects = new Dictionary<int,SyncObject>();

	private static System.Object locker = new System.Object ();

	// Use this for initialization
	void Start () {
		if (!IsConnected) {
			ConnectServerAndLogin ();
		}
	}

	public static void ConnectServerAndLogin(){
		lock (locker) {
			if (ConnectServer ()) {
				CSLogin node = new CSLogin ();
				node.AccountId = "asdfadf";
				node.Url = "sdf";
				node.Ip = "127.0.0.1";
				byte[] data = CSLogin.SerializeToBytes (node);
				byte[] loginData = SocketManager.SendMessageSync ((int)AccountOpcode.CSLogin, data);
				SCLogin scLogin = SCLogin.Deserialize (loginData);
				Debug.Log ("login success,sessionId = " + scLogin.SessionId);
			}
		}
	}

	/// <summary>  
	/// 连接指定IP和端口的服务器  
	/// </summary>  
	/// <param name="ip"></param>  
	/// <param name="port"></param>  
	public static bool ConnectServer()  
	{  
		if (IsConnected) {
			return IsConnected;
		}
		string ip = "127.0.0.1";
		int port = 8003;
		if (clientSocket == null) {
			clientSocket = new Socket (AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);  
		}
		IPAddress mIp = IPAddress.Parse (ip);  
		IPEndPoint ip_end_point = new IPEndPoint (mIp, port);  

		try {  
			clientSocket.Connect (ip_end_point);  
			IsConnected = true;  
			Debug.Log ("连接服务器成功");  
		} catch {  
			IsConnected = false;  
			Debug.Log ("连接服务器失败");  
			return IsConnected;  
		} 
		if (IsConnected) {
			Thread receiveThread = new Thread (new ThreadStart (_onReceiveSocket));
			receiveThread.IsBackground = true;
			receiveThread.Start ();
		}
		return IsConnected;  
	}  
	public static void CloseConnect(){
		lock (locker) {
			if (clientSocket != null && clientSocket.Connected) {
				clientSocket.Close ();
				IsConnected = false;
			}
		}
	}

	/// <summary>
	/// 接受网络数据
	/// </summary>
	private static void _onReceiveSocket()
	{
		byte[] _tmpReceiveBuff = new byte[1024];
		ByteBuffer buf = ByteBuffer.Allocate (10240);
		int length = 0;
		bool isReadHead = false;

		int size = 0;
		// 
		int opcode = 0;
		int id = 0;
		while (true)
		{
			if (!clientSocket.Connected)
			{
				//				_isConnected = false;
				//				_ReConnect();

				break;
			}
			try
			{
				int receiveLength = clientSocket.Receive(_tmpReceiveBuff);
				if (receiveLength > 0)
				{
					buf.WriteBytes(_tmpReceiveBuff);
					length += receiveLength;
					if(length < 12){
						continue;
					}

					if(!isReadHead){
						size = buf.ReadInt();
						opcode = buf.ReadInt();
						id = buf.ReadInt();
						Debug.Log("size:"+size+",opcode:"+opcode+",id:"+id);
						isReadHead = true;
						if(size > length-12){
							continue;
						}
					}
					if(size > length-12){
						continue;
					}

					byte[] data = new byte[size];
					buf.ReadBytes(data,0,size);
					buf.Clear();
					isReadHead = false;
					length = 0;
					if(dic.ContainsKey(id)){
						if(opcode == (int)BaseOpcode.SCException){
							SCException exception = SCException.Deserialize(data);
							Debug.LogError("error:errorCode = "+exception.ErrCode+",errorMsg = "+exception.ErrMsg);
						}else{
							ActionForReceive action = dic[id];
							if(action != null){
								action.Invoke(opcode,data);
							}
						}
						dic.Remove(id);
					}else if(syncObjects.ContainsKey(id)){
						if(opcode == (int)BaseOpcode.SCException){
							SCException exception = SCException.Deserialize(data);
//							Debug.LogError("error:errorCode = "+exception.ErrCode+",errorMsg = "+exception.ErrMsg);
						}else{
							
						}
						SyncObject syncObject = syncObjects[id];
						Monitor.Enter(syncObject);
						syncObject.Data = data;
						syncObject.Opcode = opcode;
						syncObjects.Remove(id);
						Monitor.Pulse(syncObject);
						Monitor.Exit(syncObject);
					}else{
						// 推送消息
						if(opcode == (int)BaseOpcode.SCException){
							SCException exception = SCException.Deserialize(data);
							Debug.LogError("error:errorCode = "+exception.ErrCode+",errorMsg = "+exception.ErrMsg);
						}else{
							Debug.Log("opcode = "+opcode+",");
						}
					}
				}
			}
			catch (System.Exception e)
			{
				Debug.Log("e:"+e);
				clientSocket.Disconnect(true);
				clientSocket.Shutdown(SocketShutdown.Both);
				clientSocket.Close();
				break;
			}
		}
	}

	public static void SendMessageAsyc(int opcode ,byte[] data,ActionForReceive action){
		// 
		if (IsConnected == false)  {
			ConnectServerAndLogin ();
			if (!IsConnected) {
				throw new Exception("server is not connect");
			}
		}
		try  
		{  	
			int id = Interlocked.Increment(ref idIndex);
			ByteBuffer buffer = ByteBuffer.Allocate(512);  
			buffer.WriteInt(data.Length);
			buffer.WriteInt(opcode);
			buffer.WriteInt(id);
			buffer.WriteBytes(data);
			byte[] sendData = buffer.ToArray();

			clientSocket.Send(sendData);  

			dic.Add(id,action);

			Debug.Log("send success,size = "+data.Length+",id:"+id);
		}  
		catch(Exception e)
		{  
			Debug.Log("send fail,"+e);
//			IsConnected = false;  
//			clientSocket.Shutdown(SocketShutdown.Both);  
//			clientSocket.Close();  
			throw new Exception(e.Message);
		}  
	}

	public static byte[] SendMessageSync(int opcode ,byte[] data){
		if (IsConnected == false)  {
			ConnectServerAndLogin ();
			if (!IsConnected) {
				throw new Exception("server is not connect");
			}
		}
		try  
		{  
			int id = Interlocked.Increment(ref idIndex);
			ByteBuffer buffer = ByteBuffer.Allocate(512);  
			buffer.WriteInt(data.Length);
			buffer.WriteInt(opcode);
			buffer.WriteInt(id);
			buffer.WriteBytes(data);
			byte[] sendData = buffer.ToArray();
			clientSocket.Send(sendData);  
			SyncObject syncObject = new SyncObject();
			syncObjects.Add(id,syncObject);
			Debug.Log("send success,size = "+data.Length);
			Monitor.Enter(syncObject);
			if(!Monitor.Wait(syncObject,1000)){
				syncObjects.Remove(id);
			}
			Monitor.Exit(syncObject);
			if(syncObject.Opcode == (int)BaseOpcode.SCException){
				SCException exception = SCException.Deserialize(syncObject.Data);
//				Debug.LogError("error:errorCode = "+exception.ErrCode+",errorMsg = "+exception.ErrMsg);
				// TODO 弹出窗口，抛出异常
				throw new Exception(exception.ErrCode+":"+exception.ErrMsg);
			}
			return syncObject.Data;
		}  
		catch(Exception e)
		{  
			
//			IsConnected = false;  
//			clientSocket.Shutdown(SocketShutdown.Both);  
//			clientSocket.Close();  
			Debug.Log("send fail:"+e);
			throw new Exception(e.Message);
		}  
		return null;
	}

	public static void SendMessage(int id,int opcode ,byte[] data){
		// 
		if (IsConnected == false)  {
			ConnectServerAndLogin ();
			if (!IsConnected) {
				throw new Exception("server is not connect");
			}
		}
		try  
		{  
			ByteBuffer buffer = ByteBuffer.Allocate(512);  
			buffer.WriteInt(data.Length);
			buffer.WriteInt(opcode);
			buffer.WriteInt(id);
			buffer.WriteBytes(data);
			byte[] sendData = buffer.ToArray();

			clientSocket.Send(sendData);  
			//			Debug.Log("send success,size = "+data.Length);
			//			Monitor.Wait();
		}  
		catch (Exception e)
		{  
//			IsConnected = false;  
//			clientSocket.Shutdown(SocketShutdown.Both);  
//			clientSocket.Close();  
			Debug.Log("send fail");
			throw new Exception(e.Message);
		}  
	}
	void OnDestroy () {
		SocketManager.CloseConnect ();
		Debug.Log("close connect");  
	}
}

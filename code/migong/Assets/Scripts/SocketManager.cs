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

public class AsyncObject{
	public volatile int Opcode;
	public volatile byte[] Data;
	public volatile ActionForReceive action;
}

public class SocketManager : MonoBehaviour {
	public static string ACCOUNT_ID = "asdfadf668";
	public static int MAX_SEND_COUNT = 5; // 一次最大发送消息
	/**
	 * BlockingQueue 用来发包
	 * Queue queue = Queue.Synchronized (new Queue ());
	 * Queue 当count>0的时候加锁取值，去锁执行
	 */

	private static int idIndex = 0;

	private static Socket clientSocket;  
	//是否已连接的标识  
	public static volatile bool IsConnected = false;
	public static volatile bool IsConnecting = false;
	public static volatile bool IsLogin = false;

	// 发送消息的queue
	static Queue<AsyncObject> sendQueue = new Queue<AsyncObject>();
	// 后端推送消息的处理
	static volatile Dictionary<int,ActionForReceive> dic = new Dictionary<int,ActionForReceive>();
	// 执行dic中的东西
	static Queue<AsyncObject> invokeQueue = new Queue<AsyncObject>();
//	static volatile Dictionary<int,ActionForReceive> invoke = new Dictionary<int,ActionForReceive>();
	static volatile Dictionary<int,SyncObject> syncObjects = new Dictionary<int,SyncObject>();
	// 推送消息的处理
	static volatile Dictionary<int,ActionForReceive> serverSendData = new Dictionary<int,ActionForReceive>();
	//
	private static System.Object locker = new System.Object ();

	// Use this for initialization
	void Awake () {
		if (!IsConnected) {
			ConnectServerAndLogin ();
		}
	}

	void Update(){
		// 发送
		if (IsConnected && IsLogin) {
			if (sendQueue.Count > 0) {
				int count = 0;
				while (sendQueue.Count > 0 && count++ < MAX_SEND_COUNT) {
					AsyncObject asyncObject = sendQueue.Dequeue ();
					_SendMessageAsyc (asyncObject.Opcode, asyncObject.Data, asyncObject.action);
				}
			}
		} else {
			if (sendQueue.Count > 0) { // 除了启动时的连接，有发送消息的时候再连
				ConnectServerAndLogin ();
			}
		}
		// 接收的处理
		if (invokeQueue.Count > 0) {
			lock (invokeQueue) {
				while (invokeQueue.Count > 0) {
					AsyncObject asyncObject = invokeQueue.Dequeue ();
					asyncObject.action.Invoke (asyncObject.Opcode,asyncObject.Data);
				}
			}
		}
	}

	public static void AddServerSendReceive(int opcode,ActionForReceive actionFroReceive){
		if (!serverSendData.ContainsKey (opcode)) {
			serverSendData.Add (opcode, actionFroReceive);
		}
	}

	public static void ConnectServerAndLogin(){
		lock (locker) {
			if (!IsConnected && !IsConnecting) {
				IsConnecting = true;

				Thread connectThread = new Thread (new ThreadStart (delegate() {
					if (ConnectServer ()) {
						IsConnected = true;
						CSLogin node = new CSLogin ();
						node.AccountId = ACCOUNT_ID;
						node.Url = "sdf";
						node.Ip = "127.0.0.1";
						byte[] data = CSLogin.SerializeToBytes (node);
						byte[] loginData = SocketManager.SendMessageSync ((int)AccountOpcode.CSLogin, data);
						SCLogin scLogin = SCLogin.Deserialize (loginData);
						Debug.Log ("login success,sessionId = " + scLogin.SessionId);
						IsLogin = true;
					}
					IsConnecting = false;
				}));
				connectThread.IsBackground = true;
				connectThread.Start ();
			}
		}
	}

	/// <summary>  
	/// 连接指定IP和端口的服务器  
	/// </summary>  
	/// <param name="ip"></param>  
	/// <param name="port"></param>  
	private static bool ConnectServer()  
	{  
		bool ret = false;
		string ip = "127.0.0.1";
		int port = 8003;
		if (clientSocket == null) {
			clientSocket = new Socket (AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);  
		}
		IPAddress mIp = IPAddress.Parse (ip);  
		IPEndPoint ip_end_point = new IPEndPoint (mIp, port);  

		clientSocket.SendTimeout = 2000;
//		clientSocket.ReceiveTimeout = 2000;
		clientSocket.SendBufferSize = 81920;
		clientSocket.ReceiveBufferSize = 81920;
		try {
			clientSocket.Connect (ip_end_point);  
			ret = true;  
			Debug.Log ("连接服务器成功");  
		} catch (Exception e){  
			ret = false;  
			Debug.Log ("连接服务器失败"+e);  
			WarnDialog.showWarnDialog ("连接服务器失败1",delegate() {
//				ConnectServer();
			});
			return ret;  
		} 
		if (ret) {
			Thread receiveThread = new Thread (new ThreadStart (_onReceiveSocket));
			receiveThread.IsBackground = true;
			receiveThread.Start ();
		}
		return ret;  
	}  
	public static void CloseConnect(){
		lock (locker) {
			if (clientSocket != null && clientSocket.Connected) {
				clientSocket.Close ();
				IsConnected = false;
				IsLogin = false;
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
//						Debug.Log("size:"+size+",opcode:"+opcode+",id:"+id);
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
							resoveSCException(exception);
						}else{
							ActionForReceive action = dic[id];
							if(action!=null){
								AsyncObject o = new AsyncObject();
								o.Opcode = opcode;
								o.Data = data;
								o.action = action;
								invokeQueue.Enqueue(o);
							}
						}
						dic.Remove(id);
					}else if(syncObjects.ContainsKey(id)){
						if(opcode == (int)BaseOpcode.SCException){
							SCException exception = SCException.Deserialize(data);
							resoveSCException(exception);
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
							resoveSCException(exception);
						}else{
							if(serverSendData.ContainsKey(opcode)){
								ActionForReceive action = serverSendData[opcode];
								AsyncObject o = new AsyncObject();
								o.Opcode = opcode;
								o.Data = data;
								o.action = action;
								invokeQueue.Enqueue(o);
							}
//							Debug.Log("opcode = "+opcode+",");
						}
					}
				}
			}
			catch (System.Exception e)
			{
				WarnDialog.showWarnDialog ("无法连接到服务器",delegate() {
//									ConnectServer();
				});
				Debug.Log("e:"+e);
				clientSocket.Disconnect(true);
				clientSocket.Shutdown(SocketShutdown.Both);
				clientSocket.Close();
				break;
			}
		}
	}

	private static void resoveSCException(SCException exception){
		Debug.LogError("error:errorCode = "+exception.ErrCode+",errorMsg = "+exception.ErrMsg+",csOpcode:"+exception.CsOpcode+",scOpcode:"+exception.ScOpcode);
		WarnDialog.showWarnDialog ("数据错误："+exception.ErrMsg,delegate() {
			//				ConnectServer();
		});
	}

	public static void SendMessageAsyc(int opcode ,byte[] data,ActionForReceive action){
		AsyncObject asyncObject = new AsyncObject ();
		asyncObject.action = action;
		asyncObject.Data = data;
		asyncObject.Opcode = opcode;
		sendQueue.Enqueue (asyncObject);
	}

	private static void _SendMessageAsyc(int opcode ,byte[] data,ActionForReceive action){
		// 
//		if (IsConnected == false)  {
//			ConnectServerAndLogin ();
//			if (!IsConnected) {
//				throw new Exception("server is not connect");
//			}
//		}
		try  
		{  	
			int id = Interlocked.Increment(ref idIndex);
			ByteBuffer buffer = ByteBuffer.Allocate(data.Length+12);  
			buffer.WriteInt(data.Length);
			buffer.WriteInt(opcode);
			buffer.WriteInt(id);
			buffer.WriteBytes(data);
			byte[] sendData = buffer.ToArray();

			clientSocket.Send(sendData);  

			dic.Add(id,action);

//			Debug.Log("send success,size = "+data.Length+",id:"+id);
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
	/**
	 * 不要在主线程用，会阻塞主线程
	 * 如果需要可以在主线程调用的同步网络io，可以考虑用协程做
	 **/
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
	}

	void OnDestroy () {
		SocketManager.CloseConnect ();
		Debug.Log("close connect");  
	}
}

using UnityEngine;  
using System.Collections;  
using System.Net;  
using System.Net.Sockets;  
using System.IO;
using System;
using System.Threading;
using System.Collections.Generic; 

public delegate void ActionForReceive(int opcode,byte[] data);

public class ClientSocket  
{  
	private static byte[] result = new byte[1024];  
	private static Socket clientSocket;  
	//是否已连接的标识  
	public bool IsConnected = false;  


	Dictionary<int,ActionForReceive> dic = new Dictionary<int,ActionForReceive>();


	public ClientSocket(){  
		clientSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);  
	}  

	/// <summary>  
	/// 连接指定IP和端口的服务器  
	/// </summary>  
	/// <param name="ip"></param>  
	/// <param name="port"></param>  
	public void ConnectServer(string ip,int port)  
	{  
//		try
//		{
//			clientSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);//创建套接字
//			IPAddress ipAddress = IPAddress.Parse(ip);//解析IP地址
//			IPEndPoint ipEndpoint = new IPEndPoint(ipAddress, port);
//			IAsyncResult result = clientSocket.BeginConnect(ipEndpoint, new AsyncCallback(_onConnect_Sucess), clientSocket);//异步连接
//			bool success = result.AsyncWaitHandle.WaitOne(5000, true);
//			if (!success) //超时
//			{
//				_onConnect_Outtime();
//			}
//		}
//		catch (System.Exception _e)
//		{
////			_onConnect_Fail();
//		}
		IPAddress mIp = IPAddress.Parse(ip);  
		IPEndPoint ip_end_point = new IPEndPoint(mIp, port);  

		try {  
//			IAsyncResult asyncResult = clientSocket.BeginConnect(ip_end_point,new System.AsyncCallback(_onConnect_Success),clientSocket);//异步连接
//			bool success = asyncResult.AsyncWaitHandle.WaitOne(5000,true);
//			if(!success){
//				
//			}
			clientSocket.Connect(ip_end_point);  
			IsConnected = true;  
			Debug.Log("连接服务器成功");  
		}  
		catch  
		{  
			IsConnected = false;  
			Debug.Log("连接服务器失败");  
			return;  
		}  
		//服务器下发数据长度  
//		int receiveLength = clientSocket.Receive(result);  
//		ByteBuffer buffer = new ByteBuffer(result);  
//		int len = buffer.ReadShort();  
//		string data = buffer.ReadString();  
//		Debug.Log("服务器返回数据：" + data);  

		Thread receiveThread = new Thread(new ThreadStart(_onReceiveSocket));
		receiveThread.IsBackground = true;
		receiveThread.Start();
	}  
	private void _onConnect_Sucess(IAsyncResult iar)
	{
		try
		{
			Socket client = (Socket)iar.AsyncState;
			client.EndConnect(iar);

			Thread receiveThread = new Thread(new ThreadStart(_onReceiveSocket));
			receiveThread.IsBackground = true;
			receiveThread.Start();
//			_isConnected = true;
			Debug.Log("连接成功");
		}
		catch (Exception _e)
		{
//			Close();
		}
	}
	private void _onConnect_Outtime()
	{
		Debug.Log("连接超时");
//		_close();
	}

	public void CloseConnect(){
		if (clientSocket != null && clientSocket.Connected) {
			clientSocket.Close ();
		}
	}
	/// <summary>
	/// 接受网络数据
	/// </summary>
	private void _onReceiveSocket()
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
					Debug.Log("receiveLength4:"+receiveLength+",id:"+id);
					if(dic.ContainsKey(id)){
						ActionForReceive action = dic[id];
						Debug.Log("receiveLength5:"+receiveLength);
						if(action != null){
							action.Invoke(opcode,data);
							Debug.Log("receiveLength6:"+receiveLength);
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

	/// <summary>  
	/// 发送数据给服务器  
	/// </summary>  
//	public void SendMessage(string data)  
//	{  
//		if (IsConnected == false)  
//			return;  
//		try  
//		{  
//			ByteBuffer buffer = ByteBuffer.Allocate(512);  
//			buffer.WriteString(data);  
//			clientSocket.Send(WriteMessage(buffer.ToBytes()));  
//		}  
//		catch  
//		{  
//			IsConnected = false;  
//			clientSocket.Shutdown(SocketShutdown.Both);  
//			clientSocket.Close();  
//		}  
//	}  
	public void SendMessageAsyc(int id,int opcode ,byte[] data,ActionForReceive action){
		// 
		if (IsConnected == false)  
			return;  
		try  
		{  
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
			IsConnected = false;  
			clientSocket.Shutdown(SocketShutdown.Both);  
			clientSocket.Close();  

		}  
	}
	public void SendMessage(int id,int opcode ,byte[] data){
		// 
		if (IsConnected == false)  
			return;  
		try  
		{  
			ByteBuffer buffer = ByteBuffer.Allocate(512);  
			buffer.WriteInt(data.Length);
			buffer.WriteInt(opcode);
			buffer.WriteInt(id);
			buffer.WriteBytes(data);
			byte[] sendData = buffer.ToArray();

			clientSocket.Send(sendData);  
			Debug.Log("send success,size = "+data.Length);

//			Monitor.Wait();
		}  
		catch  
		{  
			IsConnected = false;  
			clientSocket.Shutdown(SocketShutdown.Both);  
			clientSocket.Close();  
			Debug.Log("send fail");
		}  
	}

	/// <summary>  
	/// 数据转换，网络发送需要两部分数据，一是数据长度，二是主体数据  
	/// </summary>  
	/// <param name="message"></param>  
	/// <returns></returns>  
	private static byte[] WriteMessage(byte[] message)  
	{  
		MemoryStream ms = null;  
		using (ms = new MemoryStream())  
		{  
			ms.Position = 0;  
			BinaryWriter writer = new BinaryWriter(ms);  
			ushort msglen = (ushort)message.Length;  
			writer.Write(msglen);  
			writer.Write(message);  
			writer.Flush();  
			return ms.ToArray();  
		}  
	}  
}  

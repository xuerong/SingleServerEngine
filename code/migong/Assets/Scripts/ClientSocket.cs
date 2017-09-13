using UnityEngine;  
using System.Collections;  
using System.Net;  
using System.Net.Sockets;  
using System.IO;
using System;
using System.Threading; 

public class ClientSocket  
{  
	private static byte[] result = new byte[1024];  
	private static Socket clientSocket;  
	//是否已连接的标识  
	public bool IsConnected = false;  

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

//		Thread receiveThread = new Thread(new ThreadStart(_onReceiveSocket));
//		receiveThread.IsBackground = true;
//		receiveThread.Start();
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
//				int receiveLength = clientSocket.Receive(_tmpReceiveBuff);
//				if (receiveLength > 0)
//				{
//					_databuffer.AddBuffer(_tmpReceiveBuff, receiveLength);//将收到的数据添加到缓存器中
//					while (_databuffer.GetData(out _socketData))//取出一条完整数据
//					{
//						sEvent_NetMessageData tmpNetMessageData = new sEvent_NetMessageData();
//						tmpNetMessageData._eventType = _socketData._protocallType;
//						tmpNetMessageData._eventData = _socketData._data;
//
//						//锁死消息中心消息队列，并添加数据
//						lock (MessageCenter.Instance._netMessageDataQueue)
//						{
//							Debug.Log(tmpNetMessageData._eventType);
//							MessageCenter.Instance._netMessageDataQueue.Enqueue(tmpNetMessageData);
//						}
//					}
//				}
			}
			catch (System.Exception e)
			{
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
	public void SendMessage(string data)  
	{  
		if (IsConnected == false)  
			return;  
		try  
		{  
			ByteBuffer buffer = new ByteBuffer();  
			buffer.WriteString(data);  
			clientSocket.Send(WriteMessage(buffer.ToBytes()));  
		}  
		catch  
		{  
			IsConnected = false;  
			clientSocket.Shutdown(SocketShutdown.Both);  
			clientSocket.Close();  
		}  
	}  
	public void SendMessage(byte[] data){
		// 
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

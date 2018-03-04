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
using System.Net.NetworkInformation;
using UnityEngine.Networking;

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
	public static string ACCOUNT_KEY = "account";
	public static string SERVER_ID_KEY = "server_id";
	public static string SERVER_IP_KEY = "server_ip";
	public static string SERVER_PORT_KEY = "server_port";


	public static int MAX_SEND_COUNT = 5; // 一次最大发送消息

	public static string accountId = "gaorui";
	public static int port=8003;
	public static string ip="10.1.6.254";
	public static int serverId = 1;
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

	private static bool needConnectOnce = false;

	private static string localization = "Chinese";

	private static bool isPaused = false;

	static Dictionary<int,long> lastSendTime = new Dictionary<int, long>();
	static Dictionary<int,int> needCheckOpcode = new Dictionary<int, int>(){ // 
		{(int)MiGongOpcode.CSGetMiGongMap,(int)MiGongOpcode.CSGetMiGongMap},
		{(int)MiGongOpcode.CSUnlimitedGo,(int)MiGongOpcode.CSUnlimitedGo},
		{(int)MiGongOpcode.CSMatching,(int)MiGongOpcode.CSMatching},
		{(int)MiGongOpcode.CSUnlimitedInfo,(int)MiGongOpcode.CSUnlimitedInfo},
		{(int)MiGongOpcode.CSGetOnlineInfo,(int)MiGongOpcode.CSGetOnlineInfo},
		{(int)MiGongOpcode.CSGetPassReward,(int)MiGongOpcode.CSGetPassReward}
	};
	// Use this for initialization
	void Awake () {
		localization = Application.systemLanguage.ToString();
//		getServerListAndConnectServerAndLogin ();
		needConnectOnce = true;
//		ConnectServerAndLogin ();
	}


	private void getServerListAndConnectServerAndLogin(){
		PlayerPrefs.DeleteAll ();

		serverId = PlayerPrefs.GetInt (SERVER_ID_KEY);
		ip = PlayerPrefs.GetString (SERVER_IP_KEY);
		port = PlayerPrefs.GetInt (SERVER_PORT_KEY);
		accountId = PlayerPrefs.GetString (ACCOUNT_KEY);

		if (ip == "" || accountId == "") {
			StartCoroutine (PostReq());
		} else {
			if (!IsConnected && !IsConnecting) {
				ConnectServerAndLogin ();
			}
		}
	}

	IEnumerator PostReq () {
		Dictionary<string,string> headers = new Dictionary<string, string> ();
		headers.Add("opcode",(int)AccountOpcode.CSGetLoginInfo+"");
		headers.Add("localization",localization);
		CSGetLoginInfo loginInfo = new CSGetLoginInfo ();
		NetworkInterface[] nis = NetworkInterface.GetAllNetworkInterfaces ();
		foreach (NetworkInterface ni in nis) {  
//			Debug.Log ("Name = " + ni.Name );  
//			Debug.Log ("Description = " + ni.Description );  
//			Debug.Log ("NetworkInterfaceType = " + ni.NetworkInterfaceType.ToString() );  
//			Debug.Log ("Mac地址 = " + ni.GetPhysicalAddress().ToString() );  /// Ethernet 6C0B8490F3B5
			if ("Ethernet".Equals (ni.NetworkInterfaceType.ToString ())) {
				loginInfo.DeviceId = "mac-"+ni.GetPhysicalAddress ().ToString ();
				break;
			}
		}  
		if (loginInfo.DeviceId == null || loginInfo.DeviceId == "") {
			loginInfo.DeviceId = SystemInfo.deviceUniqueIdentifier;
		}
		if (loginInfo.DeviceId == null || loginInfo.DeviceId == "") {
			WarnDialog.showWarnDialog ("can not get device id");
			yield return 0.1f;
		}
		Debug.Log ("loginInfo.DeviceId = "+loginInfo.DeviceId);
        //		loginInfo.DeviceId = "shdfshksshfkk";
        //		WWW getData = new WWW("http://10.1.6.254:8083",CSGetLoginInfo.SerializeToBytes(loginInfo),headers);
        //		yield return getData;

        //		if(getData.error!= null){  
        //			Debug.Log (getData.error);
        //			WarnDialog.showWarnDialog ("get server info fail !",null);
        //		}else{ 
        //			SCGetLoginInfo ret = SCGetLoginInfo.Deserialize (getData.bytes);
        //			serverId = ret.ServerId;
        //			accountId = ret.AccountId;
        //			ip = ret.Ip;
        //			port = ret.Port;
        //
        //			PlayerPrefs.SetInt (SERVER_ID_KEY,serverId);
        //			PlayerPrefs.SetString (SERVER_IP_KEY,ip);
        //			PlayerPrefs.SetInt (SERVER_PORT_KEY,port);
        //			PlayerPrefs.SetString (ACCOUNT_KEY,accountId);
        //			PlayerPrefs.Save ();
        //
        //			ConnectServerAndLogin ();
        //
        //			Debug.Log ("accountId = " + ret.AccountId);  
        //			Debug.Log ("serverId = " + ret.ServerId);  
        //			Debug.Log ("ip = " + ret.Ip);  
        //			Debug.Log ("port = " + ret.Port);  
        //		}      
        //
//        string url = "http://127.0.0.1:8083";
        //		string url = "http://10.1.6.254:8083";  
        //		string url = "http://111.230.144.111:8083";  // 腾讯云
        string url = "http://47.95.219.97:8083";  // 阿里云
        UnityWebRequest request = new UnityWebRequest(url, "POST");  
		byte[] postBytes = CSGetLoginInfo.SerializeToBytes(loginInfo);
        Debug.Log(postBytes.Length);
		request.uploadHandler = (UploadHandler)new UploadHandlerRaw(postBytes);
		request.downloadHandler = (DownloadHandler)new DownloadHandlerBuffer();  
		request.SetRequestHeader ("opcode",(int)AccountOpcode.CSGetLoginInfo+"");
        //request.SetRequestHeader("Content-Length", ""+postBytes.Length);
        // 防止系统给出的request.getContentLength() = -1
        request.SetRequestHeader("contentLength", ""+ postBytes.Length);
        yield return request.Send();//.SendWebRequest();
        Debug.Log(request.uploadedBytes);
		if (request.responseCode == 200) {
			SCGetLoginInfo ret = SCGetLoginInfo.Deserialize (request.downloadHandler.data);
			serverId = ret.ServerId;
			accountId = ret.AccountId;
			ip = ret.Ip;
			port = ret.Port;
            switch(ret.ServerState){
                case 0:break;
                case 1:
                    WarnDialog.showWarnDialog("服务器正在维护");
                    break;
            }

			PlayerPrefs.SetInt (SERVER_ID_KEY, serverId);
			PlayerPrefs.SetString (SERVER_IP_KEY, ip);
			PlayerPrefs.SetInt (SERVER_PORT_KEY, port);
			PlayerPrefs.SetString (ACCOUNT_KEY, accountId);
			PlayerPrefs.Save ();
			ConnectServerAndLogin ();
//			Debug.Log ("accountId = " + ret.AccountId);  
//			Debug.Log ("serverId = " + ret.ServerId);  
//			Debug.Log ("ip = " + ret.Ip);  
//			Debug.Log ("port = " + ret.Port);  
		} else {
			Debug.Log ("request.responseCode = "+request.responseCode);
			WarnDialog.showWarnDialog (Message.getText("getServerInfoFail"),null);
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
			if (needConnectOnce) {
				needConnectOnce = false;
				getServerListAndConnectServerAndLogin (); //
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
		if (serverSendData.ContainsKey (opcode)) { // 可以被覆盖
			serverSendData.Remove (opcode);
		}
		serverSendData.Add (opcode, actionFroReceive); 
	}

	public static void ConnectServerAndLogin(){
		lock (locker) {
			if (!IsConnected && !IsConnecting) {
				IsConnecting = true;

				Thread connectThread = new Thread (new ThreadStart (delegate() {
					if (ConnectServer ()) {
						IsConnected = true;
						CSLogin node = new CSLogin ();
						node.AccountId = accountId;
						node.Url = "sdf";
						node.Ip = ip;
						node.Localization = localization;
//						node.Ip = "10.0.2.2";
						byte[] data = CSLogin.SerializeToBytes (node);
						byte[] loginData = SocketManager.SendMessageSync ((int)AccountOpcode.CSLogin, data);
						SCLogin scLogin = SCLogin.Deserialize (loginData);
						Debug.Log ("login success,sessionId = " + scLogin.SessionId);
						if (DateTime.Now.Ticks - scLogin.ServerTime < 3000){
							Params.disFromServerTime = 0;
						}
						else { 
							Params.disFromServerTime = (int)((Util.ConvertDateTimeToInt(DateTime.Now) - scLogin.ServerTime)/1000);
						}
						Debug.Log("disFromServerTime = "+Params.disFromServerTime);
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
		string ip = SocketManager.ip;
		int port = SocketManager.port;
		if (clientSocket == null) {
			clientSocket = new Socket (AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);  
		} else {
			clientSocket.Close ();
			clientSocket = new Socket (AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);  
		}
		IPAddress mIp = IPAddress.Parse (ip);  
		IPEndPoint ip_end_point = new IPEndPoint (mIp, port);  

		clientSocket.SendTimeout = 2000;
//		clientSocket.ReceiveTimeout = 10000; // TODO 加上这个会有问题，是谁没有正确放回呀？难道是因为网络线程是阻塞的，这个阻塞最多等待 ReceiveTimeout秒？
		clientSocket.SendBufferSize = 81920;
		clientSocket.ReceiveBufferSize = 81920;
		try {
			clientSocket.Connect (ip_end_point);  
			ret = true;  
			Debug.Log ("连接服务器成功");  
		} catch (Exception e){  
			ret = false;  
			Debug.Log ("连接服务器失败"+e);  
			WarnDialog.showWarnDialog (Message.getText("connectServerFail"),delegate() {
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
                    if(opcode == (int)MiGongOpcode.SCSendEatBean){
                        SCSendEatBean sc = SCSendEatBean.Deserialize(data);
                        Debug.Log("opcode:MiGongOpcode.SCEatBean"+sc.Beans.Count);
                        foreach (PBEatBeanInfo bean in sc.Beans)
                        {
                            Debug.Log(""+bean.UserId+","+bean.BeanPos); // 谁吃的
                        }
                    }
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
                                lock(invokeQueue){
                                    invokeQueue.Enqueue(o);
                                }
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
                                lock (invokeQueue){
                                    invokeQueue.Enqueue(o);
                                }
							}
//							Debug.Log("opcode = "+opcode+",");
						}
					}
				}
			}
			catch (System.Exception e)
			{
				WarnDialog.showWarnDialog (Message.getText("connectServerFail"),delegate() {
//									ConnectServer();
				});
				Debug.Log("e:"+e);
				IsConnected = false;
				IsConnecting = false;
				clientSocket.Disconnect(true);
				clientSocket.Shutdown(SocketShutdown.Both);
				clientSocket.Close();
				break;
			}
		}
	}

	private static void resoveSCException(SCException exception){
        // 有些exception不弹窗，不报警
        if(exception.ErrCode == ErrorCode.RoomNotExist){
            return;
        }
		Debug.LogError("error:errorCode = "+exception.ErrCode+",errorMsg = "+exception.ErrMsg+",csOpcode:"+exception.CsOpcode+",scOpcode:"+exception.ScOpcode);
		WarnDialog.showWarnDialog (/*Message.getText("dataError")+*/exception.ErrMsg,delegate() {
			//				ConnectServer();
		});
	}

	public static void SendMessageAsyc(int opcode ,byte[] data,ActionForReceive action){
		if (checkRepeatSend (opcode)) {
			Debug.LogWarning ("repead send "+opcode);
			return;
		}
		if (!IsConnected && !IsConnecting) {
			needConnectOnce = true;
		}
		AsyncObject asyncObject = new AsyncObject ();
		asyncObject.action = action;
		asyncObject.Data = data;
		asyncObject.Opcode = opcode;
		sendQueue.Enqueue (asyncObject);
	}

	// 检测重复发送，需要检测重复发送的要记录过来
	private static bool checkRepeatSend(int opcode){
		if (lastSendTime.ContainsKey (opcode)) {
			long lastTime = lastSendTime [opcode];
			if (DateTime.Now.Ticks - lastTime < 1500) {
				return true;
			}
		}
		if (needCheckOpcode.ContainsKey (opcode)) {
			lastSendTime [opcode] = DateTime.Now.Ticks;
		}
		return false;
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
		if (!IsConnected && !IsConnecting)  {
			needConnectOnce = true;
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
//			Debug.Log("send success,size = "+data.Length);
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


	/**
	 * 
	 * 正常进:
OnApplicationFocus, isFocus=True
正常退:
OnApplicationQuit

Home出：
OnApplicationPause, isPause=True
OnApplicationFocus, isFocus=False

Home进：
OnApplicationPause, isPause=False
OnApplicationFocus, _isFocus=True

Kill进程：
当前应用双击Home，然后Kill：
OnApplicationQuit  (IOS 有回调，android 没回调)

跳出当前应用，然后Kill：
OnApplicationQuit  (IOS和Android都没回调)
	 */
	void OnApplicationFocus(bool hasFocus)
	{
		Debug.Log ("OnApplicationPause ,hasFocus="+hasFocus);
		isPaused = !hasFocus;
		if (hasFocus) {
			checkNeedReconnect ();
		}
	}

	void OnApplicationPause(bool pauseStatus)
	{
		Debug.Log ("OnApplicationPause ,pauseStatus="+pauseStatus);
		isPaused = pauseStatus;
		if (!pauseStatus) {
			checkNeedReconnect ();
		}
	}

	void checkNeedReconnect(){
		if (IsConnected && !clientSocket.Connected) {
			IsConnected = false;
		}
	}
}

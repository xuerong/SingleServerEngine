using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using Example;
using com.protocol;
using System.Threading;

public class SocketTest : MonoBehaviour {
	ClientSocket mSocket;
	// Use this for initialization  
	void Start () {  
//		mSocket = new ClientSocket();  
//		mSocket.ConnectServer("10.1.6.254", 8003);  
////		mSocket.SendMessage("服务器傻逼！");  
//
//		CSLogin node = new CSLogin ();
//		node.AccountId = "asdfadf";
//		node.Url = "sdf";
//		node.Ip = "10.1.6.254";
//		byte[] data = CSLogin.SerializeToBytes (node);
//		mSocket.SendMessage (10,(int)AccountOpcode.CSLogin,data);
//		Thread.Sleep (100);
//
//		CSGetMiGongMap miGongMap = new CSGetMiGongMap ();
//		data = CSGetMiGongMap.SerializeToBytes (miGongMap);
//		mSocket.SendMessageAsyc (10,(int)MiGongOpcode.CSGetMiGongMap,data,((_opcode, _data) => {
//			SCGetMiGongMap map = SCGetMiGongMap.Deserialize(_data);
//			int[] mapInt = map.Map.ToArray();
//			Debug.Log("map length:"+mapInt.Length);
//		}));
	}  

	// Update is called once per frame  
	void Update () {  

	}

	void OnDestroy () {
		if (mSocket != null) {
			mSocket.CloseConnect ();
			Debug.Log("close connect");  
		}
	}
}

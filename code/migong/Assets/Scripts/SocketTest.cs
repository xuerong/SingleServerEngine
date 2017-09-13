using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using Example;
using com.protocol;

public class SocketTest : MonoBehaviour {
	ClientSocket mSocket;
	// Use this for initialization  
	void Start () {  
		mSocket = new ClientSocket();  
		mSocket.ConnectServer("127.0.0.1", 8002);  
//		mSocket.SendMessage("服务器傻逼！");  

		CSLoginNode node = new CSLoginNode ();
		node.AccountId = "asdfadf";
		byte[] data = CSLoginNode.SerializeToBytes (node);
		mSocket.SendMessage ();
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

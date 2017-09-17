using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using Example;
using com.protocol;
using System.Threading;

public class MapCreate : MonoBehaviour {
	public GameObject camera;
	ClientSocket mSocket;
	public int[][] map = new int[][]{ 
		new int[]{0,2	,2,	2,		2},
		new int[]{1,1	,0,	2,		1},
		new int[]{1,1,	2,	2	,	1},
		new int[]{1,0,	1,	0,		1},
		new int[]{1,3,	2,	3,		3}
	};

	int x = 0,y = 0;
	static float myScale = 0.2f;
	static float nodeX = 1.9f * myScale,nodeY = 1.9f * myScale;
	// Use this for initialization
	bool canCreate = false;
	bool hasCreate = false;
	void Start () {

		mSocket = new ClientSocket();  
		mSocket.ConnectServer("127.0.0.1", 8003);  
		//		mSocket.SendMessage("服务器傻逼！");  

		CSLogin node = new CSLogin ();
		node.AccountId = "asdfadf";
		node.Url = "sdf";
		node.Ip = "127.0.0.1";
		byte[] data = CSLogin.SerializeToBytes (node);
		mSocket.SendMessage (10,(int)AccountOpcode.CSLogin,data);
		Thread.Sleep (100);

		CSGetMiGongMap miGongMap = new CSGetMiGongMap ();
		data = CSGetMiGongMap.SerializeToBytes (miGongMap);
		mSocket.SendMessageAsyc (10,(int)MiGongOpcode.CSGetMiGongMap,data,((_opcode, _data) => {
			
			SCGetMiGongMap scmap = SCGetMiGongMap.Deserialize(_data);
			int[] mapInt = scmap.Map.ToArray();
			int size = (int)Mathf.Sqrt(mapInt.Length);
			map = new int[size][];
			for(int i=0;i<size;i++){
				map[i] = new int[size];
				for(int j=0;j<size;j++){
					map[i][j] = mapInt[i*size+j];
				}
			}
			Debug.Log("map size:"+size);
//			createMap();
			canCreate = true;
		}));


	}
	// 注意顺序，从上向下，从下向上
	public int getPointByPosition(Vector2 pos){
		int x = (int)(pos.x / nodeX);
		int y = (int)(pos.y / nodeY);
		int tr=map.Length;
		int i = (tr - y - 1);
		int td=map[0].Length;
		return i * td + x;
	}

	private void createMap(){
		//		transform.localScale = new Vector3 (0.1f,0.1f,1); // 这样为啥不行
		int tr=map.Length;											//行数和列数
		int td=map[0].Length;
		Object down = Resources.Load ("down");
		Object right = Resources.Load ("right");
		for (int i = 0; i < tr; i++) {										//绘制墙
			for (int j = 0; j < td; j++) {
				//				if ((map[i][j] & 8) == 8) {								//注意这里的为运算的用法
				//					int w = tr - i-1;
				////					g.drawLine (x + j * nodeX, y + i * nodeY, x + (j + 1) * nodeX, y + i * nodeY);
				//					GameObject up = Instantiate(Resources.Load("up")) as GameObject;
				//					up.transform.parent = transform;
				//					up.transform.position = new Vector3 (x + j * nodeX, y + w * nodeY,0);
				//					up.transform.localScale = new Vector3 (0.1f,0.1f,1);
				//				}
				//				if ((map[i][j] & 4) == 4) {
				////					g.drawLine (x + j * nodeX, y + i * nodeY, x + j * nodeX, y + (i + 1) * nodeY);
				//					int w = tr - i-1;
				//					GameObject up = Instantiate(Resources.Load("left")) as GameObject;
				//					up.transform.parent = transform;
				//					up.transform.position = new Vector3 (x + j * nodeX, y + w * nodeY,0);
				//					up.transform.localScale = new Vector3 (0.1f,0.1f,1);
				//				}
				if ((map[i][j] & 2) == 2) {
					//					g.drawLine (x + j * nodeX, y + (i + 1) * nodeY, x + (j + 1) * nodeX, y + (i + 1) * nodeY);
					int w = tr - i-1;
					GameObject up = Instantiate(down) as GameObject;
					up.transform.parent = transform;
					up.transform.position = new Vector3 (x + j * nodeX, y + w * nodeY,0);
					up.transform.localScale = new Vector3 (myScale,myScale,1);
				}
				if ((map[i][j] & 1) == 1) {
					//					g.drawLine (x + (j + 1) * nodeX, y + i * nodeY, x + (j + 1) * nodeX, y + (i + 1) * nodeY);
					int w = tr - i-1;
					GameObject up = Instantiate(right) as GameObject;
					up.transform.parent = transform;
					up.transform.position = new Vector3 (x + j * nodeX, y + w * nodeY,0);
					up.transform.localScale = new Vector3 (myScale,myScale,1);
				}
			}
		}
		// 修改相机位置
		camera.transform.position = new Vector3(nodeX*(tr-1)/2,nodeY * (td-1)/2,camera.transform.position.z);
		// 修改相机大小
//		camera.transform.localScale = new Vector3(Mathf.Max(tr,td)/20,Mathf.Max(tr,td)/20,camera.transform.localScale.z);
		//
		Camera ca = camera.GetComponent<Camera>();
		ca.orthographicSize = ca.orthographicSize * Mathf.Max (tr, td) / 20;
	}
	// Update is called once per frame
	void Update () {
		if (canCreate && !hasCreate) {
			createMap ();
			hasCreate = true;
		}
	}

	void OnDestroy () {
		if (mSocket != null) {
			mSocket.CloseConnect ();
			Debug.Log("close connect");  
		}
	}
}

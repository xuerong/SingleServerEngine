using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using Example;
using com.protocol;
using System.Threading;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public class MapCreate : MonoBehaviour{
	public Button closeButton;
	public Button okButton;

	public int[][] map = new int[][]{ 
		new int[]{0,2	,2,	2,		2},
		new int[]{1,1	,0,	2,		1},
		new int[]{1,1,	2,	2	,	1},
		new int[]{1,0,	1,	0,		1},
		new int[]{1,3,	2,	3,		3}
	};

	public int Mode; // 0单机，2pvp

	public int Level;
	public int Pass;

	public Rect mapRect;

	public Vector2 startPoint;
	public Vector2 endPoint;
	public int size;


	int x = 0,y = 0;
	static float myScale = 1f;

	public float nodeX = 0,nodeY = 0;

	Camera ca;
	float defaultOrthographicSize = 5;

	int tr = 0,td = 0;

	public Dictionary<string,Pacman> pacmanMap = new Dictionary<string, Pacman> ();
	public List<CircleCollider2D> pacmanColliders = new List<CircleCollider2D> ();
	// Use this for initialization
	void Start () {
		// 设置button
		closeButton.onClick.AddListener(delegate {
			passFinish(false,null);
			Destroy(transform.parent.gameObject);
			GameObject mainGo = GameObject.Find ("main");
			MainPanel mainPanel = mainGo.GetComponent<MainPanel>();
			mainPanel.showMainPanel();;
		});
		okButton.onClick.AddListener(delegate {
			Destroy(transform.parent.gameObject);
			GameObject mainGo = GameObject.Find ("main");
			MainPanel mainPanel = mainGo.GetComponent<MainPanel>();
			mainPanel.showMainPanel();;
		});
		// 0.21 碰撞体的宽，1.9碰撞体的长
		float wallWidth = 0.21f * myScale;
		nodeX = 1.9f * myScale - wallWidth*2 ;nodeY = 1.9f * myScale - wallWidth*2;

		createMap ();
	}
	// 注意顺序，从上向下，从下向上
	public int getPointByPosition(Vector2 pos){
		int x = (int)((pos.x + nodeX / 2) / nodeX);
		int y = (int)((pos.y + nodeY / 2) / nodeY);
		int i = (tr - y - 1);
		return i * td + x;
	}

	private void createMap(){
		//		transform.localScale = new Vector3 (0.1f,0.1f,1); // 这样为啥不行
		tr=map.Length;											//行数和列数
		td=map[0].Length;
		mapRect = new Rect (x, y, td * nodeX, tr * nodeY);

		Object down = Resources.Load ("down");
		Object right = Resources.Load ("right");
		for (int i = 0; i < tr; i++) {										//绘制墙
			for (int j = 0; j < td; j++) {
				if ((map[i][j] & 2) == 2) {
					int w = tr - i-1;
					GameObject up = Instantiate(down) as GameObject;
					up.transform.parent = transform;
					up.transform.position = new Vector3 (x + j * nodeX, y + w * nodeY,0);
					up.transform.localScale = new Vector3 (myScale,myScale,1);
				}
				if ((map[i][j] & 1) == 1) {
					int w = tr - i-1;
					GameObject up = Instantiate(right) as GameObject;
					up.transform.parent = transform;
					up.transform.position = new Vector3 (x + j * nodeX, y + w * nodeY,0);
					up.transform.localScale = new Vector3 (myScale,myScale,1);
				}
			}
		}
		// 修改相机位置
		GameObject camera = transform.parent.Find("mapCamera").gameObject;
		camera.transform.position = new Vector3(nodeX*(tr-1)/2,nodeY * (td-1)/2,camera.transform.position.z);
		// 修改相机大小
		ca = camera.GetComponent<Camera>();
		defaultOrthographicSize = ca.orthographicSize;
		ca.orthographicSize = defaultOrthographicSize * Mathf.Max (tr, td) / 20;
		// 设置终点特效
		transform.parent.Find("endEffect").localPosition = new Vector3(endPoint.x*nodeX,(tr - endPoint.y - 1) * nodeY,0);
	}

	public void OnSliderChange(float value){
		ca.orthographicSize = defaultOrthographicSize * Mathf.Max (tr, td) / 10 * (value *0.5f+0.25f);
	}

	public Vector3 getStartPointWithScale(){
		return new Vector3 (startPoint.x*nodeX,(tr - startPoint.y - 1) * nodeY,0);
	}

	public bool checkEndPoint(int curPoint){
		return false;
	}

	public void passFinish(bool success,List<int> route){
		CSPassFinish pf = new CSPassFinish ();
		pf.Success = success ? 1 : 0;
		pf.Level = this.Level;
		pf.Pass = this.Pass;
		if (route == null) {
//			route =
		}
		pf.Route = route;
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSPassFinish, CSPassFinish.SerializeToBytes (pf), delegate(int opcode, byte[] data) {
			
		});
		GameObject settleGo = transform.parent.Find ("Canvas/settle").gameObject;
		settleGo.SetActive (true);
	}

}

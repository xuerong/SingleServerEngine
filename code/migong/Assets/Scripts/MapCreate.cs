using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using Example;
using com.protocol;
using System.Threading;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public enum MapMode{
	Level,
	Unlimited,
	Online
}

public class Bean{
	public int score;
	public GameObject go;
	public bool sendSelf = false; // 这个是防止自己被发送两次的，
	public Bean(int score){
		this.score = score;
	}
}

public class MapCreate : MonoBehaviour{

	public Button closeButton;
	public Button okButton;
	public Slider starSlider;

	public int[][] map = new int[][]{ 
		new int[]{0,2	,2,	2,		2},
		new int[]{1,1	,0,	2,		1},
		new int[]{1,1,	2,	2	,	1},
		new int[]{1,0,	1,	0,		1},
		new int[]{1,3,	2,	3,		3}
	};

	public Bean[][] beanMap;
	public int[] route;
	public bool needGuide = false;

	public MapMode Mode; // 0单机，2pvp

	public int Pass;

	public Rect mapRect;

	public int size;

	public int[] stars;

	public float totalTime;


	int x = 0,y = 0;
	static float myScale = 1f;

	public float nodeX = 0,nodeY = 0;

	Camera ca;
	float defaultOrthographicSize = 5;

	int tr = 0,td = 0;


	float currentTime;
	public Text showTime;

	private bool gameOver = false;


	public Dictionary<string,Pacman> pacmanMap = new Dictionary<string, Pacman> ();
	private Dictionary<string,Text> scoreText = new Dictionary<string, Text> ();
	public List<CircleCollider2D> pacmanColliders = new List<CircleCollider2D> ();
	// Use this for initialization
	void Start () {
		// 设置button
		closeButton.onClick.AddListener(delegate {
			WarnDialog.showWarnDialog(Message.getText("exit?"),delegate {
				selfArrive(false,null,false);
				Destroy(transform.parent.parent.gameObject);
				GameObject mainGo = GameObject.Find ("main");
				MainPanel mainPanel = mainGo.GetComponent<MainPanel>();
				mainPanel.showMainPanel();;
			});
		});
		okButton.onClick.AddListener(delegate {
			Destroy(transform.parent.parent.gameObject);
			GameObject mainGo = GameObject.Find ("main");
			MainPanel mainPanel = mainGo.GetComponent<MainPanel>();
			mainPanel.showMainPanel();
		});
		// 0.21 碰撞体的宽，1.9碰撞体的长
		float wallWidth = 0.13f * myScale;
		nodeX = 1.9f * myScale - wallWidth*2 ;nodeY = 1.9f * myScale - wallWidth*2;

		//
		if (Mode == MapMode.Online) {
			SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCSendEatBean,delegate(int opcode, byte[] data) {
				SCSendEatBean ret = SCSendEatBean.Deserialize(data);
				foreach(PBEatBeanInfo bean in ret.Beans){
					_checkEatBean(bean.UserId,bean.BeanPos); // 谁吃的
				}
			});
		}
		// 注册玩家到达的信息
		SocketManager.AddServerSendReceive((int)MiGongOpcode.SCUserArrived,userArrived);
		SocketManager.AddServerSendReceive((int)MiGongOpcode.SCGameOver,doGameOver);

		// 联网模式
		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCUserMove, userMoveAction);


		currentTime = totalTime;

		createMap ();
	}

	public void Update(){
		if (currentTime == 0 || gameOver) {
			return;
		}
		int oldInt = (int)currentTime;
		currentTime -= Time.deltaTime;
		if (currentTime < 0) {
			currentTime = 0;
		}
		if (oldInt != (int)currentTime) {
			showTime.text = ((int)currentTime).ToString ()+" s";
		}
		if (currentTime == 0) {
			if (Mode == MapMode.Level || Mode == MapMode.Unlimited) {// pvp由服务器结束时间
				//弹出结算界面，发送结束消息
				selfArrive (false, null, true);
			} 
		}
	}

	public void userMoveAction(int opcode, byte[] data){
		SCUserMove userMove = SCUserMove.Deserialize (data);
		foreach(PBUserMoveInfo userMoveInfo in userMove.UserMoveInfos){
			//			userMoveInfo.Frame
			if(userMoveInfo.UserId.Equals(SocketManager.accountId)){
				//				this.Dir = userMoveInfo.Dir;
				//				transform.localPosition = new Vector3 (userMoveInfo.PosX,userMoveInfo.PosY,transform.localPosition.z);
			}else{
				Pacman pacman = pacmanMap [userMoveInfo.UserId];
				pacman.Dir = userMoveInfo.Dir;
				Debug.Log ("receive:"+userMoveInfo.PosX+","+userMoveInfo.PosY);
				pacman.transform.localPosition = new Vector3 (userMoveInfo.PosX,userMoveInfo.PosY,pacman.transform.localPosition.z);
			}
		}
	}

	public void addScoreShow(string userId){
		// 添加计分板
		Object scoreShow = Resources.Load("scoreShow");
		if (Mode == MapMode.Level || Mode == MapMode.Unlimited) {
//			GameObject scoreGo = Instantiate (scoreShow) as GameObject;
//			scoreGo.transform.localScale = new Vector3 (1,1,1);
//			scoreGo.transform.localPosition = new Vector3 (100, -20,0);
//			scoreGo.transform.SetParent(transform.parent.Find("Canvas"),false);
//			scoreText.Add (userId,scoreGo.transform.Find("Text").GetComponent<Text>());
		} else {
			starSlider.transform.parent.gameObject.SetActive (false);
			GameObject scoreGo = Instantiate (scoreShow) as GameObject;
			scoreGo.transform.localScale = new Vector3 (1,1,1);
			scoreGo.transform.localPosition = new Vector3 (20+(180*scoreText.Count),-40,0);
			scoreGo.transform.SetParent(transform.parent.parent.Find("Canvas"),false);
			scoreText.Add (userId,scoreGo.transform.Find("Text").GetComponent<Text>());
		}
	}
	// 注意顺序，从上向下，从下向上
	public int getPointByPosition(Vector2 pos){
		int x = (int)((pos.x + nodeX / 2) / nodeX);
		int y = (int)((pos.y + nodeY / 2) / nodeY);
		int i = (tr - y - 1);
		return i * td + x;
	}
	// 检查吃豆子，如果有就吃掉---根据类型
	public void checkEatBean(int pos){
		if (Mode == MapMode.Level || Mode == MapMode.Unlimited) {
			_checkEatBean (pos);
		} else if (Mode == MapMode.Online){
			if (checkBean (pos)) {
				// 发送服务器
				CSEatBean eatBean = new CSEatBean ();
				eatBean.BeanPos = pos;
				SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSEatBean, CSEatBean.SerializeToBytes (eatBean), delegate(int opcode, byte[] data) {
//					SCEatBean ret = SCEatBean.Deserialize (data);
					// 这里不需要处理，因为有统一推送
				});
				int x = pos / size;
				int y = pos % size;
				beanMap [x] [y].sendSelf = true;
			}
		}
	}
	private void _checkEatBean(int pos){
		_checkEatBean (SocketManager.accountId, pos);
	}
	private void _checkEatBean(string userId,int pos){
		int x = pos / size;
		int y = pos % size;

		if (beanMap [x] [y] != null) {
			Destroy (beanMap [x] [y].go); // 计算在玩家身上，并做出相应的效果
			if (Mode == MapMode.Level || Mode == MapMode.Unlimited) {
				float old = starSlider.value;
				starSlider.value = starSlider.value + beanMap [x] [y].score;
				Text scoreText = starSlider.transform.Find ("score").GetComponent<Text> ();
				scoreText.text = starSlider.value + "";
				// 点亮星星
				for(int i=1;i<5;i++){
					int score = i * 10;
					if (score > old && score <= starSlider.value) { // 0的自然就不会点亮
						// 点亮
						Image image = starSlider.transform.parent.Find ("star" + i).GetComponent<Image>();
						image.color = new Color (255,255,255);
						break;
					}
				}
			}else if(Mode == MapMode.Online){
				scoreText [userId].text = int.Parse (scoreText [userId].text) + beanMap [x] [y].score + "";
			}
			pacmanMap [userId].score += beanMap [x] [y].score;
			beanMap [x] [y] = null;
		}
	}
	private bool checkBean(int pos){
		int x = pos / size;
		int y = pos % size;
		if (beanMap [x] [y] != null && !beanMap [x] [y].sendSelf) {
			return true;
		}
		return false;
	}

	private void createMap(){
		//		transform.localScale = new Vector3 (0.1f,0.1f,1); // 这样为啥不行
		tr=map.Length;											//行数和列数
		td=map[0].Length;
		mapRect = new Rect (x, y, td * nodeX, tr * nodeY);

		Object down = Resources.Load ("down");
		Object right = Resources.Load ("right");

//		Object downShadow = Resources.Load ("downShadow");
//		Object rightShadow = Resources.Load ("rightShadow");

		Object bean1 = Resources.Load ("bean1");
		Object bean5 = Resources.Load ("bean5");
		Object bean10 = Resources.Load ("bean10");
		int maxScore = 0;
		float mapWidth = td * nodeX;
		for (int i = 0; i < tr; i++) {										//绘制墙
			for (int j = 0; j < td; j++) {
				if ((map[i][j] & 2) == 2) {
					int w = tr - i-1;
					GameObject up = Instantiate(down) as GameObject;
					up.transform.parent = transform;
					up.transform.localPosition = new Vector3 (x + j * nodeX, y + w * nodeY,0);
					up.transform.localScale = new Vector3 (myScale,myScale,1);
				}
				if ((map[i][j] & 1) == 1) {
					int w = tr - i-1;
					GameObject up = Instantiate(right) as GameObject;
					up.transform.parent = transform;
					up.transform.localPosition = new Vector3 (x + j * nodeX, y + w * nodeY,0);
					up.transform.localScale = new Vector3 (myScale,myScale,1);
				}
				if (beanMap!= null && beanMap [i] [j] != null) {
					int w = tr - i-1;
					GameObject beanGo = Instantiate(beanMap [i] [j].score == 10?bean10:(beanMap [i] [j].score == 5?bean5:bean1)) as GameObject;
					beanGo.transform.parent = transform;
					beanGo.transform.localPosition = new Vector3 (x + j * nodeX, y + w * nodeY,0);
					beanGo.transform.localScale = new Vector3 (myScale* 0.6f,myScale* 0.6f,1);
					beanMap [i] [j].go = beanGo;

					maxScore += beanMap [i] [j].score;
				}
			}
		}
		// 设置单机版的最大值 和星星的位置
		if (Mode == MapMode.Level || Mode == MapMode.Unlimited) {
			starSlider.maxValue = maxScore;
			starSlider.transform.Find ("maxScore").GetComponent<Text> ().text = "/"+maxScore;
			RectTransform buRec = starSlider.GetComponent<RectTransform> ();
			float perStarDelta = buRec.rect.width * 10 / maxScore; // 每个星星的像素便宜
			for (int i = 1; i < stars.Length+1; i++) {
				RectTransform starRec = starSlider.transform.parent.Find ("star" + i).GetComponent<RectTransform> ();
				if (stars [i-1] <= 0) {
					starRec.gameObject.SetActive (false);
				} else {
					starRec.anchoredPosition = new Vector2 (i * perStarDelta, -40);
				}
			}
		}
		// 设置引导
		if(needGuide){
			initGuide ();
		}
		// 修改相机位置
		GameObject camera = transform.parent.parent.Find("mapCamera").gameObject;
		camera.transform.position = new Vector3(nodeX*(tr)/2,nodeY * (td)/2,camera.transform.position.z);
		// 修改相机大小
		ca = camera.GetComponent<Camera>();
		defaultOrthographicSize = ca.orthographicSize;
//		ca.orthographicSize = defaultOrthographicSize * Mathf.Max (tr, td) / mapWidth;
		float hwRate = (float)Screen.height/Screen.width;
		ca.orthographicSize = defaultOrthographicSize * Mathf.Max (tr, td) /1.2f * hwRate;

		//
//		float defaultRate = 
//		float hwRate = (float)Screen.height/Screen.width;



		Debug.Log("width:"+Screen.width+",height:"+Screen.height);
	}

	public void setEndEffect(int x,int y){
		// 设置终点特效
		GameObject endEffectGo = Instantiate(Resources.Load("endEffect")) as GameObject;
		endEffectGo.transform.parent = transform.parent;
		float localX = y * nodeY;
		float localY = (tr - x - 1)* nodeX;
		endEffectGo.transform.localPosition = new Vector3(localX,localY,1);
	}

	public void OnSliderChange(float value){
		ca.orthographicSize = defaultOrthographicSize * Mathf.Max (tr, td) / 10 * (value *0.5f+0.25f);
	}

	public Vector3 getStartPointWithScale(int x,int y){
		return new Vector3 (y * nodeY, (tr - x - 1) * nodeX, 0);
	}

	public bool checkEndPoint(int curPoint){
		return false;
	}

	public void selfArrive(bool success,List<int> route,bool showSettle){
		if (Mode == MapMode.Level) {
			this.gameOver = true;
			CSPassFinish pf = new CSPassFinish ();
			pf.Success = success ? 1 : 0;
			pf.Pass = this.Pass;
			if (route == null) {
//			route =
			}
			pf.Route = route;
			SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSPassFinish, CSPassFinish.SerializeToBytes (pf), delegate(int opcode, byte[] data) {
				SCPassFinish pas = SCPassFinish.Deserialize (data);
				if (showSettle) {
					GameObject go = transform.parent.parent.Find ("Canvas/settle").gameObject;
					go.transform.Find ("Text").GetComponent<Text> ().text = pas.Success == 1 ? Message.getText("success") : Message.getText("fail");
				}
				if(pas.Success == 1){ // 修改关卡并解锁
					GameObject mainGo = GameObject.Find ("main");
					MainPanel mainPanel = mainGo.GetComponent<MainPanel>();
					mainPanel.openPass = pas.OpenPass;
					mainPanel.doShowLock();
				}
			});
			if (showSettle) {
				GameObject settleGo = transform.parent.parent.Find ("Canvas/settle").gameObject;
				settleGo.SetActive (true);
			}
		}else if (Mode == MapMode.Unlimited) {
			this.gameOver = true;
			CSUnlimitedFinish uf = new CSUnlimitedFinish ();
			uf.Success = success ? 1 : 0;
			uf.Route = route;
			uf.Pass = this.Pass;
			SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSUnlimitedFinish, CSUnlimitedFinish.SerializeToBytes (uf), delegate(int opcode, byte[] data) {
				SCUnlimitedFinish pas = SCUnlimitedFinish.Deserialize (data);
				if (showSettle) {
					GameObject go = transform.parent.parent.Find ("Canvas/settle").gameObject;
					go.transform.Find ("Text").GetComponent<Text> ().text = pas.Success == 1 ? Message.getText("success") : Message.getText("fail");
				}
			});
			if (showSettle) {
				GameObject settleGo = transform.parent.parent.Find ("Canvas/settle").gameObject;
				settleGo.SetActive (true);
			}
		}else if(Mode == MapMode.Online){
			CSArrived arrived = new CSArrived ();
			Pacman pacman = pacmanMap [SocketManager.accountId];
			arrived.Pos = pacman.outX * td + pacman.outY;
			SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSArrived, CSArrived.SerializeToBytes (arrived), delegate(int opcode, byte[] data) {
//				SCArrived ret = SCArrived.Deserialize(data);
				// TODO 结算
			});
			// TODO 结算
		}
	}
	// 玩家到达的推送
	public void userArrived(int opcode,byte[] data){
		// 标识玩家到达，比如在分数上打个对号或者变个颜色，玩家消失
		SCUserArrived userArrived = SCUserArrived.Deserialize(data);
		Pacman pacman = pacmanMap[userArrived.UserId];
		if (pacman == null) {
			Debug.LogError ("pacman is not exist while user arrived ,userId = " + userArrived.UserId);
		} else {
			pacman.finish = true;
		}
	}

	public void doGameOver(int opcode,byte[] data){
		SCGameOver gameOver = SCGameOver.Deserialize (data);
		//gameOver.OverType // 0其它，1都抵达终点，2时间到

		this.gameOver = true;

		GameObject content = transform.parent.parent.Find ("Canvas/settle/bg/scrollView/Viewport/Content").gameObject;

		//获取按钮游戏对象
		Object button = Resources.Load ("onlineSettleItem");

		// 列表
		int count = gameOver.UserInfos.Count;
		float dis = 0f;

		GameObject up = Instantiate(button) as GameObject;
		RectTransform buRec = up.GetComponent<RectTransform> ();
		Destroy(up);

		RectTransform contentTrans = content.GetComponent<RectTransform> ();
		contentTrans.sizeDelta = new Vector2 (0,(buRec.rect.height + dis) * count + dis);

		for (int i = 0; i < count; i++) {
			PBGameOverUserInfo info = gameOver.UserInfos[i];
			up = Instantiate(button) as GameObject;
			up.transform.localPosition = new Vector3 (0, -((buRec.rect.height+dis)*i+dis),0);
			up.transform.localScale = new Vector3 (1,1,1);
			up.transform.SetParent(content.transform,false);
			// 生成各个玩家的排名item
			GameObject textGo = up.transform.Find ("Text").gameObject;
			Text text = textGo.GetComponent<Text> ();
			text.text = Message.getText ("onlineSettleItem",info.Rank,info.UserName,info.Score,info.Arrived);
		}


		GameObject settleGo = transform.parent.parent.Find ("Canvas/settle").gameObject;
		settleGo.SetActive (true);

//		Renderer render;
//		Texture texture = new Texture ();
//		texture.
	}


	private void initGuide(){
		GameObject main = GameObject.Find ("main");
		GuideControl guideControl = main.transform.Find("uiHelp").GetComponent<GuideControl> ();
		guideControl.showHelp (true);
	}

}

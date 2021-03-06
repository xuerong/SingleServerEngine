﻿using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using Example;
using com.protocol;
using System.Threading;
using UnityEngine.EventSystems;
using UnityEngine.UI;
using System.Text;

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

    public int enemyCount = 0;

	 //加速道具数量
	//加时间道具数量
	//显示路线道具数量
	//加速道具数量
	public Dictionary<int,int> skillItemCount = new Dictionary<int, int>();
	public Dictionary<ItemType,Text> skillItemText = new Dictionary<ItemType, Text>();

    float x = 0,y = 0;
	static float myScale = 1f;

	public float nodeX = 0,nodeY = 0;
    float wallWidth;

	Camera ca;
	float defaultOrthographicSize = 5;

	int tr = 0,td = 0;


	float currentTime;
	public Text showTime;
	public Text showScore;
	public Text showMaxScore;

	public bool gameOver = false;

    //doors
    public GameObject door1;
    public GameObject door2;
    public GameObject door3;
    public GameObject door4;
    //door wall
    private GameObject door1Wall;
    private GameObject door2Wall;
    private GameObject door3Wall;
    private GameObject door4Wall;

	public Dictionary<string,Pacman> pacmanMap = new Dictionary<string, Pacman> ();
	private Dictionary<string,Text> scoreText = new Dictionary<string, Text> ();
	public List<CircleCollider2D> pacmanColliders = new List<CircleCollider2D> ();
	// Use this for initialization
	void Start () {
		// 设置button
		closeButton.onClick.AddListener(delegate {
			Sound.playSound(SoundType.Click);
			WarnDialog.showWarnDialog(Message.getText("exit?"),delegate {
				selfArrive(false,null,false);
				Destroy(transform.parent.parent.gameObject);
				GameObject mainGo = GameObject.Find ("main");
				MainPanel mainPanel = mainGo.GetComponent<MainPanel>();
				mainPanel.showUi(this.Mode);
			});
		});
		okButton.onClick.AddListener(delegate {
			Sound.playSound(SoundType.Click);
			Destroy(transform.parent.parent.gameObject);
			GameObject mainGo = GameObject.Find ("main");
			MainPanel mainPanel = mainGo.GetComponent<MainPanel>();
			mainPanel.showUi(this.Mode);
		});
		// 0.21 碰撞体的宽，1.9碰撞体的长
		wallWidth = 0.13f * myScale;
		nodeX = 1.9f * myScale - wallWidth*2 ;nodeY = 1.9f * myScale - wallWidth*2;

		// 显示当前的道具数量
		showSkillCountAndAddClick("addSpeed",ItemType.AddSpeed);
		showSkillCountAndAddClick("addTime",ItemType.AddTime);
		showSkillCountAndAddClick("mulBean",ItemType.MulBean);
		showSkillCountAndAddClick("showRoute",ItemType.ShowRoute);
		//
		if (Mode == MapMode.Online) {
            // 屏蔽
            starSlider.transform.parent.gameObject.SetActive(false);
            transform.parent.parent.Find("Canvas/skills").gameObject.SetActive(false);
            transform.parent.parent.Find("Canvas/score").gameObject.SetActive(false);
            // time居中
            Vector3 old = transform.parent.parent.Find("Canvas/time").localPosition;
            transform.parent.parent.Find("Canvas/time").localPosition = new Vector3(0,old.y,old.z);
            //
            foreach(KeyValuePair<string,Pacman> kv in pacmanMap){
                int x = kv.Value.inX;
                int y = kv.Value.inY;
                Transform scoreShowtransform = null;
                if (x < 2 && y < 2){
                    scoreShowtransform = transform.parent.parent.Find("Canvas/onlineScoreShow/blue");
                }else if (y > 2 && x < 2){
                    scoreShowtransform = transform.parent.parent.Find("Canvas/onlineScoreShow/green");
                }else if (y > 2 && x > 2){
                    scoreShowtransform = transform.parent.parent.Find("Canvas/onlineScoreShow/red");
                }else if (x > 2 && y < 2){
                    scoreShowtransform = transform.parent.parent.Find("Canvas/onlineScoreShow/yellow");
                }
                scoreShowtransform.gameObject.SetActive(true);
                scoreText.Add(kv.Key, scoreShowtransform.Find("Text").GetComponent<Text>());
            }

            //
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

	private void showSkillCountAndAddClick(string path,ItemType itemType){
		Text text = transform.parent.parent.Find ("Canvas/skills/"+path+"/count").GetComponent<Text>();
		int count = getItemCountByType (itemType);
		text.text = "x"+count.ToString ();
		skillItemText.Add (itemType,text);
		if (count == 0) {
			transform.parent.parent.Find ("Canvas/skills/"+path).GetComponent<Button> ().enabled = false;
		} else {
			// 使用技能的事件
			transform.parent.parent.Find ("Canvas/skills/"+path).GetComponent<Button>().onClick.AddListener(delegate {
				Sound.playSound(SoundType.Click);
				useSkill(path,itemType);
			});
		}
	}

	private int getItemCountByType(ItemType itemType){
		int itemId = Params.getItemId (itemType);
		if (skillItemCount.ContainsKey (itemId)) {
			return skillItemCount [itemId];
		}
		return 0;
	}

	//使用技能
	public void useSkill(string path,ItemType itemType){
		int itemId = Params.getItemId (itemType);

		if (!skillItemCount.ContainsKey (itemId) || skillItemCount [itemId] < 1) {
			return;
		}

		CSUseItem useItem = new CSUseItem ();
		PBItem item = new PBItem();
		item.ItemId = itemId;
		item.Count = 1;
		useItem.Item = item;

		if (itemType == ItemType.MulBean) {
			Pacman pacman = pacmanMap [SocketManager.accountId];
			useItem.Args = pacman.route.Count.ToString ();
		}

		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSUseItem, CSUseItem.SerializeToBytes (useItem), delegate(int opcode, byte[] data) {
			// 不能再点
			transform.parent.parent.Find ("Canvas/skills/"+path).GetComponent<Button>().enabled = false;
			// 如果能走到这里，说明道具使用正常
			SCUseItem ret = SCUseItem.Deserialize(data);
			skillItemCount[itemId] = skillItemCount[itemId] - 1;
			skillItemText[itemType].text = "x"+skillItemCount[itemId].ToString();

			// 道具发挥效果
			switch(itemType){
			case ItemType.AddSpeed:
				Pacman pacman = pacmanMap [SocketManager.accountId];
				pacman.addSpeed(Params.itemTables[itemId].Para1);
				break;
			case ItemType.AddTime:
				currentTime += Params.itemTables[itemId].Para1;
				break;
			case ItemType.MulBean:
				pacman = pacmanMap [SocketManager.accountId];
				pacman.mulBean = 1+Params.itemTables[itemId].Para1;
				break;
			case ItemType.ShowRoute:
				string[] routes = ret.Ret.Split (';');
				int[] routeInt = new int[routes.Length];
				for (int i = 0, len = routes.Length; i < len; i++) {
					routeInt [i] = int.Parse (routes [i]);
				}
				this.route = routeInt;
				showRoute();
				break;
			}
		});
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
				pacman.MovePosiNorm = new Vector3(userMoveInfo.DirX,userMoveInfo.DirY,0);
                pacman.changePos(pacman.MovePosiNorm);
				//Debug.Log ("receive:"+userMoveInfo.PosX+","+userMoveInfo.PosY);
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
	public Vector2 getPointByPosition(float x,float y) { 
		int row = (int)((x + nodeX / 2) / nodeX);
		int col = (int)((y + nodeY / 2) / nodeY);
		int i = (tr - col - 1);
		return new Vector2(i,row);
	}
	public Vector2 getPositionByPoint(int x,int y) {
		float y_ = tr - x - 1;
		float posY = y_ * nodeY;
		float posX = y * nodeX;
		return new Vector2(posX,posY);
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
			Sound.playSound (SoundType.EatBean);
			Destroy (beanMap [x] [y].go); // 计算在玩家身上，并做出相应的效果
			int addScore = beanMap [x] [y].score * pacmanMap [userId].mulBean;
			if (Mode == MapMode.Level || Mode == MapMode.Unlimited) {
				float old = starSlider.value;
				starSlider.value = starSlider.value + addScore;
//				Text scoreText = starSlider.transform.Find ("score").GetComponent<Text> ();
				showScore.text = starSlider.value + "";
				// 点亮星星
				for(int i=1;i<5;i++){
					int score = stars [i-1];
					if (score > old && score <= starSlider.value) { // -1的自然就不会点亮
						// 点亮
						Image image = starSlider.transform.parent.Find ("star" + i).GetComponent<Image>();
//						image.color = new Color (255,255,255);
						image.sprite = SpriteCache.getLightSprite();
					}
				}
			}else if(Mode == MapMode.Online){
				scoreText [userId].text = int.Parse (scoreText [userId].text) + addScore + "";
			}
			pacmanMap [userId].score += addScore;
			Debug.Log ("score:"+pacmanMap [userId].score);
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

	private void showRoute(){
		Object bean10 = Resources.Load ("routeUnit");
		float mapWidth = td * nodeX;

		foreach (int r in this.route) {
			Debug.Log (r);
			int i = r / td;
			int w = tr - i-1;
			int j = r % td;
			GameObject beanGo = Instantiate(bean10) as GameObject;
			beanGo.transform.parent = transform;

			beanGo.transform.localPosition = new Vector3 (x + j * nodeX, y + w * nodeY,12f);
			beanGo.transform.localScale = new Vector3 (1.05f,1.05f,1);
		}
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

        Vector3 pos = new Vector3(-x - tr * nodeX / 2f, -y - (td - 2) * nodeY / 2f, transform.localPosition.z);
        transform.parent.transform.localPosition = pos;

        Vector3 center = new Vector3(x + tr / 2f * nodeX, (td - 2) * nodeY / 2f, 10);
        Transform t = transform.parent.Find("migong_bg");
        t.localPosition = center;
        t.localScale = new Vector3(nodeX * (td - 0.4f) / 5.76f, nodeY * (tr - 0.4f) / 5.76f, 0);
        //float scale = nodeX * tr / 5.76f;


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
                    if(i==1 && j == 0){
                        door1Wall = up;
                    }else if (i == 1 && j == td - 1){
                        door2Wall = up;
                    }else if (i == tr-1 && j == td-1){
                        door3Wall = up;
                    }else if (i == tr-1 && j == 0){
                        door4Wall = up;
                    }
				}
				if (beanMap!= null && beanMap [i] [j] != null) {
					int w = tr - i-1;
					GameObject beanGo = Instantiate(beanMap [i] [j].score == 10?bean10:(beanMap [i] [j].score == 5?bean5:bean1)) as GameObject;
					beanGo.transform.parent = transform;
					beanGo.transform.localPosition = new Vector3 (x + j * nodeX, y + w * nodeY,0);
					//beanGo.transform.localScale = new Vector3 (myScale* 0.6f,myScale* 0.6f,1);
					beanMap [i] [j].go = beanGo;

					maxScore += beanMap [i] [j].score;
				}
			}
		}
        // 设置野怪
        for (int i = 0; i < enemyCount; i++)
        {
            Object enemyObj = Resources.Load("enemy");
            GameObject enemyGo = Instantiate(enemyObj) as GameObject;
            enemyGo.transform.parent = transform;
            enemyGo.transform.localPosition = new Vector3(x + tr/2 * nodeX, y + (td-1)/2 * nodeY, 0);
            enemyGo.transform.localScale = new Vector3(myScale * 0.6f, myScale * 0.6f, 1);

            Enemy enemy = enemyGo.GetComponent<Enemy>();
            enemy.mapCreate = this;
            enemy.map = this.map;
            enemy.speed = int.Parse(Params.sysParas["enemyDefaultSpeed"]);
        }
		// 设置单机版的最大值 和星星的位置
		if (Mode == MapMode.Level || Mode == MapMode.Unlimited) {
			starSlider.maxValue = maxScore;
			showMaxScore.text = "/"+maxScore;
			RectTransform buRec = starSlider.GetComponent<RectTransform> ();
			float perStarDelta = buRec.rect.width / maxScore; // 每个星星的像素便宜
			for (int i = 1; i < stars.Length+1; i++) {
				RectTransform starRec = starSlider.transform.parent.Find ("star" + i).GetComponent<RectTransform> ();
				if (stars [i-1] <= 0) {
					starRec.gameObject.SetActive (false);
				} else {
					starRec.anchoredPosition = new Vector2 (stars [i-1] * perStarDelta, 0);
				}
			}
		}
		// 设置引导
		if(needGuide){
            initGuide ((int)Mode);
		}
		// 修改相机位置
		GameObject camera = transform.parent.parent.Find("mapCamera").gameObject;

        //camera.transform.position = new Vector3(nodeX * (tr) / 2f, nodeY * (td) / 2f, camera.transform.position.z);
		// 修改相机大小
		ca = camera.GetComponent<Camera>();
		defaultOrthographicSize = ca.orthographicSize;
//		ca.orthographicSize = defaultOrthographicSize * Mathf.Max (tr, td) / mapWidth;
		float hwRate = (float)Screen.height/Screen.width;
        float newRate = Mathf.Max(tr, td) / 1.2f * hwRate;
        ca.orthographicSize = defaultOrthographicSize * newRate;
        if (Mode != MapMode.Online)
        {
            camera.transform.position = new Vector3(0, -Screen.height / 960f * ca.orthographicSize / 20f, camera.transform.position.z);

        }
        //Vector3 old =  transform.parent.transform.localPosition;


        //transform.localPosition = pos;
        //transform.parent.Find("doors").localPosition = pos
        //transform.parent.Find("pacman").localPosition = pos;

        //
//		float defaultRate = 
//		float hwRate = (float)Screen.height/Screen.width;



		Debug.Log("width:"+Screen.width+",height:"+Screen.height);
	}

	public void setEndEffect(int x,int y){
		// 设置终点特效
		//GameObject endEffectGo = Instantiate(Resources.Load("endEffect")) as GameObject;
		//endEffectGo.transform.parent = transform.parent;
		float localX = y * nodeY;
		float localY = (tr - x - 1)* nodeX;
        //endEffectGo.transform.localPosition = new Vector3(localX,localY,1);
        float doorScale = myScale * 0.6f;
        if(x <2 && y <2){
            door1.SetActive(true);
            door1.transform.localPosition = new Vector3(localX-nodeY/2, localY, 1);
            door1.transform.localScale = new Vector3(door1.transform.localScale.x * doorScale,doorScale,1);
            door1Wall.SetActive(false);
        }else if (y > 2 && x < 2)
        {
            door2.SetActive(true);
            door2.transform.localPosition = new Vector3(localX+ nodeY / 2, localY, 1);
            door2.transform.localScale = new Vector3(door2.transform.localScale.x * doorScale, doorScale, 1);
            door2Wall.SetActive(false);
        }else if (y > 2 && x > 2)
        {
            door3.SetActive(true);
            door3.transform.localPosition = new Vector3(localX+ nodeY / 2, localY, 1);
            door3.transform.localScale = new Vector3(door3.transform.localScale.x * doorScale, doorScale, 1);
            door3Wall.SetActive(false);
        }else if (x > 2 && y < 2)
        {
            door4.SetActive(true);
            door4.transform.localPosition = new Vector3(localX- nodeY / 2, localY, 1);
            door4.transform.localScale = new Vector3(door4.transform.localScale.x * doorScale, doorScale, 1);
            door4Wall.SetActive(false);
        }
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
		if (success) {
			Sound.playSound (SoundType.Arrive);
		}
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
					StringBuilder sb = new StringBuilder(pas.Success == 1 ? Message.getText("success") : Message.getText("fail"));
					if(pas.PassReward != null){
						sb.Append("\n");
						sb.Append("gold:"+pas.PassReward.Gold+"|energy:"+pas.PassReward.Energy);
						if(pas.PassReward.Item != null && pas.PassReward.Item.Count > 0){
							foreach(PBItem item in pas.PassReward.Item){
								sb.Append("|"+item.ItemId+":"+item.Count);
							}
						}
					}

					GameObject go = transform.parent.parent.Find ("Canvas/settle").gameObject;
					go.transform.Find ("Text").GetComponent<Text> ().text = sb.ToString();
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
		Sound.playSound (SoundType.Over);
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


	private void initGuide(int index){
		GameObject main = GameObject.Find ("main");
        Help guideControl = main.transform.Find("uiHelp").GetComponent<Help> ();
        guideControl.showHelp (index,true);
	}

}

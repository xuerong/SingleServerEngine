using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using com.protocol;
using Example;

public class MainPanel : MonoBehaviour {
	public GameObject uiMain;
	public GameObject uiLevel;
	public GameObject uiUnlimit;
	public GameObject uiOnline;

	public GameObject ui;
	// 系统参数
	private Dictionary<string,string> sysParas = new Dictionary<string, string> ();

	private int energy;

	// Use this for initialization
	void Start () {
		show (uiMain);
		// 给主界面按钮加事件
		string canvasPath = "main/ui/uiMain/Canvas/buttons/";
		Button levelButton = GameObject.Find (canvasPath+"level").GetComponent<Button>();
		levelButton.onClick.AddListener (delegate() {
			// 打开level 界面
			Debug.Log("open level window");
			openLevelWindow();
		});
		Button unlimitButton = GameObject.Find (canvasPath+"unlimit").GetComponent<Button>();
		unlimitButton.onClick.AddListener (delegate() {
			// 打开unlimit 界面
			Debug.Log("open unlimit window");
			openUnlimitWindow();
		});
		Button onlineButton = GameObject.Find (canvasPath+"online").GetComponent<Button>();
		onlineButton.onClick.AddListener (delegate() {
			// 打开online 界面
			Debug.Log("open online window");
			show (uiOnline);
		});
		// uiLevel 关闭按钮
		GameObject closeGo = GameObject.Find ("main/ui/uiLevel/Canvas/close");
		Button closeButton = closeGo.GetComponent<Button> ();
		closeButton.onClick.AddListener (delegate() {
			show (uiMain);
		});
		// uiUnlimit 关闭按钮和进入按钮
		Button go = GameObject.Find ("main/ui/uiUnlimit/Canvas/go").GetComponent<Button>();
		go.onClick.AddListener (delegate() {
			CSUnlimitedGo unlimitedGo = new CSUnlimitedGo();
			SocketManager.SendMessageAsyc((int)MiGongOpcode.CSUnlimitedGo,CSUnlimitedGo.SerializeToBytes(unlimitedGo),delegate(int opcode, byte[] data) {
				SCUnlimitedGo ret = SCUnlimitedGo.Deserialize(data);
				// 消耗精力
				energy = ret.Energy;
				int[] stars= {ret.Star1,ret.Star2,ret.Star3,ret.Star4};
				createMap (MapMode.Unlimited,ret.Map.ToArray (),ret.Beans, ret.Start, ret.End, ret.Pass,null,stars);
			});
			ui.SetActive (false);
		});
		closeButton = GameObject.Find ("main/ui/uiUnlimit/Canvas/close").GetComponent<Button>();
		closeButton.onClick.AddListener (delegate() {
			show (uiMain);
		});
		// uiOnline 关闭按钮和进入按钮
		go = GameObject.Find ("main/ui/uiOnline/Canvas/go").GetComponent<Button>();
		go.onClick.AddListener (delegate() {
			OnPvpClick();
		});
		closeButton = GameObject.Find ("main/ui/uiOnline/Canvas/close").GetComponent<Button>();
		closeButton.onClick.AddListener (delegate() {
			show (uiMain);
		});
		// 获取登陆基本信息
		CSBaseInfo baseInfo = new CSBaseInfo();
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSBaseInfo, CSBaseInfo.SerializeToBytes (baseInfo), delegate(int opcode, byte[] data) {
			SCBaseInfo ret = SCBaseInfo.Deserialize(data);
			this.energy = ret.Energy;
			// TODO 显示精力
			// 系统参数
			foreach(PBSysPara sp in ret.SysParas){
				sysParas.Add(sp.Key,sp.Value);
			}
		});

		// 联网对战按钮
		//
		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCMatchingSuccess, matchSuccess);
		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCMatchingFail, delegate(int opcode, byte[] receiveData) {
			Debug.Log("");
		});
		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCBegin, delegate(int opcode, byte[] receiveData) {
			Debug.Log("");
		});
	}

	private void openLevelWindow(){
		show (uiLevel);
		//

		//获取按钮游戏对象
		Object button = Resources.Load ("Button");

		GameObject content = GameObject.Find ("main/ui/uiLevel/Canvas/scrollView/Viewport/Content");
		for (int i = 0; i < content.transform.childCount; i++) {
			Destroy (content.transform.GetChild(i).gameObject);		
		}
		// 获取当前关卡
		CSGetMiGongLevel getMiGongLevel = new CSGetMiGongLevel();
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSGetMiGongLevel, CSGetMiGongLevel.SerializeToBytes (getMiGongLevel),delegate(int opcode, byte[] data) {
			SCGetMiGongLevel level = SCGetMiGongLevel.Deserialize (data);
			int count = level.PassCount;
			float dis = 20f;

			GameObject up = Instantiate(button) as GameObject;
			RectTransform buRec = up.GetComponent<RectTransform> ();
			Destroy(up);

			RectTransform contentTrans = content.GetComponent<RectTransform> ();
			contentTrans.sizeDelta = new Vector2 (0,(buRec.rect.height + dis) * count + dis);

			for (int i = 0; i < count; i++) {
				up = Instantiate(button) as GameObject;
				up.transform.SetParent(content.transform);
				up.transform.localPosition = new Vector3 (0, -((buRec.rect.height+dis)*i+dis),0);
				up.transform.localScale = new Vector3 (1,1,1);

				Button b1 = up.GetComponent<Button> ();

				ButtonIndex buttonIndex = up.GetComponent<ButtonIndex> ();
				buttonIndex.pass = i+1;
				buttonIndex.star = 0;
				if(level.StarInLevel.Count>i){
					buttonIndex.star = level.StarInLevel[i];
				}
				b1.onClick.AddListener (delegate() {OnClick(buttonIndex);});

				GameObject textGo = up.transform.Find ("Text").gameObject;
				Text text = textGo.GetComponent<Text> ();
				text.text = "pass"+buttonIndex.pass+",star"+buttonIndex.star+")";
			}
		});
	}

	private void openUnlimitWindow(){
		show (uiUnlimit);
		//

		//获取按钮游戏对象
		Object button = Resources.Load ("UnlimitItem");

		GameObject content = GameObject.Find ("main/ui/uiUnlimit/Canvas/scrollView/Viewport/Content");
		for (int i = 0; i < content.transform.childCount; i++) {
			Destroy (content.transform.GetChild(i).gameObject);		
		}
		// 获取当前关卡
		CSUnlimitedInfo unlimitedInfo = new CSUnlimitedInfo();
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSUnlimitedInfo, CSUnlimitedInfo.SerializeToBytes (unlimitedInfo),delegate(int opcode, byte[] data) {
			SCUnlimitedInfo ret = SCUnlimitedInfo.Deserialize (data);

			//
			Text passText = GameObject.Find ("main/ui/uiUnlimit/Canvas/pass").GetComponent<Text>();
			passText.text = "pass:"+ret.Pass+",star:"+ret.Star+",rank:"+ret.Rank;
			// 列表
			int count = ret.UnlimitedRankInfo.Count;
			float dis = 20f;

			GameObject up = Instantiate(button) as GameObject;
			RectTransform buRec = up.GetComponent<RectTransform> ();
			Destroy(up);

			RectTransform contentTrans = content.GetComponent<RectTransform> ();
			contentTrans.sizeDelta = new Vector2 (0,(buRec.rect.height + dis) * count + dis);

			for (int i = 0; i < count; i++) {
				PBUnlimitedRankInfo info = ret.UnlimitedRankInfo[i];
				up = Instantiate(button) as GameObject;
				up.transform.localPosition = new Vector3 (0, -((buRec.rect.height+dis)*i+dis),0);
				up.transform.localScale = new Vector3 (1,1,1);
				up.transform.SetParent(content.transform,false);
				// 生成各个玩家的排名item
				GameObject textGo = up.transform.Find ("Text").gameObject;
				Text text = textGo.GetComponent<Text> ();
				text.text = info.Rank+","+info.UserName+","+info.Pass+","+info.Star;
			}
		});
	}
		
	public void showMainPanel(){
		ui.SetActive (true);
		show (uiMain);
	}
	public void show(GameObject showUi){
		uiMain.SetActive (false);
		uiLevel.SetActive (false);
		uiUnlimit.SetActive (false);
		uiOnline.SetActive (false);
		showUi.SetActive (true);
	}

	public void matchSuccess(int opcode, byte[] data){
		SCMatchingSuccess matchingSuccess = SCMatchingSuccess.Deserialize (data);
		createMap(MapMode.Online,matchingSuccess.Map.ToArray(),matchingSuccess.Beans,matchingSuccess.Start,matchingSuccess.End,1,matchingSuccess.OtherInfos,null);
		ui.SetActive (false);
	}

	// Update is called once per frame
	void Update () {
	}

	public void OnPvpClick(){
		CSMatching matching = new CSMatching ();
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSMatching, CSMatching.SerializeToBytes(matching),delegate(int opcode, byte[] data) {
			Debug.Log("send matching success,opcode = "+opcode);
		});
		//matchWaitTime
		WarnDialog.showWaitDialog ("matching...", int.Parse (sysParas ["matchWaitTime"]), delegate() {
			WarnDialog.showWarnDialog("match fail , please try again later.",null);	
		});
	}

	public void OnClick(ButtonIndex buttonIndex){
		CSGetMiGongMap miGongMap = new CSGetMiGongMap ();
		miGongMap.Pass = buttonIndex.pass;
		byte[] data = CSGetMiGongMap.SerializeToBytes (miGongMap);
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSGetMiGongMap, data,delegate(int opcode, byte[] ret) {
			SCGetMiGongMap scmap = SCGetMiGongMap.Deserialize(ret);
			// 消耗精力
			energy = scmap.Energy;
			int[] stars= {scmap.Star1,scmap.Star2,scmap.Star3,scmap.Star4};
			createMap (MapMode.Level,scmap.Map.ToArray (),scmap.Beans, scmap.Start, scmap.End, scmap.Pass,null,stars);
		});
		ui.SetActive (false);
	}
	private void createMap(MapMode mode,int[] mapInt,List<PBBeanInfo> beans,int start,int end,int pass,List<PBOtherInfo> otherInfos,int[] stars){
		Object gamePanel = Resources.Load ("GamePanel");
		GameObject gamePanelGo = Instantiate(gamePanel) as GameObject;
		GameObject mapGo = gamePanelGo.transform.Find ("content/map").gameObject;
		MapCreate mapCreate = mapGo.GetComponent<MapCreate> ();
		mapCreate.Pass = pass;

		mapCreate.Mode = mode;
		mapCreate.stars = stars;

		int size = (int)Mathf.Sqrt(mapInt.Length);
		mapCreate.map = new int[size][];
		for(int i=0;i<size;i++){
			mapCreate.map[i] = new int[size];
			for(int j=0;j<size;j++){
				mapCreate.map[i][j] = mapInt[i*size+j];
			}
		}

		mapCreate.beanMap = new Bean[size][];
		for (int i = 0; i < size; i++) {
			mapCreate.beanMap[i] = new Bean[size];
		}
		foreach(PBBeanInfo info in beans){
			mapCreate.beanMap [info.Pos / size] [info.Pos % size] = new Bean(info.Score);
		}

		Pacman pacman = gamePanelGo.transform.Find ("content/pacman").GetComponent<Pacman> ();

		pacman.inX = start % size;
		pacman.inY = start / size;
		pacman.outX = end % size;
		pacman.outY = end / size;

		mapCreate.size = size;
		Debug.Log("map size:"+size+",End:"+end);

		gamePanelGo.transform.parent = transform;
		gamePanelGo.transform.localPosition = new Vector3(0,0,0);


		// 
		if(mode == MapMode.Online && otherInfos != null){
			Object pacmanIns = Resources.Load ("pacman");
			foreach (PBOtherInfo otherInfo in otherInfos) {
				GameObject pacmanGo = Instantiate(pacmanIns) as GameObject;

				pacman = pacmanGo.GetComponent<Pacman> ();
				pacman.userId = otherInfo.UserId;

				pacman.inX = otherInfo.Start % size;
				pacman.inY = otherInfo.Start / size;
				pacman.outX = otherInfo.End % size;
				pacman.outY = otherInfo.End / size;

				pacman.mapCreate = mapCreate;

				pacmanGo.transform.parent = gamePanelGo.transform.Find("content");

			}
		}
	}
}

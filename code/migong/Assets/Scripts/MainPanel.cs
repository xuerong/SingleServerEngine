﻿using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using com.protocol;
using Example;
using cn.sharesdk.unity3d;

public class MainPanel : MonoBehaviour {
	public GameObject uiMain;
	public GameObject uiLevel;
	public GameObject uiUnlimit;
	public GameObject uiOnline;

	public GameObject ui;
	// 系统参数
	private Dictionary<string,string> sysParas = new Dictionary<string, string> ();

	private int energy;

	private int matchingDialogId; // 匹配阶段弹窗的id

	private ShareSDK ssdk;
	// Use this for initialization
	void Start () {
		show (uiMain);
		// 给主界面按钮加事件
		string canvasPath = "main/ui/uiMain/Canvas/buttons/";
		Button levelButton = GameObject.Find (canvasPath+"level2").GetComponent<Button>();
		levelButton.onClick.AddListener (delegate() {
			// 打开level 界面
			Debug.Log("open level window");
			openLevelWindow();
		});
		Button unlimitButton = GameObject.Find (canvasPath+"unlimit2").GetComponent<Button>();
		unlimitButton.onClick.AddListener (delegate() {
			// 打开unlimit 界面
			Debug.Log("open unlimit window");
			openUnlimitWindow();
		});
		Button onlineButton = GameObject.Find (canvasPath+"online2").GetComponent<Button>();
		onlineButton.onClick.AddListener (delegate() {
			// 打开online 界面
			Debug.Log("open online window");
			openOnlineWindow();
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
				ui.SetActive (false);
				SCUnlimitedGo ret = SCUnlimitedGo.Deserialize(data);
				// 消耗精力
				energy = ret.Energy;
				int[] stars= {ret.Star1,ret.Star2,ret.Star3,ret.Star4};
				createMap (MapMode.Unlimited,ret.Map.ToArray (),ret.Beans, ret.Speed,ret.Start, ret.End, ret.Pass,null,stars);
			});
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


		// 账号，分享，帮助
		ssdk = new ShareSDK();

		Button accountButton = GameObject.Find (canvasPath+"account").GetComponent<Button>();
		accountButton.onClick.AddListener (delegate() {
			Debug.Log("account");
			doAccount();
		});
		ssdk.showUserHandler = GetUserInfoResultHandler;
		Button shareButton = GameObject.Find (canvasPath+"share").GetComponent<Button>();
		shareButton.onClick.AddListener (delegate() {
			Debug.Log("share");
			doShare();
		});
		ssdk.shareHandler = ShareResultHandler;

		// 联网对战按钮
		//
		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCMatchingSuccess, matchSuccess);
		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCMatchingFail, delegate(int opcode, byte[] receiveData) {
			Debug.Log("");
		});
		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCBegin, delegate(int opcode, byte[] receiveData) {
			Debug.Log("");
		});
		SocketManager.AddServerSendReceive ((int)AccountOpcode.SCBeTakePlace, delegate(int opcode, byte[] receiveData) {
			WarnDialog.showWarnDialog("你的账号在其它地方登录！",delegate() {
				Application.Quit();
			},true);
		});
	}

	void doAccount(){
		ssdk.Authorize(PlatformType.WeChat);
	}
	// 账号回调
	void GetUserInfoResultHandler (int reqID, ResponseState state, PlatformType type, Hashtable result)
	{
		if (state == ResponseState.Success)
		{
			print ("get user info result :");
			print (MiniJSON.jsonEncode(result));
			ssdk.GetUserInfo(PlatformType.WeChat);
		}
		else if (state == ResponseState.Fail)
		{
			print ("fail! throwable stack = " + result["stack"] + "; error msg = " + result["msg"]);
		}
		else if (state == ResponseState.Cancel) 
		{
			print ("cancel !");
		}
	}

	void doShare(){
		ShareContent content = new ShareContent();
		content.SetText("this is a test string.");
		content.SetImageUrl("https://f1.webshare.mob.com/code/demo/img/1.jpg");
		content.SetTitle("test title");
		content.SetTitleUrl("http://www.mob.com");
		content.SetSite("Mob-ShareSDK");
		content.SetSiteUrl("http://www.mob.com");
		content.SetUrl("http://www.mob.com");
		content.SetComment("test description");
		content.SetMusicUrl("http://mp3.mwap8.com/destdir/Music/2009/20090601/ZuiXuanMinZuFeng20090601119.mp3");
		content.SetShareType(ContentType.Webpage);


		//通过分享菜单分享
		ssdk.ShowPlatformList (null, content, 100, 100);

		//直接通过编辑界面分享
		ssdk.ShowShareContentEditor (PlatformType.SinaWeibo, content);

		//直接分享
		ssdk.ShareContent (PlatformType.SinaWeibo, content);
	}
	// 分享回调
	void ShareResultHandler (int reqID, ResponseState state, PlatformType type, Hashtable result)
	{
		if (state == ResponseState.Success)
		{
			print ("share result :");
			print (MiniJSON.jsonEncode(result));
		}
		else if (state == ResponseState.Fail)
		{
			print ("fail! error code = " + result["error_code"] + "; error msg = " + result["error_msg"]);
		}
		else if (state == ResponseState.Cancel) 
		{
			print ("cancel !");
		}
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
		Debug.Log("send getMiGongLevel:");
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSGetMiGongLevel, CSGetMiGongLevel.SerializeToBytes (getMiGongLevel),delegate(int opcode, byte[] data) {
			Debug.Log("receive SCGetMiGongLevel:");
			SCGetMiGongLevel level = SCGetMiGongLevel.Deserialize (data);
			int count = level.PassCount;
			float dis = 20f;

			GameObject up = Instantiate(button) as GameObject;
			RectTransform buRec = up.GetComponent<RectTransform> ();
			Destroy(up);

			RectTransform contentTrans = content.GetComponent<RectTransform> ();
			contentTrans.sizeDelta = new Vector2 (0,(buRec.rect.height + dis) * count + dis);

			Debug.Log("count:"+count);

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
			show (uiUnlimit);
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

	private void openOnlineWindow(){
		CSGetOnlineInfo getOnlineInfo = new CSGetOnlineInfo ();
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSGetOnlineInfo, CSGetOnlineInfo.SerializeToBytes (getOnlineInfo), delegate(int opcode, byte[] data) {
			show (uiOnline);
			SCGetOnlineInfo ret = SCGetOnlineInfo.Deserialize(data);
			Text scoreText = GameObject.Find ("main/ui/uiOnline/Canvas/score").GetComponent<Text>();
			Text titleText = GameObject.Find ("main/ui/uiOnline/Canvas/title").GetComponent<Text>();
			Text rankText = GameObject.Find ("main/ui/uiOnline/Canvas/rank").GetComponent<Text>();

			scoreText.text = "score:"+ret.Score;
			titleText.text = "score:"+ret.Title;
			rankText.text = "score:"+ret.Rank;
//			onlineInfo.

			GameObject content = GameObject.Find ("main/ui/uiOnline/Canvas/scrollView/Viewport/Content");
			for (int i = 0; i < content.transform.childCount; i++) {
				Destroy (content.transform.GetChild(i).gameObject);		
			}

			//获取按钮游戏对象
			Object button = Resources.Load ("UnlimitItem");

			// 列表
			int count = ret.RankInfos.Count;
			float dis = 20f;

			GameObject up = Instantiate(button) as GameObject;
			RectTransform buRec = up.GetComponent<RectTransform> ();
			Destroy(up);

			RectTransform contentTrans = content.GetComponent<RectTransform> ();
			contentTrans.sizeDelta = new Vector2 (0,(buRec.rect.height + dis) * count + dis);

			for (int i = 0; i < count; i++) {
				PBOnlineRankInfo info = ret.RankInfos[i];
				up = Instantiate(button) as GameObject;
				up.transform.localPosition = new Vector3 (0, -((buRec.rect.height+dis)*i+dis),0);
				up.transform.localScale = new Vector3 (1,1,1);
				up.transform.SetParent(content.transform,false);
				// 生成各个玩家的排名item
				GameObject textGo = up.transform.Find ("Text").gameObject;
				Text text = textGo.GetComponent<Text> ();
				text.text = info.Rank+","+info.Name+","+info.Score+","+info.Title;
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
		createMap(MapMode.Online,matchingSuccess.Map.ToArray(),matchingSuccess.Beans,matchingSuccess.Speed,matchingSuccess.Start,matchingSuccess.End,1,matchingSuccess.OtherInfos,null);
		ui.SetActive (false);
		if (matchingDialogId > 0) {
			WarnDialog.closeWaitDialog (matchingDialogId);
			matchingDialogId = 0;
		}
	}

	// Update is called once per frame
	void Update () {
	}

	public void OnPvpClick(){
		CSMatching matching = new CSMatching ();
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSMatching, CSMatching.SerializeToBytes(matching),delegate(int opcode, byte[] data) {
			Debug.Log("send matching success,opcode = "+opcode);
			//matchWaitTime
			matchingDialogId = WarnDialog.showWaitDialog ("matching...", int.Parse (sysParas ["matchWaitTime"]), delegate() {
				WarnDialog.showWarnDialog("match fail , please try again later.",null);	
				//
				CSCancelMatching cancelMatching = new CSCancelMatching();
				SocketManager.SendMessageAsyc((int)MiGongOpcode.CSCancelMatching,CSCancelMatching.SerializeToBytes(cancelMatching),delegate(int retOpcode, byte[] retData) {

				});
			},delegate() {
				//
				CSCancelMatching cancelMatching = new CSCancelMatching();
				SocketManager.SendMessageAsyc((int)MiGongOpcode.CSCancelMatching,CSCancelMatching.SerializeToBytes(cancelMatching),delegate(int retOpcode, byte[] retData) {

				});
			});
		});
	}

	public void OnClick(ButtonIndex buttonIndex){
		CSGetMiGongMap miGongMap = new CSGetMiGongMap ();
		miGongMap.Pass = buttonIndex.pass;
		byte[] data = CSGetMiGongMap.SerializeToBytes (miGongMap);
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSGetMiGongMap, data,delegate(int opcode, byte[] ret) {
			ui.SetActive (false);
			SCGetMiGongMap scmap = SCGetMiGongMap.Deserialize(ret);
			// 消耗精力
			energy = scmap.Energy;
			int[] stars= {scmap.Star1,scmap.Star2,scmap.Star3,scmap.Star4};
			createMap (MapMode.Level,scmap.Map.ToArray (),scmap.Beans, scmap.Speed,scmap.Start, scmap.End, scmap.Pass,null,stars);
		});
	}
	private void createMap(MapMode mode,int[] mapInt,List<PBBeanInfo> beans,int speed,int start,int end,int pass,List<PBOtherInfo> otherInfos,int[] stars){
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
		pacman.speed = speed;

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
				pacman.speed = speed;
//				pacman.transform.localScale = new Vector3 (0.6f,0.6f,0.6f);

				pacman.mapCreate = mapCreate;

				pacmanGo.transform.parent = gamePanelGo.transform.Find("content");

			}
		}
	}
}

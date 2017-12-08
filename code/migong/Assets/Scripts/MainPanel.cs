using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using com.protocol;
using Example;
using cn.sharesdk.unity3d;
using System.Text;

public class MainPanel : MonoBehaviour {
	public GameObject uiMain;
	public GameObject uiLevel;
	public GameObject uiUnlimit;
	public GameObject uiOnline;

	public GameObject uiHelp;

	public GameObject ui;
	// 系统参数
	private Dictionary<string,string> sysParas = new Dictionary<string, string> ();
	private Dictionary<int,int> guideStep = new Dictionary<int, int> ();

	private int energy;

	private int matchingDialogId; // 匹配阶段弹窗的id

	public ShareSDK ssdk;

	public Button shareWeChat1;
	public Button shareWeChat2;
	public Button closeShare;

	public Button exit;


	private Button levelButton;
	private Button unlimitButton;
	private Button onlineButton;
	// Use this for initialization

	public int openPass;

	public int showRewardPass = 0;

	void Start () {
		show (uiMain);
		// 给主界面按钮加事件
		string canvasPath = "main/ui/uiMain/Canvas/buttons/";
		levelButton = GameObject.Find (canvasPath+"level2").GetComponent<Button>();
		levelButton.onClick.AddListener (delegate() {
			// 打开level 界面
//			Debug.Log("open level window");
			Sound.playSound(SoundType.Click);
			openLevelWindow();
		});
		unlimitButton = GameObject.Find (canvasPath+"unlimit2").GetComponent<Button>();
		unlimitButton.onClick.AddListener (delegate() {
			// 打开unlimit 界面
			Debug.Log("open unlimit window");
			Sound.playSound(SoundType.Click);
			openUnlimitWindow();
		});
		onlineButton = GameObject.Find (canvasPath+"online2").GetComponent<Button>();
		onlineButton.onClick.AddListener (delegate() {
			// 打开online 界面
			Debug.Log("open online window");
			Sound.playSound(SoundType.Click);
			openOnlineWindow();
		});
		// uiLevel 关闭按钮
		GameObject closeGo = GameObject.Find ("main/ui/uiLevel/Canvas/close");
		Button closeButton = closeGo.GetComponent<Button> ();
		closeButton.onClick.AddListener (delegate() {
			Sound.playSound(SoundType.Click);
			show (uiMain);
		});
		// uiUnlimit 关闭按钮和进入按钮
		Button go = GameObject.Find ("main/ui/uiUnlimit/Canvas/go").GetComponent<Button>();
		go.onClick.AddListener (delegate() {
			Sound.playSound(SoundType.Click);
			CSUnlimitedGo unlimitedGo = new CSUnlimitedGo();
			SocketManager.SendMessageAsyc((int)MiGongOpcode.CSUnlimitedGo,CSUnlimitedGo.SerializeToBytes(unlimitedGo),delegate(int opcode, byte[] data) {
				ui.SetActive (false);
				SCUnlimitedGo ret = SCUnlimitedGo.Deserialize(data);
				// 消耗精力
				energy = ret.Energy;
				int[] stars= {ret.Star1,ret.Star2,ret.Star3,ret.Star4};
				createMap (MapMode.Unlimited,ret.Map.ToArray (),ret.Beans,ret.Time, ret.Speed,ret.Start, ret.End, ret.Pass,null,stars,null,null);
			});
		});
		closeButton = GameObject.Find ("main/ui/uiUnlimit/Canvas/close").GetComponent<Button>();
		closeButton.onClick.AddListener (delegate() {
			Sound.playSound(SoundType.Click);
			show (uiMain);
		});
		// uiOnline 关闭按钮和进入按钮
		go = GameObject.Find ("main/ui/uiOnline/Canvas/go").GetComponent<Button>();
		go.onClick.AddListener (delegate() {
			Sound.playSound(SoundType.Click);
			OnPvpClick();
		});
		closeButton = GameObject.Find ("main/ui/uiOnline/Canvas/close").GetComponent<Button>();
		closeButton.onClick.AddListener (delegate() {
			Sound.playSound(SoundType.Click);
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

			foreach(PBNewGuide newGuide in ret.NewGuide ){
				guideStep.Add(newGuide.Id,newGuide.Step);
			}
			this.openPass = ret.OpenPass;
			// 道具表
			Params.init(ret);
			// 无尽版和pvp是否开启
			doShowLock();
		});

		// 账号，分享，帮助

		Button accountButton = GameObject.Find (canvasPath+"account").GetComponent<Button>();
		accountButton.onClick.AddListener (delegate() {
			Debug.Log("account");
			Sound.playSound(SoundType.Click);
			doAccount();
		});
		ssdk.showUserHandler = GetUserInfoResultHandler;
		Button shareButton = GameObject.Find (canvasPath+"share").GetComponent<Button>();
		shareButton.onClick.AddListener (delegate() {
			Debug.Log("share");
//			doShare();
			Sound.playSound(SoundType.Click);
			shareWeChat1.transform.parent.gameObject.SetActive(true);
		});
		ssdk.shareHandler = ShareResultHandler;
		// 分享的三个按钮就
		shareWeChat1.onClick.AddListener (delegate() {
			Debug.Log("share WeChat");
			Sound.playSound(SoundType.Click);
			doShare(PlatformType.WeChat);
		});
		shareWeChat2.onClick.AddListener (delegate() {
			Debug.Log("share WeChatMoments");
			Sound.playSound(SoundType.Click);
			doShare(PlatformType.WeChatMoments);
		});
		closeShare.onClick.AddListener (delegate() {
			Sound.playSound(SoundType.Click);
			shareWeChat1.transform.parent.gameObject.SetActive(false);
		});

		exit.onClick.AddListener (delegate() {
			Sound.playSound(SoundType.Click);
			Application.Quit();
		});

		//
		Button helpButton = GameObject.Find (canvasPath+"help").GetComponent<Button>();
		helpButton.onClick.AddListener (delegate() {
			Debug.Log("help");
//			WarnDialog.showWarnDialog("test",null,false,10,20);
			Sound.playSound(SoundType.Click);
			GuideControl guideControl = uiHelp.GetComponent<GuideControl>();
			guideControl.showHelp(false);
		});

		// 对战中显示奖励界面的进入地图按钮
		Button showRewardOk = uiLevel.transform.Find("Canvas/showReward/dialog/ok").GetComponent<Button>();
		showRewardOk.onClick.AddListener (delegate() {
			Sound.playSound(SoundType.Click);
			onIntoPassClick();
		});
		Button showRewardClose = uiLevel.transform.Find("Canvas/showReward/dialog/close").GetComponent<Button>();
		showRewardClose.onClick.AddListener (delegate() {
			Sound.playSound(SoundType.Click);
			uiLevel.transform.Find("Canvas/showReward").gameObject.SetActive(false);
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
		SocketManager.AddServerSendReceive ((int)AccountOpcode.SCBeTakePlace, delegate(int opcode, byte[] receiveData) {
			WarnDialog.showWarnDialog("你的账号在其它地方登录！",delegate() {
				Application.Quit();
			},true);
		});
	}

	public void doShowLock(){
		showLock(MapMode.Unlimited,int.Parse(sysParas["openUnlimited"]) >= openPass);
		showLock(MapMode.Online,int.Parse(sysParas["openPvp"]) >= openPass);
	}
	private void showLock(MapMode mode,bool _lock){
		Button bu = unlimitButton;
		if (mode == MapMode.Online) {
			bu = onlineButton;
		}

		if (_lock) {
			bu.transform.Find ("lock").gameObject.SetActive (true);
			bu.interactable = false;
		} else {
			bu.transform.Find ("lock").gameObject.SetActive (false);
			bu.interactable = true;
		}
	}

	void doAccount(){
		// TODO 账号要开发者认证 300块/年
//		ssdk.Authorize(PlatformType.WeChat);
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

	void doShare(PlatformType type){
		// 显示分享菜单


		ShareContent content = new ShareContent();
		content.SetText(Message.getText("gameTitle"));
		content.SetImageUrl("https://xuerong.github.io/mazeAndPacman/resource/icon.png");
		content.SetTitle(Message.getText("gameTitle"));
		content.SetTitleUrl("https://xuerong.github.io/mazeAndPacman/");
//		content.SetSite("迷宫与吃豆人");
//		content.SetSiteUrl("https://xuerong.github.io/mazeAndPacman/");
		content.SetUrl("https://xuerong.github.io/mazeAndPacman/");
//		content.SetComment("迷宫与吃豆人");
//		content.SetMusicUrl("http://mp3.mwap8.com/destdir/Music/2009/20090601/ZuiXuanMinZuFeng20090601119.mp3");
//		content.SetFilePath("https://xuerong.github.io/mazeAndPacman/resource/mi.apk");
		content.SetShareType(ContentType.Webpage);

		//通过分享菜单分享
//		ssdk.ShowPlatformList (null, content, 100, 100);

		//直接通过编辑界面分享
//		ssdk.ShowShareContentEditor (PlatformType.WeChat, content);
//
//		//直接分享
		ssdk.ShareContent (type, content);

		shareWeChat1.transform.parent.gameObject.SetActive(false);
	}
	// 分享回调
	void ShareResultHandler (int reqID, ResponseState state, PlatformType type, Hashtable result)
	{
		if (state == ResponseState.Success) {
			WarnDialog.showWarnDialog ("share result :" + MiniJSON.jsonEncode (result));
			print ("share result :");
			print (MiniJSON.jsonEncode (result));
		} else if (state == ResponseState.Fail) {
			StringBuilder sb = new StringBuilder ();
			foreach(DictionaryEntry de in result)
			{
				sb.Append(string.Format("{0}-{1}", de.Key, de.Value));
			}
			WarnDialog.showWarnDialog ("fail! error code = " + sb.ToString());
			print ("fail! error code = " + result ["error_code"] + "; error msg = " + result ["error_msg"]);
		} else if (state == ResponseState.Cancel) {
			WarnDialog.showWarnDialog ("cancel !");
			print ("cancel !");
		} else {
			WarnDialog.showWarnDialog ("state:"+state);
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

			GameObject energyTextGo = GameObject.Find ("main/ui/uiLevel/Canvas/energy/Text");
			Text energyText = energyTextGo.GetComponent<Text>();
			energyText.text = this.energy.ToString();

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
				b1.onClick.AddListener (delegate() {
					Sound.playSound(SoundType.Click);
					OnClick(buttonIndex);
				});

				GameObject textGo = up.transform.Find ("Text").gameObject;
				Text text = textGo.GetComponent<Text> ();
				text.text = Message.getText("levelItem",buttonIndex.pass,buttonIndex.star);
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
			passText.text = Message.getText("unlimitRankSelf",ret.Pass,ret.Star,ret.Rank);
			//
			GameObject energyTextGo = GameObject.Find ("main/ui/uiUnlimit/Canvas/energy/Text");
			Text energyText = energyTextGo.GetComponent<Text>();
			energyText.text = this.energy.ToString();
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
				text.text = Message.getText("unlimitRankItem",info.Rank,info.UserName,info.Star);
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

			scoreText.text = Message.getText("onlineScorev",ret.Score);
			titleText.text = Message.getText("onlineTitle",ret.Title);
			rankText.text = Message.getText("onlineRank",ret.Rank);


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
				text.text = Message.getText("onlineRankItem",info.Rank,info.Name,info.Score,info.Title);
			}

		});
	}

	public void showUi(MapMode mode){
		ui.SetActive (true);
		switch (mode) {
		case MapMode.Level:
			openLevelWindow ();
			break;
		case MapMode.Unlimited:
			openUnlimitWindow ();
			break;
		case MapMode.Online:
			openOnlineWindow ();
			break;
		}
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
		createMap(MapMode.Online,matchingSuccess.Map.ToArray(),matchingSuccess.Beans,matchingSuccess.Time,matchingSuccess.Speed,matchingSuccess.Start,matchingSuccess.End,1,matchingSuccess.OtherInfos,null,null,null);
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
			matchingDialogId = WarnDialog.showWaitDialog (Message.getText("matching"), int.Parse (sysParas ["matchWaitTime"]), delegate() {
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

		CSGetPassReward passReward = new CSGetPassReward ();
		passReward.Pass = buttonIndex.pass;
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSGetPassReward, CSGetPassReward.SerializeToBytes(passReward), delegate(int opcode, byte[] data) {
			uiLevel.transform.Find("Canvas/showReward").gameObject.SetActive(true);
			SCGetPassReward ret = SCGetPassReward.Deserialize(data);
//			ret.PassRewardStar1
			showRewardStarInfo(1,ret.PassRewardStar1);
			showRewardStarInfo(2,ret.PassRewardStar2);
			showRewardStarInfo(3,ret.PassRewardStar3);
			showRewardStarInfo(4,ret.PassRewardStar4);

			this.showRewardPass = buttonIndex.pass;
		});
	}

	private void showRewardStarInfo(int starIndex,PBPassReward passReward){
		if(passReward != null){
			StringBuilder sb = new StringBuilder();
			if(passReward.Item!= null && passReward.Item.Count>0){
				foreach(PBItem item in passReward.Item){
					sb.Append(item.ItemId+":"+item.Count+"|");
				}
			}
			Text starText = uiLevel.transform.Find("Canvas/showReward/dialog/star"+starIndex).GetComponent<Text>();
			starText.text = "gold:"+passReward.Gold+"|energy:"+passReward.Energy+"|"+sb.ToString();
		}
	}

	public void onIntoPassClick(){
		CSGetMiGongMap miGongMap = new CSGetMiGongMap ();
		miGongMap.Pass = showRewardPass;
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSGetMiGongMap, CSGetMiGongMap.SerializeToBytes (miGongMap),delegate(int opcode, byte[] ret) {
			uiLevel.transform.Find("Canvas/showReward").gameObject.SetActive(false);
			ui.SetActive (false);
			SCGetMiGongMap scmap = SCGetMiGongMap.Deserialize(ret);
			// 消耗精力
			energy = scmap.Energy;
			int[] stars= {scmap.Star1,scmap.Star2,scmap.Star3,scmap.Star4};

			createMap (MapMode.Level,scmap.Map.ToArray (),scmap.Beans,scmap.Time, scmap.Speed,scmap.Start, scmap.End, scmap.Pass,null,stars,scmap.Route,scmap.Items);
		});
	}

	private void createMap(MapMode mode,int[] mapInt,List<PBBeanInfo> beans,int time,int speed,int start,int end,int pass,List<PBOtherInfo> otherInfos,int[] stars,string guideRoute,List<PBItem> skillItems){
		Object gamePanel = Resources.Load ("GamePanel");
		GameObject gamePanelGo = Instantiate(gamePanel) as GameObject;
		GameObject mapGo = gamePanelGo.transform.Find ("content/map").gameObject;
		MapCreate mapCreate = mapGo.GetComponent<MapCreate> ();

		// 创建引导
		if (guideRoute != null && mode == MapMode.Level && guideStep[(int)GuideType.Pass] == 0) {
			string[] routes = guideRoute.Split (';');
			int[] routeInt = new int[routes.Length];
			for (int i = 0, len = routes.Length; i < len; i++) {
				routeInt [i] = int.Parse (routes [i]);
			}
			mapCreate.route = routeInt;
			mapCreate.needGuide = true;
		}


		mapCreate.Pass = pass;

		mapCreate.Mode = mode;
		mapCreate.stars = stars;
		mapCreate.totalTime = time;

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

		mapCreate.pacmanMap.Add (SocketManager.accountId,pacman);

		pacman.inX = start % size;
		pacman.inY = start / size;
		pacman.outX = end % size;
		pacman.outY = end / size;
		pacman.speed = speed;


		mapCreate.size = size;
		Debug.Log("map size:"+size+",End:"+end);

		gamePanelGo.transform.parent = transform;
		gamePanelGo.transform.localPosition = new Vector3(0,0,0);

		if (skillItems != null) {
			foreach (PBItem item in skillItems) {
				mapCreate.skillItemCount.Add (item.ItemId,item.Count);
			}
		}
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

				mapCreate.pacmanMap.Add (otherInfo.UserId,pacman);

				pacman.mapCreate = mapCreate;

				pacmanGo.transform.parent = gamePanelGo.transform.Find("content");

			}
		}
	}

	public void setGuideState(GuideType type,int step){
		guideStep [(int)type] = step;
	}

}
public enum GuideType{
	Pass = 1,
	Pvp = 2
}

using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using com.protocol;
using Example;
using cn.sharesdk.unity3d;
using System.Text;

class UnlimitedAwardSeg {
	public Image image;
	public int star;
	public int state;
}
public class MainPanel : MonoBehaviour {
	public GameObject uiMain;
	public GameObject uiLevel;
	public GameObject uiUnlimit;
	public GameObject uiOnline;

	public GameObject uiHelp;
    public GameObject uiAccount;

	public GameObject uiPacket;
	public GameObject uiShop;

	public GameObject ui;
	// 系统参数
	private Dictionary<string,string> sysParas = new Dictionary<string, string> ();
	private Dictionary<int,int> guideStep = new Dictionary<int, int> ();


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


	public Matching matching; // 匹配中的控制

	public GameObject unlimitedAwardGo;
	private UnlimitedAwardSeg[] unlimitedAwardSegs;

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
			//Job.startJob(Params.energyJobKey, delegate() {
			//	Debug.Log("sdfsdfsdfs");
			//},10);
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
				// 剩余精力
				Params.startEnergySchedule(ret.Energy.Energy,ret.Energy.RefreshTime);
				int[] stars= {ret.Star1,ret.Star2,ret.Star3,ret.Star4};
                createMap (MapMode.Unlimited,ret.Map.ToArray (),ret.Beans,ret.Time, ret.Speed,ret.Start, ret.End, ret.Pass,ret.EnemyCount,null,stars,null,null);
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
			// TODO 显示精力
			// 系统参数
			foreach(PBSysPara sp in ret.SysParas){
				sysParas.Add(sp.Key,sp.Value);
			}
            Params.sysParas = sysParas;

			Params.energyRecoverTime = int.Parse(sysParas["energyRecoverTime"]);
			// 精力相关
            Params.startEnergySchedule(ret.Energy.Energy,ret.Energy.RefreshTime);

			foreach(PBNewGuide newGuide in ret.NewGuide ){
				guideStep.Add(newGuide.Id,newGuide.Step);
			}
			this.openPass = ret.OpenPass;
			// 道具表
			Params.init(ret);
			// 无尽版和pvp是否开启
			doShowLock();
			// 初始化无尽版的每日星数奖励
			//string para = sysParas["unlimitedAwardStar"];
			//string[] paras = para.Split(';');
			Slider slider = unlimitedAwardGo.transform.Find("starSlider").GetComponent<Slider>();
			RectTransform buRec = unlimitedAwardGo.transform.Find("starSlider").GetComponent<RectTransform> ();
			int len = ret.UnlimitedRewardTable.Count;
			slider.maxValue = ret.UnlimitedRewardTable[len-1].Star;
			Object ob = Resources.Load("chest");

			unlimitedAwardSegs = new UnlimitedAwardSeg[len];
			for (int i = 0; i < len; i++) {
				GameObject chestGo = Instantiate(ob) as GameObject;
				chestGo.transform.localPosition = new Vector3(
					buRec.rect.width/slider.maxValue*ret.UnlimitedRewardTable[i].Star - buRec.rect.width/2,0,0);
				chestGo.transform.SetParent(unlimitedAwardGo.transform, false);
				UnlimitedAwardSeg unlimitedAward = unlimitedAwardSegs[i] = new UnlimitedAwardSeg();
				unlimitedAward.star = ret.UnlimitedRewardTable[i].Star;
				unlimitedAward.image = chestGo.GetComponent<Image>();
				Button bt = chestGo.GetComponent<Button>();
				int index = i;
				PBUnlimitedRewardTable table = ret.UnlimitedRewardTable[i];
				bt.onClick.AddListener(delegate
				{
					Sound.playSound(SoundType.Click);
					// 如果达到了，就直接领取了，然后就是确定，否则就是显示，然后是确定
					if (unlimitedAward.state == 1)
					{
						// 领取
						Sound.playSound(SoundType.Click);
						CSUnlimitedAward award = new CSUnlimitedAward();
						award.Index = index;
						SocketManager.SendMessageAsyc((int)MiGongOpcode.CSUnlimitedAward, CSUnlimitedAward.SerializeToBytes(award), (opcode2, data2) => {
							SCUnlimitedAward retAward = SCUnlimitedAward.Deserialize(data2);
							unlimitedAward.image.sprite = SpriteCache.getSprite("box/open");
							unlimitedAward.state = 2;
							// 显示
							WarnDialog.reward(Message.getText("gainReward"),Message.getText("ok"),table.Gold,table.Reward,null);
						});

					}
					else {
						// 显示
						WarnDialog.reward(Message.getText("dailyReward"),Message.getText("ok"),table.Gold,table.Reward,null);
					}
				});
			}
		});

		// 账号，分享，帮助

		Button accountButton = GameObject.Find (canvasPath+"more/account").GetComponent<Button>();
		accountButton.onClick.AddListener (delegate() {
			Debug.Log("account");
			Sound.playSound(SoundType.Click);
            closeMorePanel();
			doAccount();
		});
		ssdk.showUserHandler = GetUserInfoResultHandler;
        Button shareButton = GameObject.Find (canvasPath+"more/share").GetComponent<Button>();
		shareButton.onClick.AddListener (delegate() {
			Debug.Log("share");
//			doShare();
			Sound.playSound(SoundType.Click);
            closeMorePanel();
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

        // show more
        Button moreButton = GameObject.Find(canvasPath + "moreBt").GetComponent<Button>();
        moreButton.onClick.AddListener(delegate () {
            GameObject moreGo = GameObject.Find(canvasPath + "more");
            moreGo.SetActive(!moreGo.activeSelf);
        });
        // hide more
        Button hideMoreButton = GameObject.Find(canvasPath + "more/clickPanel").GetComponent<Button>();
        hideMoreButton.onClick.AddListener(delegate () {
            closeMorePanel();
        });
		//
        Button helpButton = GameObject.Find (canvasPath+"more/help").GetComponent<Button>();
		helpButton.onClick.AddListener (delegate() {
			Debug.Log("help");
//			WarnDialog.showWarnDialog("test",null,false,10,20);
			Sound.playSound(SoundType.Click);
            closeMorePanel();
            Help guideControl = uiHelp.GetComponent<Help>();
			guideControl.showHelp();
		});

		// 背包，商店
		Button packetButton = GameObject.Find (canvasPath+"packet").GetComponent<Button>();
		packetButton.onClick.AddListener (delegate() {
			Debug.Log("backet");
			Sound.playSound(SoundType.Click);
			uiPacket.GetComponent<Packet>().showPacket();
		});
		Button shopButton = GameObject.Find (canvasPath+"shop").GetComponent<Button>();
		shopButton.onClick.AddListener (delegate() {
			Debug.Log("shop");
			Sound.playSound(SoundType.Click);
			uiShop.GetComponent<Shop>().showShop();
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
			matching.matchFail();
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

    private void closeMorePanel(){
        GameObject moreGo = GameObject.Find("main/ui/uiMain/Canvas/buttons/" + "more");
        if (moreGo.activeSelf)
        {
            moreGo.SetActive(!moreGo.activeSelf);
        }
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
        uiAccount.SetActive(true);
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
            // 体力
            //GameObject energyTextGo = GameObject.Find ("main/ui/uiLevel/Canvas/energy/Text");
            //Text energyText = energyTextGo.GetComponent<Text>();
            //energyText.text = Params.energy.ToString();

            //获取游戏对象
            Object levelItem = Resources.Load ("levelItem");

			GameObject up = Instantiate(levelItem) as GameObject;
			RectTransform buRec = up.GetComponent<RectTransform> ();
			Destroy(up);

			Debug.Log("count:"+count);

			RectTransform canvas = GameObject.Find ("main/ui/uiLevel/Canvas").GetComponent<RectTransform>();
			float scale = canvas.rect.width/Params.uiWidth;
			float be = (canvas.rect.width-(buRec.rect.width * 3))/4;
			float step = be+buRec.rect.width;
//			Debug.LogError(canvas.rect.width+","+Screen.width+","+buRec.rect.width+","+step+","+be);

			RectTransform contentTrans = content.GetComponent<RectTransform> ();
			float sizeHeight = be + (step*(count/3+(count%3>0?1:0)));
			Debug.Log("sizeHeight:"+sizeHeight);
			contentTrans.sizeDelta = new Vector2 (0,sizeHeight);
			int allStarCount = 0;
            Object lockPic = Resources.Load("lock");
			for (int i = 0; i < count; i++) { // todo 这个地方要分段显示
				int x = i%3;
				int y = i/3;
				up = Instantiate(levelItem) as GameObject;

				up.transform.localPosition = new Vector3 (be+x*step, -be-y*step,0);
				up.transform.localScale = new Vector3 (1,1,1);
				up.transform.SetParent(content.transform,false);

				Button b1 = up.GetComponent<Button> ();

				ButtonIndex buttonIndex = up.GetComponent<ButtonIndex> ();
				buttonIndex.pass = i+1;
				buttonIndex.star = 0;
                buttonIndex.isOpen = true;
				if(level.StarInLevel.Count>i){
					buttonIndex.star = level.StarInLevel[i];
					allStarCount += buttonIndex.star;
                }else if(level.StarInLevel.Count < i){
                    GameObject lockOb = Instantiate(lockPic) as GameObject;
                    //lockOb.transform.localPosition = new Vector3(0,0, 0);
                    //lockOb.transform.localScale = new Vector3(1, 1, 1);
                    lockOb.transform.SetParent(up.transform, false);
                    buttonIndex.isOpen = false;
                }
                if (level.StarInLevel.Count >= i)
                {
                    b1.onClick.AddListener(delegate ()
                    {
                        Sound.playSound(SoundType.Click);
                        OnClick(buttonIndex);
                    });
                }
                else
                {
                    b1.onClick.AddListener(delegate ()
                    {
                        Sound.playSound(SoundType.Click);
                        WarnDialog.showWarnDialog(Message.getText("passNotOpen"));
                    });
                }


//				GameObject textGo = up.transform.Find ("Text").gameObject;
//				Text text = textGo.GetComponent<Text> ();
//				text.text = Message.getText("levelItem",buttonIndex.pass,buttonIndex.star);
			}
			// 星数
			GameObject starTextGo = GameObject.Find ("main/ui/uiLevel/Canvas/star/Text");
			Text starText = starTextGo.GetComponent<Text>();
			starText.text = allStarCount.ToString();
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
			//GameObject energyTextGo = GameObject.Find ("main/ui/uiUnlimit/Canvas/energy/Text");
			//Text energyText = energyTextGo.GetComponent<Text>();
			//energyText.text = Params.energy.ToString();
			// 每日星数奖励
			//ret.TodayStar
			Slider slider = unlimitedAwardGo.transform.Find("starSlider").GetComponent<Slider>();
			slider.value = ret.TodayStar;
			float step = slider.maxValue / unlimitedAwardSegs.Length;

			string[] isLight = null;
			if (ret.Award .Length > 0)
			{
				isLight = ret.Award.Split(';');
			}

			for (int i = 0; i < unlimitedAwardSegs.Length; i++)
			{
				//Debug.Log(ret.TodayStar + "," + unlimitedAwardSegs[i].star);
				// 是否还没点亮
				if (ret.TodayStar < unlimitedAwardSegs[i].star)
				{
					unlimitedAwardSegs[i].image.sprite = SpriteCache.getSprite("box/unreach");
					unlimitedAwardSegs[i].state = 0;
				}
				else if (isLight != null && int.Parse(isLight[i]) > 0)
				{
					unlimitedAwardSegs[i].image.sprite = SpriteCache.getSprite("box/open");
					unlimitedAwardSegs[i].state = 2;
				}
				else
				{
					unlimitedAwardSegs[i].image.sprite = SpriteCache.getSprite("box/reach");
					unlimitedAwardSegs[i].state = 1;
				}
			}


			// 列表
			int count = ret.UnlimitedRankInfo.Count;
			float dis = 0f;

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
				GameObject textGo = up.transform.Find ("rank").gameObject;
                Text text = textGo.GetComponent<Text> ();
                text.text = info.Rank+"";
				//text.text = Message.getText("unlimitRankItem",info.Rank,info.UserName,info.Star);
                textGo = up.transform.Find("name").gameObject;
                text = textGo.GetComponent<Text>();
                text.text = info.UserName + "";
                textGo = up.transform.Find("pass").gameObject;
                text = textGo.GetComponent<Text>();
                text.text = info.Pass + "";
                textGo = up.transform.Find("star").gameObject;
                text = textGo.GetComponent<Text>();
                text.text = info.Star + "";
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

			scoreText.text = Message.getText("onlineScore",ret.Score);
			titleText.text = Message.getText("onlineTitle",ret.Title);
			rankText.text = Message.getText("onlineRank",ret.Rank);


			GameObject content = GameObject.Find ("main/ui/uiOnline/Canvas/scrollView/Viewport/Content");
			for (int i = 0; i < content.transform.childCount; i++) {
				Destroy (content.transform.GetChild(i).gameObject);		
			}

			//获取按钮游戏对象
            Object button = Resources.Load ("OnlineItem");

			// 列表
			int count = ret.RankInfos.Count;
			float dis = 0f;

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
                GameObject textGo = up.transform.Find("rank").gameObject;
                Text text = textGo.GetComponent<Text>();
                text.text = info.Rank + "";
                //text.text = Message.getText("unlimitRankItem",info.Rank,info.UserName,info.Star);
                textGo = up.transform.Find("name").gameObject;
                text = textGo.GetComponent<Text>();
                text.text = info.Name + "";
                textGo = up.transform.Find("pvpTimes").gameObject;
                text = textGo.GetComponent<Text>();
                text.text = info.PvpTime + "";
                textGo = up.transform.Find("score").gameObject;
                text = textGo.GetComponent<Text>();
                text.text = info.Score + "";
                textGo = up.transform.Find("ladder").gameObject;
                text = textGo.GetComponent<Text>();
                text.text = info.Title + "";
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
		createMap(MapMode.Online,matchingSuccess.Map.ToArray(),matchingSuccess.Beans,matchingSuccess.Time,matchingSuccess.Speed,matchingSuccess.Start,matchingSuccess.End,1,0,matchingSuccess.OtherInfos,null,null,null);
		ui.SetActive (false);

		matching.matchSuccess ();
	}

	// Update is called once per frame
	void Update () {
	}

	public void OnPvpClick(){
		

		CSMatching csmatching = new CSMatching ();
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSMatching, CSMatching.SerializeToBytes(csmatching),delegate(int opcode, byte[] data) {
			Debug.Log("send matching success,opcode = "+opcode);
			matching.show (float.Parse (sysParas ["matchWaitTime"]));
			//matchWaitTime
//			matchingDialogId = WarnDialog.showWaitDialog (Message.getText("matching"), int.Parse (sysParas ["matchWaitTime"]), delegate() {
//				WarnDialog.showWarnDialog("match fail , please try again later.",null);	
//				//
//				CSCancelMatching cancelMatching = new CSCancelMatching();
//				SocketManager.SendMessageAsyc((int)MiGongOpcode.CSCancelMatching,CSCancelMatching.SerializeToBytes(cancelMatching),delegate(int retOpcode, byte[] retData) {
//
//				});
//			},delegate() {
//				//
//				CSCancelMatching cancelMatching = new CSCancelMatching();
//				SocketManager.SendMessageAsyc((int)MiGongOpcode.CSCancelMatching,CSCancelMatching.SerializeToBytes(cancelMatching),delegate(int retOpcode, byte[] retData) {
//
//				});
//			});
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
//			if(ret.PassRewardStar4 != null){
//				showRewardStarInfo(4,ret.PassRewardStar4);
//			}

			this.showRewardPass = buttonIndex.pass;
		});
	}

	private void showRewardStarInfo(int starIndex,PBPassReward passReward){
		if(passReward != null){
			Transform parent = uiLevel.transform.Find ("Canvas/showReward/dialog/star" + starIndex+"/reward").transform;
			Util.clearChildren (parent);
			// 显示金币
//			ShopItem.getGoldImage();
			Object rewardItemObj = Resources.Load("rewardItem");
			int step = 60;
			int x0 = 20;
			int index = 0;
			if(passReward.Gold>0){
				GameObject go = Instantiate (rewardItemObj) as GameObject;
				Image image = go.GetComponent<Image> ();
				image.sprite = ShopItem.getGoldSprite ();
				image.transform.Find ("num").GetComponent<Text>().text ="x"+passReward.Gold;
				go.transform.localPosition = new Vector3 (x0+step*index++,0,0);
				go.transform.SetParent (parent,false);
			}
			if(passReward.Energy>0){
				GameObject go = Instantiate (rewardItemObj) as GameObject;
				Image image = go.GetComponent<Image> ();
				image.sprite = ShopItem.getEnergySprite ();
				image.transform.Find ("num").GetComponent<Text>().text ="x"+passReward.Energy;
				go.transform.localPosition = new Vector3 (x0+step*index++,0,0);
				go.transform.SetParent (parent,false);
			}

			if(passReward.Item!= null && passReward.Item.Count>0){
				foreach(PBItem item in passReward.Item){
					GameObject go = Instantiate (rewardItemObj) as GameObject;
					Image image = go.GetComponent<Image> ();
					image.sprite = ShopItem.getSprite (item.ItemId);
					image.transform.Find ("num").GetComponent<Text>().text ="x"+item.Count;
					go.transform.localPosition = new Vector3 (x0+step*index++,0,0);
					go.transform.SetParent (parent,false);
				}
			}
		}
	}

	public void onIntoPassClick(){
		CSGetMiGongMap miGongMap = new CSGetMiGongMap ();
		miGongMap.Pass = showRewardPass;
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSGetMiGongMap, CSGetMiGongMap.SerializeToBytes (miGongMap),delegate(int opcode, byte[] ret) {
			uiLevel.transform.Find("Canvas/showReward").gameObject.SetActive(false);
			ui.SetActive (false);
			SCGetMiGongMap scmap = SCGetMiGongMap.Deserialize(ret);
			// 剩余精力
			Params.startEnergySchedule(scmap.Energy.Energy,scmap.Energy.RefreshTime);
			int[] stars= {scmap.Star1,scmap.Star2,scmap.Star3,scmap.Star4};

            createMap (MapMode.Level,scmap.Map.ToArray (),scmap.Beans,scmap.Time, scmap.Speed,scmap.Start, scmap.End, scmap.Pass,scmap.EnemyCount,null,stars,scmap.Route,scmap.Items);
		});
	}

    private void createMap(MapMode mode, int[] mapInt, List<PBBeanInfo> beans, int time, int speed, int start, int end, int pass, int enemyCount,
                           List<PBOtherInfo> otherInfos,int[] stars,string guideRoute,List<PBItem> skillItems){
		Object gamePanel = Resources.Load ("GamePanel");
		GameObject gamePanelGo = Instantiate(gamePanel) as GameObject;
		GameObject mapGo = gamePanelGo.transform.Find ("content/map").gameObject;
		MapCreate mapCreate = mapGo.GetComponent<MapCreate> ();

		// 创建引导
        if (guideRoute != null && (!guideStep.ContainsKey((int)mode) || guideStep[(int)mode] == 0)) {
			string[] routes = guideRoute.Split (';');
			int[] routeInt = new int[routes.Length];
			for (int i = 0, len = routes.Length; i < len; i++) {
				routeInt [i] = int.Parse (routes [i]);
			}
			mapCreate.route = routeInt;
			mapCreate.needGuide = true;
		}


		mapCreate.Pass = pass;
        mapCreate.enemyCount = enemyCount;

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
	Pass = 0,
    Unlimited = 1,
	Pvp = 2
}

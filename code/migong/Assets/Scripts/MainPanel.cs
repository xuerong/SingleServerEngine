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
	// Use this for initialization
	void Start () {
		show (uiMain);
		// 给主界面按钮加事件
		string canvasPath = "main/ui/uiMain/Canvas/";
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
			show (uiUnlimit);
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

		// 联网对战按钮
//		GameObject pvpBattle = Instantiate(button) as GameObject;
//		pvpBattle.transform.SetParent (canvasGo.transform);
//		pvpBattle.transform.position = new Vector3 (width/2, height/8 * (count),0);
//		Button bt = pvpBattle.GetComponent<Button> ();
//		bt.onClick.AddListener (delegate() {OnPvpClick();});
//		//
//		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCMatchingSuccess, matchSuccess);
//		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCMatchingFail, delegate(int opcode, byte[] receiveData) {
//			Debug.Log("");
//		});
//		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCBegin, delegate(int opcode, byte[] receiveData) {
//			Debug.Log("");
//		});
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
			int count = level.PassCountInLevel.Count;
			float dis = 20f;

			GameObject up = Instantiate(button) as GameObject;
			RectTransform buRec = up.GetComponent<RectTransform> ();
			Destroy(up);

			RectTransform contentTrans = content.GetComponent<RectTransform> ();
			contentTrans.sizeDelta = new Vector2 ((buRec.rect.width + dis) * count + dis,contentTrans.rect.height);

			for (int i = 0; i < count; i++) {
				up = Instantiate(button) as GameObject;
				up.transform.parent = content.transform;
				up.transform.localPosition = new Vector3 ((buRec.rect.width+dis)*i+dis, 0,0);
				up.transform.localScale = new Vector3 (1,1,1);

				Button b1 = up.GetComponent<Button> ();

				ButtonIndex buttonIndex = up.GetComponent<ButtonIndex> ();
				buttonIndex.index = "level" + (i + 1);
				buttonIndex.level = (i + 1);
				b1.onClick.AddListener (delegate() {OnClick(buttonIndex);});

				GameObject textGo = up.transform.Find ("Text").gameObject;
				Text text = textGo.GetComponent<Text> ();
				int curPass;
				if (i == level.OpenLevel) {
					curPass = level.OpenPass;
				} else if (i < level.OpenLevel) {
					curPass = level.PassCountInLevel [i];
				} else {
					curPass = 0;
				}
				text.text = "level"+i+"("+curPass+"/"+level.PassCountInLevel[i]+")";
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
		createMap(1,matchingSuccess.Map.ToArray(),matchingSuccess.Start,matchingSuccess.End,1,1,matchingSuccess.OtherInfos);
	}

	// Update is called once per frame
	void Update () {
	}

	public void OnPvpClick(){
		CSMatching matching = new CSMatching ();
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSMatching, CSMatching.SerializeToBytes(matching),delegate(int opcode, byte[] data) {
			Debug.Log("send matching success,opcode = "+opcode);
		});
	}

	public void OnClick(ButtonIndex buttonIndex){
		CSGetMiGongMap miGongMap = new CSGetMiGongMap ();
		miGongMap.Level = buttonIndex.level;
		miGongMap.Pass = 1;
		byte[] data = CSGetMiGongMap.SerializeToBytes (miGongMap);
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSGetMiGongMap, data,delegate(int opcode, byte[] ret) {
			SCGetMiGongMap scmap = SCGetMiGongMap.Deserialize(ret);
			createMap (0,scmap.Map.ToArray (), scmap.Start, scmap.End, buttonIndex.level, 1,null);
		});
		ui.SetActive (false);
	}
	private void createMap(int mode,int[] mapInt,int start,int end,int level,int pass,List<PBOtherInfo> otherInfos){
		Object gamePanel = Resources.Load ("GamePanel");
		GameObject gamePanelGo = Instantiate(gamePanel) as GameObject;
		GameObject mapGo = gamePanelGo.transform.Find ("map").gameObject;
		MapCreate mapCreate = mapGo.GetComponent<MapCreate> ();
		mapCreate.Level = level;
		mapCreate.Pass = pass;

		mapCreate.Mode = mode;

		int size = (int)Mathf.Sqrt(mapInt.Length);
		mapCreate.map = new int[size][];
		for(int i=0;i<size;i++){
			mapCreate.map[i] = new int[size];
			for(int j=0;j<size;j++){
				mapCreate.map[i][j] = mapInt[i*size+j];
			}
		}
		mapCreate.startPoint = new Vector2 (start%size,start/size);
		mapCreate.endPoint = new Vector2 (end%size,end/size);
		mapCreate.size = size;
		Debug.Log("map size:"+size+",End:"+end);

		gamePanelGo.transform.parent = transform;
		gamePanelGo.transform.localPosition = new Vector3(0,0,0);

		// 
		if(mode == 1 && otherInfos != null){
			Object pacmanIns = Resources.Load ("pacman");
			foreach (PBOtherInfo otherInfo in otherInfos) {
				GameObject pacmanGo = Instantiate(pacmanIns) as GameObject;

				Pacman pacman = pacmanGo.GetComponent<Pacman> ();
				pacman.userId = otherInfo.UserId;

				pacman.mapCreate = mapCreate;

				pacmanGo.transform.parent = gamePanelGo.transform;
			}
		}
	}
}

using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using com.protocol;
using Example;

public class MainPanel : MonoBehaviour {
	public GameObject camera;
	// Use this for initialization
	void Start () {
		//获取按钮游戏对象
		Object button = Resources.Load ("Button");
		GameObject btnObj = GameObject.Find ("main/Canvas");
		RectTransform rts = btnObj.GetComponent<RectTransform> ();
		float width = rts.rect.width;
		float height = rts.rect.height;

		// 获取当前关卡
		CSGetMiGongLevel getMiGongLevel = new CSGetMiGongLevel();
		byte[] data = SocketManager.SendMessageSync ((int)MiGongOpcode.CSGetMiGongLevel, CSGetMiGongLevel.SerializeToBytes (getMiGongLevel));
		SCGetMiGongLevel level = SCGetMiGongLevel.Deserialize (data);
		int count = level.PassCountInLevel.Count;
		for (int i = 0; i < count; i++) {

			GameObject up = Instantiate(button) as GameObject;
			up.transform.SetParent (btnObj.transform);
			up.transform.position = new Vector3 (width/2, height/8 * (i+1),0);

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
		// 联网对战按钮
		GameObject pvpBattle = Instantiate(button) as GameObject;
		pvpBattle.transform.SetParent (btnObj.transform);
		pvpBattle.transform.position = new Vector3 (width/2, height/8 * (count),0);
		Button bt = pvpBattle.GetComponent<Button> ();
		bt.onClick.AddListener (delegate() {OnPvpClick();});
		//
		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCMatchingSuccess, matchSuccess);
		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCMatchingFail, delegate(int opcode, byte[] receiveData) {
			Debug.Log("");
		});
		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCBegin, delegate(int opcode, byte[] receiveData) {
			Debug.Log("");
		});
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

		byte[] ret = SocketManager.SendMessageSync ((int)MiGongOpcode.CSGetMiGongMap, data);

		SCGetMiGongMap scmap = SCGetMiGongMap.Deserialize(ret);

		createMap (0,scmap.Map.ToArray (), scmap.Start, scmap.End, buttonIndex.level, 1,null);
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

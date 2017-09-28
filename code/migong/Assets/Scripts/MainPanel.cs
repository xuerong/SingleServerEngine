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
			up.transform.position = new Vector3 (width/2, height/6 * (i+1),0);

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
	}
	
	// Update is called once per frame
	void Update () {
	}


	public void OnClick(ButtonIndex buttonIndex){
		Debug.Log ("aaaa"+buttonIndex.index);
		CSGetMiGongMap miGongMap = new CSGetMiGongMap ();
		miGongMap.Level = buttonIndex.level;
		miGongMap.Pass = 1;
		byte[] data = CSGetMiGongMap.SerializeToBytes (miGongMap);

		byte[] ret = SocketManager.SendMessageSync ((int)MiGongOpcode.CSGetMiGongMap, data);

		Object gamePanel = Resources.Load ("GamePanel");
		GameObject gamePanelGo = Instantiate(gamePanel) as GameObject;
		GameObject mapGo = gamePanelGo.transform.Find ("map").gameObject;
		MapCreate mapCreate = mapGo.GetComponent<MapCreate> ();
		mapCreate.Level = buttonIndex.level;
		mapCreate.Pass = 1;

		SCGetMiGongMap scmap = SCGetMiGongMap.Deserialize(ret);
		int[] mapInt = scmap.Map.ToArray();
		int size = (int)Mathf.Sqrt(mapInt.Length);
		mapCreate.map = new int[size][];
		for(int i=0;i<size;i++){
			mapCreate.map[i] = new int[size];
			for(int j=0;j<size;j++){
				mapCreate.map[i][j] = mapInt[i*size+j];
			}
		}
		Debug.Log("map size:"+size);

		gamePanelGo.transform.parent = transform;
		gamePanelGo.transform.localPosition = new Vector3(0,0,0);
	}
}

using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;

public class MainPanel : MonoBehaviour {

	// Use this for initialization
	void Start () {
		//获取按钮游戏对象
		Object button = Resources.Load ("Button");
		GameObject btnObj = GameObject.Find ("main/Canvas");
		RectTransform rts = btnObj.GetComponent<RectTransform> ();
		float width = rts.rect.width;
		float height = rts.rect.height;
		for (int i = 0; i < 4; i++) {

			GameObject up = Instantiate(button) as GameObject;
			up.transform.SetParent (btnObj.transform);
			up.transform.position = new Vector3 (width/2, height/6 * i,0);

			Button b1 = up.GetComponent<Button> ();
			ButtonIndex buttonIndex = up.GetComponent<ButtonIndex> ();
			buttonIndex.index = "level"+i;
			b1.onClick.AddListener (delegate() {OnClick(buttonIndex);});
		}
	}
	
	// Update is called once per frame
	void Update () {
		
	}


	public void OnClick(ButtonIndex buttonIndex){
		Debug.Log ("aaaa"+buttonIndex.index);
	}
}

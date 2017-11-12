using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class Sound : MonoBehaviour {
	public static bool on = true;

	public GameObject off;
	// Use this for initialization
	void Start () {
		string canvasPath = "main/ui/uiMain/Canvas/sound";
		Button levelButton = GameObject.Find (canvasPath).GetComponent<Button>();
		levelButton.onClick.AddListener (delegate() {
			// 打开level 界面
			clickSound();
		});
	}

	public void clickSound(){
		if (on) {
			on = false;
			off.SetActive (true);
		} else {
			on = true;
			off.SetActive (false);
		}
	}
}

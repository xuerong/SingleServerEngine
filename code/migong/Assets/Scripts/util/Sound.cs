using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class Sound : MonoBehaviour {
	public static bool on = true;

	public GameObject off;

	private static Dictionary<SoundType,AudioSource> soundDic = new Dictionary<SoundType, AudioSource> ();
	// Use this for initialization
	void Start () {
		string canvasPath = "main/ui/uiMain/Canvas/sound";
		Button levelButton = GameObject.Find (canvasPath).GetComponent<Button>();
		levelButton.onClick.AddListener (delegate() {
			// 打开level 界面
			clickSound();
		});

		//
		AudioSource[] audioSources = GetComponents<AudioSource>();
		for (int i = 0; i < audioSources.Length; i++) {
			soundDic [(SoundType)i] = audioSources [i];
		}
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

	public static void playSound(SoundType soundType){
		soundDic [soundType].Play ();
	}
}

public enum SoundType{
	Click=0,
	Move,
	EatBean,
	StarAdd,
	Arrive,
	Over
}

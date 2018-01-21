using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using Example;
using com.protocol;

public class Matching : MonoBehaviour {
	public Text timeText;
	public Button cancel;

	bool isMatching = false;

	float currentTime;
	float maxTime;

	void Awake(){
		cancel.onClick.AddListener (delegate() {
			Sound.playSound(SoundType.Click);
			cancelMatching();
		});
		//gameObject.SetActive(false);
	}
	// Update is called once per frame
	void Update () {
		if (isMatching) {
			currentTime += Time.deltaTime;
			if (currentTime >= maxTime) {
				WarnDialog.showWarnDialog (Message.getText("matchingFailWarning"));	
				cancelMatching ();
			} else {
				timeText.text=(int)currentTime+"s";
			}
		}
	}

	private void cancelMatching(){
		isMatching = false;
		gameObject.SetActive(false);
		CSCancelMatching cancelMatching = new CSCancelMatching();
		SocketManager.SendMessageAsyc((int)MiGongOpcode.CSCancelMatching,CSCancelMatching.SerializeToBytes(cancelMatching),delegate(int retOpcode, byte[] retData) {

		});
	}

	public void show(float time){
		currentTime = 0;
		maxTime = time;
		isMatching = true;
		gameObject.SetActive(true);
	}

	public void matchSuccess(){
		if (isMatching) {
			isMatching = false;
			gameObject.SetActive(false);
		}
	}
	public void matchFail(){
		if (isMatching) {
			isMatching = false;
			gameObject.SetActive(false);
		}
	}
}

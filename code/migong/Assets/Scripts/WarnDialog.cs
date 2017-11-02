using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using System.Threading;

public delegate void DialogOkAction();

class WarnDialogInfo{
	public string text;
	public DialogOkAction okAction;
}

public class WarnDialog : MonoBehaviour {
	static WarnDialog instance;
	static WarnDialogInfo otherThread;
	DialogOkAction okAction;
	Text text;

	void Awake(){
		Button close = transform.Find ("Canvas/close").GetComponent<Button> ();
		close.onClick.AddListener (delegate() {
			gameObject.SetActive(false);
		});
		Button ok = transform.Find ("Canvas/ok").GetComponent<Button> ();
		ok.onClick.AddListener (delegate() {
			gameObject.SetActive(false);
			if(okAction != null){
				okAction.Invoke();
			}
		});
		text = transform.Find ("Canvas/text").GetComponent<Text> ();
		instance = this;
		instance.gameObject.SetActive (false);
	}

	void Update(){
		if (otherThread != null) {
			WarnDialogInfo w = otherThread;
			otherThread = null;
			showWarnDialog (w.text,w.okAction);
		}
	}


	public static void showWarnDialog(string text,DialogOkAction dialogOkAction){
		instance.okAction = dialogOkAction;
		instance.text.text = text;
		instance.gameObject.SetActive (true);
	}

	public static void showWarnDialogOtherThread(string text,DialogOkAction dialogOkAction){
		WarnDialogInfo w = new WarnDialogInfo ();
		w.text = text;
		w.okAction = dialogOkAction;
		otherThread = w;
	}
}

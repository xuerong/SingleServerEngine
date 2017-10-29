using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public delegate void DialogOkAction();

public class WarnDialog : MonoBehaviour {
	static WarnDialog instance;
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


	public static void showWarnDialog(string text,DialogOkAction dialogOkAction){
		instance.okAction = dialogOkAction;
		instance.text.text = text;
		instance.gameObject.SetActive (true);
	}
}

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
	static GameObject canvas;

	static Queue<WarnDialogInfo> dialogQueue = new Queue<WarnDialogInfo>();

	DialogOkAction okAction;
	Text text;

	void Awake(){
		Button close = transform.Find ("Canvas/close").GetComponent<Button> ();
		close.onClick.AddListener (delegate() {
			canvas.SetActive(false);
		});
		Button ok = transform.Find ("Canvas/ok").GetComponent<Button> ();
		ok.onClick.AddListener (delegate() {
			canvas.SetActive(false);
			if(okAction != null){
				okAction.Invoke();
			}
		});
		text = transform.Find ("Canvas/text").GetComponent<Text> ();
		instance = this;
		canvas = transform.Find ("Canvas").gameObject;
		closeDialog ();
	}

	void Update(){
		if (dialogQueue.Count > 0 && !canvas.activeSelf) {
			lock (dialogQueue) {
				WarnDialogInfo w = dialogQueue.Dequeue ();
				instance.text.text = w.text;
				instance.okAction = w.okAction;
				openDialog ();
			}
		}
	}


	public static void showWarnDialog(string text,DialogOkAction dialogOkAction){
		WarnDialogInfo w = new WarnDialogInfo ();
		w.text = text;
		w.okAction = dialogOkAction;
		lock (dialogQueue) {
			dialogQueue.Enqueue (w);
		}
	}

	private static void closeDialog(){
		canvas.SetActive (false);
	}
	private static void openDialog(){
		canvas.SetActive (true);
	}
}

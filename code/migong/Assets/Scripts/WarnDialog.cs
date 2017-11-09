using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using System.Threading;

// wait转定时器的代理
public delegate void MyDelegate(AfterWaitAction afterWaitAction);
//
public delegate void DialogOkAction();
public delegate void AfterWaitAction();



class WarnDialogInfo{
	public string text;
	public DialogOkAction okAction;
}

class WaitDialogInfo{
	public string text;
	public int time;
	public AfterWaitAction afterWaitAction;
}

public class WarnDialog : MonoBehaviour {
	static WarnDialog instance;
	static GameObject canvas;

	static Queue<WarnDialogInfo> dialogQueue = new Queue<WarnDialogInfo>();
	static Queue<WaitDialogInfo> waitDialogQueue = new Queue<WaitDialogInfo>();

	DialogOkAction okAction;
	Text text;

	private static Button close;
	private static Button ok;
	void Awake(){
		close = transform.Find ("Canvas/close").GetComponent<Button> ();
		close.onClick.AddListener (delegate() {
			canvas.SetActive(false);
		});
		ok = transform.Find ("Canvas/ok").GetComponent<Button> ();
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
				showButton (true,true);
				WarnDialogInfo w = dialogQueue.Dequeue ();
				instance.text.text = w.text;
				instance.okAction = w.okAction;
				openDialog ();
			}
		}
		if (waitDialogQueue.Count > 0 && !canvas.activeSelf) {
			lock (waitDialogQueue) {
				showButton (false,false);
				WaitDialogInfo w = waitDialogQueue.Dequeue ();
				instance.text.text = w.text;
				openDialog ();
				Job.startJob (new MyDelegate(afterWait),w.time,w.afterWaitAction);
			}
		}
	}
	public void afterWait(AfterWaitAction afterWaitAction){
		closeDialog ();
		if (afterWaitAction != null) {
			afterWaitAction.Invoke ();
		}
	}

	public static void showWaitDialog(string text,int time,AfterWaitAction afterWaitAction){
		WaitDialogInfo w = new WaitDialogInfo ();
		w.text = text;
		w.time = time;
		w.afterWaitAction = afterWaitAction;
		lock (waitDialogQueue) {
			waitDialogQueue.Enqueue (w);
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

	private static void showButton(bool okButton,bool closeButton){
		ok.gameObject.SetActive (okButton?true:false);
		close.gameObject.SetActive (closeButton?true:false);
	}

	private static void closeDialog(){
		canvas.SetActive (false);
	}
	private static void openDialog(){
		canvas.SetActive (true);
	}
}

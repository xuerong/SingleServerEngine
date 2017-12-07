using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using System.Threading;

// wait转定时器的代理
delegate void MyDelegate(WaitDialogInfo waitDialogInfo);
//
delegate void DialogOkAction();
delegate void AfterWaitAction();


enum DialogState{
	Before,
	Showing,
	Cancel
}
abstract class DialogInfo{
	public int id;
	public DialogState state = DialogState.Before;
}
class WarnDialogInfo : DialogInfo{
	public string text;
	public DialogOkAction okAction;
	public bool hideClose;
	public float deltaX;
	public float deltaY;
}

class WaitDialogInfo : DialogInfo{
	public string text;
	public int time;
	public AfterWaitAction afterWaitAction;
	public DialogOkAction cancelAction;
}



class WarnDialog : MonoBehaviour {
	static WarnDialog instance;
	static GameObject canvas;

	static Queue<WarnDialogInfo> dialogQueue = new Queue<WarnDialogInfo>();
	static Queue<WaitDialogInfo> waitDialogQueue = new Queue<WaitDialogInfo>();

	static Dictionary<int,DialogInfo> dialogInfos = new Dictionary<int, DialogInfo> ();

	DialogOkAction okAction;
	Text text;

	private static RectTransform panelTras;
	private static Button close;
	private static Button ok;

	private static int id = 1; // from 1
	void Awake(){ 
		close = transform.Find ("Canvas/Panel/close").GetComponent<Button> ();
		close.onClick.AddListener (delegate() {
			Sound.playSound(SoundType.Click);
			canvas.SetActive(false);
		});
		ok = transform.Find ("Canvas/Panel/ok").GetComponent<Button> ();
//		ok.onClick.AddListener (delegate() {
//			canvas.SetActive(false);
//		Sound.playSound(SoundType.Click);
//			if(okAction != null){
//				okAction.Invoke();
//			}
//		});
		text = transform.Find ("Canvas/Panel/text").GetComponent<Text> ();
		instance = this;
		canvas = transform.Find ("Canvas").gameObject;
		panelTras = transform.Find ("Canvas/Panel").GetComponent<RectTransform>();
		closeDialog (0);
	}

	void Update(){
		if (dialogQueue.Count > 0 && !canvas.activeSelf) {
			ok.onClick.RemoveAllListeners ();
			lock (dialogQueue) {
				ok.transform.Find("Text").GetComponent<Text>().text = Message.getText("ok");
				WarnDialogInfo w = dialogQueue.Dequeue ();
				showButton (true,w.hideClose?false:true);
				instance.text.text = w.text;
//				instance.okAction = w.okAction;

				ok.onClick.AddListener (delegate() {
					Sound.playSound(SoundType.Click);
					canvas.SetActive(false);
					if(w.okAction != null){
						w.okAction.Invoke();
					}
				});
				panelTras.localPosition = new Vector3 (w.deltaX,w.deltaY,0);
				openDialog ();
				w.state = DialogState.Showing;
			}
		}
		if (waitDialogQueue.Count > 0 && !canvas.activeSelf) {
			ok.onClick.RemoveAllListeners ();
			lock (waitDialogQueue) {
				ok.transform.Find("Text").GetComponent<Text>().text = Message.getText("cancel");
				showButton (true,false);
				WaitDialogInfo w = waitDialogQueue.Dequeue ();
				if (w.state == DialogState.Cancel) {
					return;
				}
				instance.text.text = w.text;
//				instance.okAction = w.afterWaitAction;

				ok.onClick.AddListener (delegate() {
					Sound.playSound(SoundType.Click);
					WarnDialog.closeWaitDialog(w.id);
					if (w.cancelAction != null) {
						w.cancelAction.Invoke ();
					}
				});

				openDialog ();
				w.state = DialogState.Showing;
				Job.startJob (new MyDelegate(afterWait),w.time,w);
			}
		}
	}
	public void afterWait(WaitDialogInfo waitDialogInfo){
		if (waitDialogInfo.state == DialogState.Cancel) {
			return;
		}
		closeDialog (waitDialogInfo.id);
		if (waitDialogInfo.afterWaitAction != null) {
			waitDialogInfo.afterWaitAction.Invoke ();
		}
	}

	public static int showWaitDialog(string text,int time,AfterWaitAction afterWaitAction,DialogOkAction cancelAction){
		WaitDialogInfo w = new WaitDialogInfo ();
		w.text = text;
		w.time = time;
		w.afterWaitAction = afterWaitAction;
		w.id = Interlocked.Increment (ref id);
		w.cancelAction = cancelAction;
		lock (waitDialogQueue) {
			waitDialogQueue.Enqueue (w);
			dialogInfos.Add (w.id,w);
		}
		return w.id;
	}
	public static void closeWaitDialog(int id){
		// 拿到，如果是未显示状态（变为取消），显示状态（关闭，），
		lock (waitDialogQueue) {
			if (dialogInfos.ContainsKey (id)) {
				DialogInfo dialogInfo = dialogInfos [id];
				if (dialogInfo.state == DialogState.Showing) {
					closeDialog (id);
				}
				dialogInfo.state = DialogState.Cancel;
			} else {
				Debug.Log ("id = " + id + " is not exist");
			}
		}
	}

	public static void showWarnDialog(string text){
		showWarnDialog (text,null);
	}
	public static void showWarnDialog(string text,DialogOkAction dialogOkAction){
		showWarnDialog (text,dialogOkAction,false,0,0);
	}

	public static void showWarnDialog(string text,DialogOkAction dialogOkAction,bool hideClose){
		showWarnDialog (text,dialogOkAction,hideClose,0,0);
	}
	public static void showWarnDialog(string text,DialogOkAction dialogOkAction,bool hideClose,float deltaX,float deltaY){
		WarnDialogInfo w = new WarnDialogInfo ();
		w.text = text;
		w.okAction = dialogOkAction;
		w.hideClose = hideClose;
		w.deltaX = deltaX;
		w.deltaY = deltaY;
		lock (dialogQueue) {
			dialogQueue.Enqueue (w);
		}
	}

	private static void showButton(bool okButton,bool closeButton){
		ok.gameObject.SetActive (okButton?true:false);
		close.gameObject.SetActive (closeButton?true:false);
	}
	private static void closeDialog(int id){
		canvas.SetActive (false);
		if (dialogInfos.ContainsKey (id)) {
			dialogInfos.Remove (id);
		}
	}
	private static void openDialog(){
		canvas.SetActive (true);
	}
}

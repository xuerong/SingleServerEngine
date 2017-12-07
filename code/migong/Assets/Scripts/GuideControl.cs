using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using Example;
using com.protocol;

public class GuideControl : MonoBehaviour {

	public Text text;

	public GameObject firstGo;
	public GameObject secondGo;
	public GameObject thirdGo;
	public GameObject forthGo;


	public void showHelp(bool guide){
		transform.gameObject.SetActive (true);
		PassGuide current = new PassGuide (firstGo,text, PassGuideType.First,
			new PassGuide (secondGo,text, PassGuideType.Second,
				new PassGuide (thirdGo,text, PassGuideType.Third,
					new PassGuide (forthGo,text, PassGuideType.Forth, null))));
		
		showImage(current.imageGo);
		current.doAction ();

		Button button = transform.Find ("Canvas/mask").GetComponent<Button> ();
		button.onClick.RemoveAllListeners ();
		button.onClick.AddListener(delegate {
			Sound.playSound(SoundType.Click);
			current = current.next;
			if(current != null){
				showImage(current.imageGo);
				current.doAction ();
			}else {
				transform.gameObject.SetActive(false);
				if(guide){
					// 完成这个新手引导，发送后端
					CSNewGuideFinish guideFinish = new CSNewGuideFinish();
					guideFinish.Id = (int)GuideType.Pass;
					guideFinish.Step = 1;
					SocketManager.SendMessageAsyc((int)MiGongOpcode.CSNewGuideFinish,CSNewGuideFinish.SerializeToBytes(guideFinish),delegate(int opcode, byte[] data) {
						SCNewGuideFinish ret = SCNewGuideFinish.Deserialize(data);
						GameObject mainGo = GameObject.Find ("main");
						MainPanel mainPanel = mainGo.GetComponent<MainPanel>();
						mainPanel.setGuideState(GuideType.Pass,1);
					});
				}
			}
		});
	}

	private void showImage(GameObject imageGo){
		firstGo.SetActive (false);
		secondGo.SetActive (false);
		thirdGo.SetActive (false);
		forthGo.SetActive (false);
		imageGo.SetActive (true);
	}


	/**
 * * 首先，需要提示的条目有：
		 * 1、出发点-路径-结束点
		 * 2、时间提示
		 * 3、豆子提示-星级
		 * 4、开始吧！
		 */
	class PassGuide{
		private Text text;

		public PassGuideType type;
		public PassGuide next;

		public GameObject imageGo;

		public PassGuide(GameObject imageGo,Text text,PassGuideType type,PassGuide next){
			this.text = text;
			this.type = type;
			this.next = next;
			this.imageGo = imageGo;
		}

		public void doAction(){
			string text = "";
			switch (type) {
			case PassGuideType.First:
				// 出发点

				text = Message.getText ("guideContent1");
				break;
			case PassGuideType.Second:

				text = Message.getText ("guideContent2");
				break;
			case PassGuideType.Third:
				text = Message.getText ("guideContent3");
				break;
			case PassGuideType.Forth:
				text = Message.getText ("guideContent4");
				break;
			}

			this.text.text = text;
		}
	}
}
enum PassGuideType{
	First,
	Second,
	Third,
	Forth
}
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using Example;
using com.protocol;

[System.Serializable]
public class HelpItem{
    public GameObject go;
    public GameObject[] gos;
}

public class Help : MonoBehaviour {

    public Button close;

	public Text text;

    public Button[] btns;
    public HelpItem[] gos;

    void Awake(){
        close.onClick.AddListener(delegate {
            Sound.playSound(SoundType.Click);
            transform.gameObject.SetActive(false);
            transform.Find("Canvas/content").gameObject.SetActive(false);
            foreach(HelpItem item in gos){
                item.go.SetActive(false);
            }
        });
        for (int i = 0; i < btns.Length;i++){
            int aindex = i;
            btns[aindex].onClick.AddListener(delegate{
                Sound.playSound(SoundType.Click);
                showHelp(aindex,false);
            });
        }
    }

    public void showHelp(){
        transform.gameObject.SetActive(true);
    }


    public void showHelp(int index,bool guide){
		transform.gameObject.SetActive (true);//
        transform.Find("Canvas/content").gameObject.SetActive(true);
        this.gos[index].go.SetActive(true);
        GameObject[] gos = this.gos[index].gos;
        PassGuide current = null;
        PassGuide last = null;
        for (int i = 0; i < gos.Length;i++){
            GameObject go = gos[i];
            PassGuide passGuide = new PassGuide(go,text,index,i,null);
            if(last != null){
                last.next = passGuide;
            }
            if(current == null){
                current = passGuide;
            }
            last = passGuide;
        }
		
		showImage(current);
		current.doAction ();


		Button button = transform.Find ("Canvas/content/mask").GetComponent<Button> ();
		button.onClick.RemoveAllListeners ();
		button.onClick.AddListener(delegate {
			Sound.playSound(SoundType.Click);
			current = current.next;
			if(current != null){
				showImage(current);
				current.doAction ();
			}else {
				if(guide){
                    transform.gameObject.SetActive(false);
                    transform.Find("Canvas/content").gameObject.SetActive(false);
                    this.gos[index].go.SetActive(false);
					// 完成这个新手引导，发送后端
					CSNewGuideFinish guideFinish = new CSNewGuideFinish();
                    guideFinish.Id = index;
					guideFinish.Step = 1;
					SocketManager.SendMessageAsyc((int)MiGongOpcode.CSNewGuideFinish,CSNewGuideFinish.SerializeToBytes(guideFinish),delegate(int opcode, byte[] data) {
						SCNewGuideFinish ret = SCNewGuideFinish.Deserialize(data);
						GameObject mainGo = GameObject.Find ("main");
						MainPanel mainPanel = mainGo.GetComponent<MainPanel>();
                        mainPanel.setGuideState((GuideType)index,1);
					});
                }else{
                    transform.Find("Canvas/content").gameObject.SetActive(false);
                    this.gos[index].go.SetActive(false);
                }
			}
		});
	}

    private void showImage(PassGuide guide){
        foreach(GameObject go in gos[guide.typeIndex].gos){
            go.SetActive(false);
        }
        guide.imageGo.SetActive (true);
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

		public int typeIndex;
        public int index;
		public PassGuide next;

		public GameObject imageGo;
        public PassGuide(){}
		public PassGuide(GameObject imageGo,Text text,int typeIndex,int index,PassGuide next){
			this.text = text;
            this.typeIndex = typeIndex;
            this.index = index;
			this.next = next;
			this.imageGo = imageGo;
		}

		public void doAction(){
            this.text.text = Message.getText("guideContent"+(typeIndex+1)+(index+1));
		}
	}
}
//enum PassGuideType{
//	First,
//	Second,
//	Third,
//	Forth
//}
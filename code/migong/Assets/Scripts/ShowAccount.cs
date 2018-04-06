using System.Collections;
using System.Collections.Generic;
using com.protocol;
using Example;
using UnityEngine;
using UnityEngine.UI;

public class ShowAccount : MonoBehaviour {

    public Button changeNameButton;
    public Text nameText;

    private string oldName;
    private string oldIcon;
    private string id;

    public string iconValue;

    public GameObject iconShow;
    public GameObject[] icons;
    private string[] iconValues = {
        "default","sys_icon_1","sys_icon_2","sys_icon_3","sys_icon_4","sys_icon_5","sys_icon_6"
    };

    private void showIcon(string value,string name){
        iconValue = value;
        iconShow.transform.Find("iconName").GetComponent<Text>().text = name;
        // TODO 修改图片
    }

	// Use this for initialization

	void Start () {

        CSUserInfo userInfo = new CSUserInfo();
        SocketManager.SendMessageAsyc((int)AccountOpcode.CSUserInfo, CSUserInfo.SerializeToBytes(userInfo), delegate(int opcode, byte[] data){
            SCUserInfo ret = SCUserInfo.Deserialize(data);
            oldIcon = ret.Icon;
            oldName = ret.Name;
            this.id = ret.Id;
            //
            showIcon(oldIcon,oldName);
        });

        for (int i = 0; i < icons.Length;i++){
            GameObject go = icons[i];
            // 图片，按钮
            Button bt = go.GetComponent<Button>();
            bt.onClick.AddListener(delegate{
                string iconName = icons[i].transform.Find("iconName").GetComponent<Text>().text;
                showIcon(iconValues[i],iconName);
            });
        }

        changeNameButton.onClick.AddListener(delegate {
            if(oldName.Equals(nameText.text) && oldIcon.Equals(iconValue)){
                WarnDialog.showWarnDialog("qing shu ru mingzi ");
            }else if(nameText.text.Length == 0 && iconValue == null){
                
            }else{
                if(nameText.text.Length > 50){
                    WarnDialog.showWarnDialog("too long");
                    return;
                }
                //WarnDialog.showWarnDialog(text.text);
                CSChangeUserInfo changeUserInfo = new CSChangeUserInfo();
                changeUserInfo.Name = nameText.text;
                changeUserInfo.Icon = iconValue;
                SocketManager.SendMessageAsyc((int)AccountOpcode.CSChangeUserInfo, CSChangeUserInfo.SerializeToBytes(changeUserInfo), delegate (int opcode, byte[] data){

                });
            }
        });
	}
}

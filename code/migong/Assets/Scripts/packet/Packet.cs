using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using com.protocol;
using Example;
using UnityEngine.EventSystems;

public class Packet : MonoBehaviour {

	private Dictionary<int,GameObject> itemGo = new Dictionary<int, GameObject> ();

	public GameObject contentGo;
	public Button useButton;
	public Text text;

	public Button close;

	private int selectItemId = 0;
	void Awake(){
		close.onClick.AddListener(delegate() {
			Sound.playSound(SoundType.Click);
			gameObject.SetActive(false);
		});
		useButton.onClick.AddListener(delegate() {
			Sound.playSound(SoundType.Click);
			if(selectItemId > 0){
				PacketItem pi = itemGo[selectItemId].GetComponent<PacketItem>();
				if(pi.count < 1){
					Debug.LogError("count is "+pi.count+",can not use");
					return;
				}

				ItemTable itemTable = Params.itemTables[selectItemId];
				switch((ItemType)(itemTable.ItemType)){
				case ItemType.Energy:
					CSUseItem useItem = new CSUseItem();
					PBItem item = new PBItem();
					item.Count = 1;
					item.ItemId = itemTable.Id;
					useItem.Item = item;
					SocketManager.SendMessageAsyc((int)MiGongOpcode.CSUseItem,CSUseItem.SerializeToBytes(useItem),delegate(int opcode, byte[] data) {
						SCUseItem ret = SCUseItem.Deserialize(data);
						Params.energy = int.Parse(ret.Ret);
						// 减少数量
//						PacketItem pi = itemGo[item.ItemId].GetComponent<PacketItem>();
						pi.decCount(1);
						if(pi.count <= 0){
							init();
						}
					});
					break;
				}
			}
		});
	}


	private void init () {
		for (int i = 0; i < contentGo.transform.childCount; i++) {
			Destroy (contentGo.transform.GetChild(i).gameObject);		
		}

		itemGo.Clear ();
		selectItemId = 0;

		CSGetItems getItems = new CSGetItems ();
		SocketManager.SendMessageAsyc ((int)MiGongOpcode.CSGetItems, CSGetItems.SerializeToBytes (getItems), delegate(int opcode, byte[] data) {
			Object packetItemObj = Resources.Load ("packetItem");
			SCGetItems ret = SCGetItems.Deserialize(data);

			RectTransform contentTrans = contentGo.GetComponent<RectTransform> ();
			contentTrans.sizeDelta = new Vector2 (0,160 * (ret.Items.Count/4+(ret.Items.Count%4 == 0?0:1)));


			int step = Screen.width/8;

			float scale = Screen.width/640f;

			int index = 0;

			Button btn1 = null;
			int initSelectItemId = 0;
			foreach(PBItem item in ret.Items){
				if(item.Count > 0){
					GameObject go = Instantiate(packetItemObj) as GameObject;

					itemGo[item.ItemId] = go;

					PacketItem pi = go.GetComponent<PacketItem>();
					pi.itemId = item.ItemId;
					pi.count = item.Count;

					int x = index%4;
					int y = index/4;


					go.transform.localPosition = new Vector3 (step * (x*2+1),-160 * y,0);

					go.transform.localScale = new Vector3 (scale,scale,1);
					go.transform.SetParent(contentGo.transform,false);

					Button btn = go.GetComponent<Button>();

					btn.onClick.AddListener(delegate() {
						Sound.playSound(SoundType.Click);
						selectItemId = pi.itemId;
						showInfoByItemType(pi.itemId);
					});

					if(btn1 == null){
						btn1 = btn;
						initSelectItemId = pi.itemId;
					}

					index ++ ;
				}
			}
			//调用会触发Button的按钮变色  
			if(btn1 != null){
				showInfoByItemType(initSelectItemId);
				btn1.Select();
				selectItemId = initSelectItemId;
			}else{
				text.text = "";
				useButton.gameObject.SetActive (false);
			}
		});
	}

	public void showPacket(){
		gameObject.SetActive (true);
		init ();
	}

	public void showInfoByItemType(int itemId){
		
		ItemTable itemTable = Params.itemTables [itemId];
		switch ((ItemType)(itemTable.ItemType)) {
		case ItemType.Energy:
			useButton.gameObject.SetActive (true);
			text.text = Message.getText("itemEnergyIntroduce",itemTable.Para1);
			break;
		case ItemType.AddSpeed:
			useButton.gameObject.SetActive (false);
			text.text = Message.getText("itemAddSpeedIntroduce",itemTable.Para1);
			break;
		case ItemType.AddTime:
			useButton.gameObject.SetActive (false);
			text.text = Message.getText("itemAddTimeIntroduce",itemTable.Para1);
			break;
		case ItemType.MulBean:
			useButton.gameObject.SetActive (false);
			text.text = Message.getText("itemMulBeanIntroduce",itemTable.Para1);
			break;
		case ItemType.ShowRoute:
			useButton.gameObject.SetActive (false);
			text.text = Message.getText("itemShowRouteIntroduce");
			break;
		}
	}
}

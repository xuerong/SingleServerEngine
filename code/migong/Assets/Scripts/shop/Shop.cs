﻿using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using com.protocol;
using Example;
using UnityEngine.EventSystems;

public class Shop : MonoBehaviour {
	private decimal goldMoneyRate = 10;

	private Dictionary<int,GameObject> itemGo = new Dictionary<int, GameObject> ();

	public GameObject contentGo;

	public Button close;


	public Button itemButtion;
	public Button unitButton;
	public Button peckButton;

	public Text goldText;
	public Button addGoldButton;

	public GameObject show;
	public GameObject[] showItem;
	public Image[] showItemImage;
	public Text[] showItemText;

	public Button showButton;

	void Awake(){
		close.onClick.AddListener(delegate() {
			Sound.playSound(SoundType.Click);
			gameObject.SetActive(false);
		});

		itemButtion.onClick.AddListener(delegate() {
			Sound.playSound(SoundType.Click);
			initItem();
		});

		unitButton.onClick.AddListener(delegate() {
			Sound.playSound(SoundType.Click);
			initUnit();
		});

		peckButton.onClick.AddListener(delegate() {
			Sound.playSound(SoundType.Click);
			initPeck();
		});
		addGoldButton.onClick.AddListener(delegate() {
			Sound.playSound(SoundType.Click);
			initPeck();
		});
		// 金币数量
		goldText.text = Params.gold.ToString();
		//
		showButton.onClick.AddListener(delegate() {
			Sound.playSound(SoundType.Click);
			show.SetActive(false);
		});
	}

    private void showTabColor(ShopType type){
        Image image = itemButtion.gameObject.GetComponent<Image>();
        image.color = type == ShopType.Item?(new Color32(200, 200, 200, 255)):(new Color32(255, 255, 255, 255));
        image = unitButton.gameObject.GetComponent<Image>();
        image.color = type == ShopType.Unit ? (new Color32(200, 200, 200, 255)) : (new Color32(255, 255, 255, 255));
        image = peckButton.gameObject.GetComponent<Image>();
        image.color = type == ShopType.Peck ? (new Color32(200, 200, 200, 255)) : (new Color32(255, 255, 255, 255));
    }

	// 道具
	private void initItem () {
        showTabColor(ShopType.Item);
		for (int i = 0; i < contentGo.transform.childCount; i++) {
			Destroy (contentGo.transform.GetChild(i).gameObject);		
		}

		itemGo.Clear ();

		Object shopItemObj = Resources.Load ("shopItem");
		RectTransform contentTrans = contentGo.GetComponent<RectTransform> ();
		contentTrans.sizeDelta = new Vector2 (0,120 * Params.itemTables.Count);

		int index = 0;
		foreach (ItemTable itemTable in Params.itemTables.Values) {
			GameObject go = Instantiate(shopItemObj) as GameObject;

			ShopItem si = go.GetComponent<ShopItem>();
			si.shop = this;
			si.type = ShopType.Item;
			si.id = itemTable.Id;
			si.price = itemTable.Price;
			si.price2 = itemTable.Price;
			si.isDiscount = false;

			go.transform.localPosition = new Vector3 (0,-120 * index++,0);

			go.transform.localScale = new Vector3 (1,1,1);
			go.transform.SetParent(contentGo.transform,false);
		}
	}
	// 套装
	private void initUnit () {
        showTabColor(ShopType.Unit);
		for (int i = 0; i < contentGo.transform.childCount; i++) {
			Destroy (contentGo.transform.GetChild(i).gameObject);		
		}

		itemGo.Clear ();

		Object shopItemObj = Resources.Load ("shopItem2");
		RectTransform contentTrans = contentGo.GetComponent<RectTransform> ();
		contentTrans.sizeDelta = new Vector2 (0,140 * Params.unitTables.Count);

		int index = 0;
		foreach (UnitTable unitTable in Params.unitTables.Values) {
			GameObject go = Instantiate(shopItemObj) as GameObject;

			ShopItem si = go.GetComponent<ShopItem>();
			si.shop = this;
			si.type = ShopType.Unit;
			si.id = unitTable.Id;
			si.items = unitTable.Items;
			si.price = calOriginPrice (unitTable.Items);
			si.price2 = unitTable.Price;
			si.isDiscount = true;

			go.transform.localPosition = new Vector3 (0,-140 * index++,0);

			go.transform.localScale = new Vector3 (1,1,1);
			go.transform.SetParent(contentGo.transform,false);
		}
	}
	// 礼包
	private void initPeck () {
        showTabColor(ShopType.Peck);
		for (int i = 0; i < contentGo.transform.childCount; i++) {
			Destroy (contentGo.transform.GetChild(i).gameObject);		
		}

		itemGo.Clear ();

        Dictionary<int, string> products = new Dictionary<int, string>();
        foreach (PeckTable peckTable in Params.peckTables.Values){
            products.Add(peckTable.Id,"s_"+peckTable.Price);
        }

        Purchaser.ins().InitializePurchasing(delegate(bool success) {
            if(success){
                //
                Purchaser.ins().buyCallBack = buyCallBack;
                //
                Object shopItemObj = Resources.Load("shopItem3");
                RectTransform contentTrans = contentGo.GetComponent<RectTransform>();
                contentTrans.sizeDelta = new Vector2(0, 140 * Params.peckTables.Count);

                // goldMoneyRate
                ProductInfo onePi = Purchaser.ins().productInfos[1];
                PeckTable onePt = Params.peckTables[1];
                goldMoneyRate = onePi.price / onePt.goldNum;

                int index = 0;
                foreach(KeyValuePair<int,ProductInfo> pi in Purchaser.ins().productInfos){
                    PeckTable peckTable = Params.peckTables[pi.Key];
                    GameObject go = Instantiate(shopItemObj) as GameObject;

                    ShopItem si = go.GetComponent<ShopItem>();
                    si.shop = this;
                    si.type = ShopType.Peck;
                    si.id = peckTable.Id;
                    si.gold = peckTable.gold;
                    si.goldNum = peckTable.goldNum;
                    si.items = peckTable.Items;
                    //Mathf.
                    si.price = decimal.Round(calOriginPrice(peckTable.gold, peckTable.Items),2);
                    //si.price = calOriginPrice(peckTable.gold, peckTable.Items);
                    si.price2 = decimal.Round(pi.Value.price,2);
                    //si.price2 = pi.Value.price;//peckTable.Price;
                    si.isDiscount = true;

                    go.transform.localPosition = new Vector3(0, -140 * index++, 0);

                    go.transform.localScale = new Vector3(1, 1, 1);
                    go.transform.SetParent(contentGo.transform, false);
                }

                //int index = 0;
                //foreach (PeckTable peckTable in Params.peckTables.Values)
                //{
                //    GameObject go = Instantiate(shopItemObj) as GameObject;

                //    ShopItem si = go.GetComponent<ShopItem>();
                //    si.shop = this;
                //    si.type = ShopType.Peck;
                //    si.id = peckTable.Id;
                //    si.gold = peckTable.gold;
                //    si.goldNum = peckTable.goldNum;
                //    si.items = peckTable.Items;
                //    si.price = calOriginPrice(peckTable.gold, peckTable.Items);
                //    si.price2 = peckTable.Price;
                //    si.isDiscount = true;

                //    go.transform.localPosition = new Vector3(0, -140 * index++, 0);

                //    go.transform.localScale = new Vector3(1, 1, 1);
                //    go.transform.SetParent(contentGo.transform, false);
                //}
            }
        },products);
	}
    //
    public void buyCallBack(string id, bool success, string token)
    {
        if (success)
        {
            // 请求发货
            CSMoneyBuy moneyBuy = new CSMoneyBuy();
            moneyBuy.Id = int.Parse(id);
            moneyBuy.Num = 1;//int.Parse(showNum.text);
            moneyBuy.Token = token;//System.DateTime.Now.Ticks.ToString();
            SocketManager.SendMessageAsyc((int)MiGongOpcode.CSMoneyBuy, CSMoneyBuy.SerializeToBytes(moneyBuy), delegate (int opcode2, byte[] data2) {
                SCMoneyBuy ret2 = SCMoneyBuy.Deserialize(data2);
                buyFinish(ret2.Success > 0, (int)ShopType.Peck, int.Parse(id), 1, ret2.Gold);
            });
        }
        else
        {
            WarnDialog.showWarnDialog(Message.getText("buyFail"));
        }
    }
	// 买完回调
	public void buyFinish(bool success,int shopType,int id,int num,int gold){
		if (!success) {
			WarnDialog.showWarnDialog (Message.getText("buyFail"));
			return;
		}
		Params.gold = gold;
		goldText.text = Params.gold.ToString ();

//		WarnDialog.showWarnDialog("buy success,type = "+shopType+",itemId = "+id+",num = "+num);

		show.SetActive (true);

		ShopType type = (ShopType)shopType;
		if(type == ShopType.Item){ // 道具用的时候是实时获得的，所以，啥都不做
			showItemByCount(1);
			showIn (id, num,0);
		}else if(type == ShopType.Unit){ // 道具用的时候是实时获得的，所以，啥都不做
			UnitTable unitTable = Params.unitTables[id];
			showItemByCount (unitTable.Items.Count);
			int index = 0;
			foreach(KeyValuePair<int,int> kv in unitTable.Items) {
				showIn (kv.Key,kv.Value*num,index++);
			}
		}else if(type == ShopType.Peck){ // 
			PeckTable peckTable = Params.peckTables[id];
			if (peckTable.gold > 0) {
				Sprite sp = ShopItem.getGoldSprite ();
				showItemImage[0].sprite = sp;
				showItemText [0].text = "x" + (peckTable.gold*num);
			}

			int index = peckTable.gold > 0?1:0;
			foreach(KeyValuePair<int,int> kv in peckTable.Items) {
				showIn (kv.Key,kv.Value*num,index++);
			}
			showItemByCount (index);
		}
	}

	public void showIn(int id,int num,int index){
		Sprite sp = ShopItem.getSprite(id);
		showItemImage[index].sprite = sp;
		showItemText [index].text = "x" + num;
	}

	public void showItemByCount(int count){
		for(int i = 0;i<showItem.Length;i++){
			if (i < count) {
				showItem [i].SetActive (true);
			} else {
				showItem [i].SetActive (false);
			}
		}
	}

	// 计算peck礼包的原价
    private decimal calOriginPrice(int gold , Dictionary<int,int> items){
		int allGold = gold + calOriginPrice (items);
		decimal ret = allGold * goldMoneyRate;
		return ret;
	}
	// 计算unit套装的原价
	private int calOriginPrice(Dictionary<int,int> items){
		if (items == null || items.Count == 0) {
			return 0;
		}
		int ret = 0;
		foreach (KeyValuePair<int, int> kv in items) {
			ret += Params.itemTables [kv.Key].Price * kv.Value;
		}
		return ret;
	}

	public void showShop(){
		gameObject.SetActive (true);
		initItem ();
	}
}

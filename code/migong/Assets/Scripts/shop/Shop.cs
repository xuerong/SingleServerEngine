using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using com.protocol;
using Example;
using UnityEngine.EventSystems;

public class Shop : MonoBehaviour {
	private const int goldMoneyRate = 10;

	private Dictionary<int,GameObject> itemGo = new Dictionary<int, GameObject> ();

	public GameObject contentGo;

	public Button close;


	public Button itemButtion;
	public Button unitButton;
	public Button peckButton;

	public Text goldText;
	public Button addGoldButton;

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
	}

	// 道具
	private void initItem () {
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
		for (int i = 0; i < contentGo.transform.childCount; i++) {
			Destroy (contentGo.transform.GetChild(i).gameObject);		
		}

		itemGo.Clear ();

		Object shopItemObj = Resources.Load ("shopItem2");
		RectTransform contentTrans = contentGo.GetComponent<RectTransform> ();
		contentTrans.sizeDelta = new Vector2 (0,140 * Params.unitTables.Count);

		int index = 0;
		foreach (PeckTable peckTable in Params.peckTables.Values) {
			GameObject go = Instantiate(shopItemObj) as GameObject;

			ShopItem si = go.GetComponent<ShopItem>();
			si.shop = this;
			si.type = ShopType.Peck;
			si.id = peckTable.Id;
			si.gold = peckTable.gold;
			si.goldNum = peckTable.goldNum;
			si.items = peckTable.Items;
			si.price = calOriginPrice (peckTable.gold,peckTable.Items);
			si.price2 = peckTable.Price;
			si.isDiscount = true;

			go.transform.localPosition = new Vector3 (0,-140 * index++,0);

			go.transform.localScale = new Vector3 (1,1,1);
			go.transform.SetParent(contentGo.transform,false);
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

		WarnDialog.showWarnDialog("buy success,type = "+shopType+",itemId = "+id+",num = "+num);

		ShopType type = (ShopType)shopType;
		if(type == ShopType.Item){ // 道具用的时候是实时获得的，所以，啥都不做
			
		}else if(type == ShopType.Unit){ // 道具用的时候是实时获得的，所以，啥都不做

		}else if(type == ShopType.Peck){ // 
			
		}
	}
	// 计算peck礼包的原价
	private int calOriginPrice(int gold , Dictionary<int,int> items){
		int allGold = gold + calOriginPrice (items);
		int ret = allGold / goldMoneyRate;
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

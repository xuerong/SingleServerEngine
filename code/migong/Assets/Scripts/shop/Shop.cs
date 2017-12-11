using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using com.protocol;
using Example;
using UnityEngine.EventSystems;

public class Shop : MonoBehaviour {

	private Dictionary<int,GameObject> itemGo = new Dictionary<int, GameObject> ();

	public GameObject contentGo;

	public Button close;


	public Button itemButtion;
	public Button unitButton;
	public Button peckButton;

	void Awake(){
		close.onClick.AddListener(delegate() {
			Sound.playSound(SoundType.Click);
			gameObject.SetActive(false);
		});
	}


	private void init () {
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

	public void showShop(){
		gameObject.SetActive (true);
		init ();
	}
}

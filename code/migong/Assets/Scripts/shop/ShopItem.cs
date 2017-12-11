using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class ShopItem: MonoBehaviour {
	public ShopType type; // 0道具，1套装，3礼包：决定钱的类型
	public int id; // id决定图片
	public int price; // 原价
	public int price2; // 现价
	public bool isDiscount;

	public Image image;

	public Image typeImage;

	public Text priceText;
	public Text price2Text;

	public Button addNum;
	public Button decNum;
	public Text showNum;

	public Button buy;


	// Use this for initialization
	void Start () {
		// 图片
		Sprite sp = Resources.Load (getImage(), typeof(Sprite)) as Sprite;
		image.sprite = sp;
		//
		sp = Resources.Load (getTypeImage(), typeof(Sprite)) as Sprite;
		typeImage.sprite = sp;
		//
		showPrice(1);
		//
		addNum.onClick.AddListener(delegate() {
			int num = int.Parse(showNum.text)+1;
			showNum.text = num.ToString();
			// 价格
			showPrice(num);
		});
		decNum.onClick.AddListener(delegate() {
			int num = int.Parse(showNum.text);
			if(num <= 1){
				return;
			}
			num--;
			showNum.text = num.ToString();
			// 价格
			showPrice(num);
		});
		//
		buy.onClick.AddListener(delegate() {
			Debug.Log("do buy");
		});
	}
	//
	private void showPrice(int num){
		priceText.text = (price*num).ToString();
		if (isDiscount) {
			price2Text.text = (price2 * num).ToString ();
		} else {
			// 调整位置

		}
	}
	// 根据type和id获取对应产品的图片
	private string getImage(){
		return "itemImage/item1";
	}
	// 根据type获取对应钱的图片
	private string getTypeImage(){
		return "itemImage/item1";
	}
}

public enum ShopType{
	Item,
	Unit,
	Peck
}

using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using Example;
using com.protocol;

public class ShopItem: MonoBehaviour {
	public Shop shop;
    public Purchaser purchaser;

	public ShopType type; // 0道具，1套装，3礼包：决定钱的类型
	public int id; // id决定图片
	public int gold;
	public int goldNum;
	public Dictionary<int,int> items;
	public int price; // 原价
	public int price2; // 现价
	public bool isDiscount;

	public Image image;

	public Image typeImage;
    public Image typeImage2;

	public Text priceText;
	public Text price2Text;

	public Button addNum;
	public Button decNum;
	public Text showNum;

	public Button buy;


	// Use this for initialization
	void Start () {
        //
        this.purchaser = shop.gameObject.GetComponent<Purchaser>();
		// 图片
		if (type == ShopType.Item) {
			image.sprite = getSprite(id);
		} else {
			Object shopItemImageObj = Resources.Load ("shopItemImage");
			int index = 0;
			if (type == ShopType.Unit) {
				
			} else if (type == ShopType.Peck) {
				addShopItemItem (Instantiate (shopItemImageObj) as GameObject,getGoldSprite(),goldNum,10 + 80 * index++);
			}
			if (items != null && items.Count > 0) {
				foreach (KeyValuePair<int,int> kv in items) {
					addShopItemItem (Instantiate (shopItemImageObj) as GameObject,getSprite (kv.Key),kv.Value,10 + 80 * index++);
				}
			}
		}
		//
		Sprite spType = Resources.Load (getTypeImage(), typeof(Sprite)) as Sprite;
		typeImage.sprite = spType;
        if(typeImage2 != null){
            typeImage2.sprite = spType;
        }

		//
		showPrice(1);
		//
		addNum.onClick.AddListener(delegate() {
			Sound.playSound(SoundType.Click);
			int num = int.Parse(showNum.text)+1;
			showNum.text = num.ToString();
			// 价格
			showPrice(num);
		});
		decNum.onClick.AddListener(delegate() {
			Sound.playSound(SoundType.Click);
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
			Sound.playSound(SoundType.Click);
			// 前段进行一系列购买操作，付钱成功通知服务器加道具，然后服务器推送前端道具加成功或者数量，前端加道具
			// 买完回调

			if(type == ShopType.Item || type == ShopType.Unit){
//				CSGold
				CSGoldBuy goldBuy = new CSGoldBuy();
				goldBuy.Type = (int)type;
				goldBuy.Id = id;
				goldBuy.Num = int.Parse(showNum.text);
				SocketManager.SendMessageAsyc((int)MiGongOpcode.CSGoldBuy,CSGoldBuy.SerializeToBytes(goldBuy),delegate(int opcode, byte[] data) {
					SCGoldBuy ret = SCGoldBuy.Deserialize(data);
					shop.buyFinish(ret.Success>0,(int)type,id,int.Parse(showNum.text),ret.Gold);
				});
			}else if(type == ShopType.Peck){
				CSMoneyBuyBefore moneyBuyBefore = new CSMoneyBuyBefore();
				moneyBuyBefore.Id = id;
				moneyBuyBefore.Num = int.Parse(showNum.text);
				SocketManager.SendMessageAsyc((int)MiGongOpcode.CSMoneyBuyBefore,CSMoneyBuyBefore.SerializeToBytes(moneyBuyBefore),delegate(int opcode, byte[] data) {
					SCMoneyBuyBefore ret = SCMoneyBuyBefore.Deserialize(data);
					if(ret.IsOk>0){
                        // 支付
                        purchaser.BuyProductID("s_2");
                        //purchaser.get
                        // 请求发货
						CSMoneyBuy moneyBuy = new CSMoneyBuy();
						moneyBuy.Id = id;
						moneyBuy.Num = int.Parse(showNum.text);
						moneyBuy.Token = System.DateTime.Now.Ticks.ToString();
						SocketManager.SendMessageAsyc((int)MiGongOpcode.CSMoneyBuy,CSMoneyBuy.SerializeToBytes(moneyBuy),delegate(int opcode2, byte[] data2) {
							SCMoneyBuy ret2 = SCMoneyBuy.Deserialize(data2);
							shop.buyFinish(ret2.Success>0,(int)type,id,int.Parse(showNum.text),ret2.Gold);
						});
					}else{
						Debug.Log(ret.Reason);
						shop.buyFinish(false,(int)type,id,int.Parse(showNum.text),Params.gold);
					}
				});
			}
		});
	}

	// add shopitemimage
	private void addShopItemItem(GameObject go,Sprite sp,int num,int x){
		Image image = go.transform.Find ("image").GetComponent<Image> ();
//		Sprite sp = Resources.Load (imagepath, typeof(Sprite)) as Sprite;
		image.sprite = sp;

		Text text = go.transform.Find ("num").GetComponent<Text> ();
		text.text = "x" + num;


		go.transform.localPosition = new Vector3 (x, 0, 0);

		go.transform.localScale = new Vector3 (1, 1, 1);
		go.transform.SetParent (transform, false);
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

	public static Sprite getGoldSprite(){
		return SpriteCache.getSprite ("itemImage/gold");//Resources.Load ("itemImage/gold", typeof(Sprite)) as Sprite;
	}
	// 根据type和id获取对应产品的图片
	public static Sprite getSprite(int itemId){
        string path = getSpritePath(itemId);
		return SpriteCache.getSprite (path); //Resources.Load (path, typeof(Sprite)) as Sprite;
	}

	public static Sprite getEnergySprite(){
		return SpriteCache.getSprite ("itemImage/energy"); //Resources.Load ("itemImage/energy", typeof(Sprite)) as Sprite;
	}
	// 根据type获取对应钱的图片
	private string getTypeImage(){
		if (type == ShopType.Item || type == ShopType.Unit) {
			return "itemImage/gold";
		} else {
			return "itemImage/money";
		}
	}

    public static string getSpritePath(int itemId){
        string ret = "itemImage/item1";
        switch(itemId){
            case 1:ret = "itemImage/energy1";break;
            case 2: ret = "itemImage/energy5"; break;
            case 3: ret = "itemImage/energy30"; break;
            case 4: ret = "itemImage/s1"; break;
            case 5: ret = "itemImage/s4"; break;
            case 6: ret = "itemImage/s3"; break;
            case 7: ret = "itemImage/s2"; break;
        }
        return ret;
    }
}



public enum ShopType{
	Item,
	Unit,
	Peck
}

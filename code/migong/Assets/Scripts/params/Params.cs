using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using Example;

public class Params {

	public static int energy = 0;

	public static int gold = 0;

	public static Dictionary<int,ItemTable> itemTables = new Dictionary<int, ItemTable> ();
	public static Dictionary<int,UnitTable> unitTables = new Dictionary<int, UnitTable> ();
	public static Dictionary<int,PeckTable> peckTables = new Dictionary<int, PeckTable> ();

	public static void init(SCBaseInfo baseInfo){
		foreach (PBItemTable it in baseInfo.ItemTable) {
			ItemTable itemTable = new ItemTable ();
			itemTable.Id = it.Id;
			itemTable.ItemType = it.ItemType;
			itemTable.Para1 = it.Para1;
			itemTable.Para2 = it.Para2;
			itemTable.Price = it.Price;
			itemTables.Add (itemTable.Id, itemTable);
		}

		for (int i = 0; i < 10; i++) {
			UnitTable unitTable = new UnitTable ();
			unitTable.Id = i;
			unitTable.Items = new Dictionary<int, int> () {  
				{ 1,2+i }, { 5,420 }, { 3,5 }
			}; 
			unitTable.Price = i * 2;

			unitTables.Add (i,unitTable);

			PeckTable peckTable = new PeckTable ();
			peckTable.Id = i;
			peckTable.gold = i + 1;
			peckTable.goldNum = i + 2;
			peckTable.Items = new Dictionary<int, int> () {  
				{ 1,2+i }, { 5,42 }, { 3,5 }
			}; 
			peckTable.Price = i * 2;

			peckTables.Add (i,peckTable);
		}

	}
	// 根据类型获取id，这里仅仅是建立在：四种技能中每种技能道具只有一种的基础上
	public static int getItemId(ItemType itemType){
		switch (itemType) {
		case ItemType.AddSpeed:
			return 4;
		case ItemType.AddTime:
			return 5;
		case ItemType.MulBean:
			return 6;
		case ItemType.ShowRoute:
			return 7;
		}
		return 4;
	}
}
public class ItemTable{
	public int Id;
	public int ItemType;
	public int Para1;
	public int Para2;
	public int Price;
}

public enum ItemType{
	Energy,
	AddSpeed,
	AddTime,
	MulBean,
	ShowRoute
}
// 商店套装
public class UnitTable{
	public int Id;
	public Dictionary<int,int> Items;
	public int Price; // 价格，这里只需要这一个价格，这是实际价格，原价是按照东西的价格叠加就可以，同时可以计算出来折扣
}
// 商店礼包
public class PeckTable{
	public int Id;
	public int gold;
	public int goldNum;
	public Dictionary<int,int> Items;
	public int Price; // 价格，这里只需要这一个价格，这是实际价格，原价是按照东西的价格叠加就可以，同时可以计算出来折扣
}

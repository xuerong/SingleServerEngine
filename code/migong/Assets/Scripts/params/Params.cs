using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using Example;

public class Params {
	public static Dictionary<int,ItemTable> itemTables = new Dictionary<int, ItemTable> ();

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

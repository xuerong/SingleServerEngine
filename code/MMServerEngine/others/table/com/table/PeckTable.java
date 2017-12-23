package com.table;
//Auto Generate File, Do NOT Modify!!!!!!!!!!!!!!!
public final class PeckTable{
	public static final PeckTable[] datas={
		new PeckTable((int)1.0,"first",(int)180.0,"0;2|5;3",(int)6.0,(int)1.0),
		new PeckTable((int)2.0,"second",(int)60.0,"0;2|5;3",(int)6.0,(int)0.0),
		new PeckTable((int)3.0,"third",(int)240.0,"1;2|2;4|3:3",(int)24.0,(int)0.0),
		new PeckTable((int)4.0,"forth",(int)1024.0,"6;5",(int)96.0,(int)0.0),
		new PeckTable((int)5.0,"wu",(int)2048.0,"4;2|5;6",(int)166.0,(int)0.0),
		new PeckTable((int)6.0,"liu",(int)8888.0,"6;2|7;7",(int)666.0,(int)0.0)
	};
	private int id;
	private String name;
	private int gold;
	private String items;
	private int price;
	private int limit;

	public PeckTable(int id,String name,int gold,String items,int price,int limit){
		this.id=id;
		this.name=name;
		this.gold=gold;
		this.items=items;
		this.price=price;
		this.limit=limit;
	}
	public int getId(){return id;}
	public void setId(int id){this.id=id;}
	public String getName(){return name;}
	public void setName(String name){this.name=name;}
	public int getGold(){return gold;}
	public void setGold(int gold){this.gold=gold;}
	public String getItems(){return items;}
	public void setItems(String items){this.items=items;}
	public int getPrice(){return price;}
	public void setPrice(int price){this.price=price;}
	public int getLimit(){return limit;}
	public void setLimit(int limit){this.limit=limit;}
}
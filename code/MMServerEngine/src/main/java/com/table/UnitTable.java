package com.table;
//Auto Generate File, Do NOT Modify!!!!!!!!!!!!!!!
public final class UnitTable{
	public static UnitTable[] datas={
		new UnitTable((int)1.0,"first","0;2|5;3",(int)15.0,(int)0.0),
		new UnitTable((int)2.0,"first","1;2|2;4|3;3",(int)36.0,(int)0.0),
		new UnitTable((int)3.0,"first","6;5",(int)47.0,(int)3.0),
		new UnitTable((int)4.0,"first","4;2|5;6",(int)58.0,(int)0.0),
		new UnitTable((int)5.0,"first","6;2|7;7",(int)69.0,(int)1.0)
	};
	private int id;
	private String name;
	private String items;
	private int price;
	private int limit;

	public UnitTable(int id,String name,String items,int price,int limit){
		this.id=id;
		this.name=name;
		this.items=items;
		this.price=price;
		this.limit=limit;
	}
	public int getId(){return id;}
	public void setId(int id){this.id=id;}
	public String getName(){return name;}
	public void setName(String name){this.name=name;}
	public String getItems(){return items;}
	public void setItems(String items){this.items=items;}
	public int getPrice(){return price;}
	public void setPrice(int price){this.price=price;}
	public int getLimit(){return limit;}
	public void setLimit(int limit){this.limit=limit;}
}
package com.table;
//Auto Generate File, Do NOT Modify!!!!!!!!!!!!!!!
public final class ItemTable{
	public static ItemTable[] datas={
		new ItemTable((int)1.0,(int)0.0,(int)1.0,(int)0.0,(int)1.0,(int)1.0),
		new ItemTable((int)2.0,(int)0.0,(int)5.0,(int)0.0,(int)3.0,(int)1.0),
		new ItemTable((int)3.0,(int)0.0,(int)30.0,(int)0.0,(int)15.0,(int)1.0),
		new ItemTable((int)4.0,(int)1.0,(int)5.0,(int)0.0,(int)2.0,(int)1.0),
		new ItemTable((int)5.0,(int)2.0,(int)30.0,(int)0.0,(int)5.0,(int)1.0),
		new ItemTable((int)6.0,(int)3.0,(int)1.0,(int)0.0,(int)10.0,(int)1.0),
		new ItemTable((int)7.0,(int)4.0,(int)0.0,(int)0.0,(int)15.0,(int)1.0)
	};
	private int id;
	private int itemType;
	private int para1;
	private int para2;
	private int price;
	private int valid;

	public ItemTable(int id,int itemType,int para1,int para2,int price,int valid){
		this.id=id;
		this.itemType=itemType;
		this.para1=para1;
		this.para2=para2;
		this.price=price;
		this.valid=valid;
	}
	public int getId(){return id;}
	public void setId(int id){this.id=id;}
	public int getItemtype(){return itemType;}
	public void setItemtype(int itemType){this.itemType=itemType;}
	public int getPara1(){return para1;}
	public void setPara1(int para1){this.para1=para1;}
	public int getPara2(){return para2;}
	public void setPara2(int para2){this.para2=para2;}
	public int getPrice(){return price;}
	public void setPrice(int price){this.price=price;}
	public int getValid(){return valid;}
	public void setValid(int valid){this.valid=valid;}
}
package com.table;
//Auto Generate File, Do NOT Modify!!!!!!!!!!!!!!!
public final class UnlimitedRewardTable{
	public static UnlimitedRewardTable[] datas={
		new UnlimitedRewardTable((int)1.0,(int)10.0,(int)0.0,"1;2|3;3|4;1"),
		new UnlimitedRewardTable((int)2.0,(int)20.0,(int)2.0,"1;2|3;3|4;2"),
		new UnlimitedRewardTable((int)3.0,(int)30.0,(int)5.0,"1;2|3;3|4;3"),
		new UnlimitedRewardTable((int)4.0,(int)40.0,(int)8.0,"1;2|3;3|4;4"),
		new UnlimitedRewardTable((int)5.0,(int)50.0,(int)15.0,"1;2|3;3|4;5")
	};
	private int id;
	private int star;
	private int gold;
	private String reward;

	public UnlimitedRewardTable(int id,int star,int gold,String reward){
		this.id=id;
		this.star=star;
		this.gold=gold;
		this.reward=reward;
	}
	public int getId(){return id;}
	public void setId(int id){this.id=id;}
	public int getStar(){return star;}
	public void setStar(int star){this.star=star;}
	public int getGold(){return gold;}
	public void setGold(int gold){this.gold=gold;}
	public String getReward(){return reward;}
	public void setReward(String reward){this.reward=reward;}
}
package com.table;
//Auto Generate File, Do NOT Modify!!!!!!!!!!!!!!!
public final class MiGongLevel{
	public static final MiGongLevel[] datas={
		new MiGongLevel((int)1.0,(int)3.0,(int)1.0),
		new MiGongLevel((int)2.0,(int)3.0,(int)2.0),
		new MiGongLevel((int)3.0,(int)3.0,(int)3.0),
		new MiGongLevel((int)4.0,(int)3.0,(int)4.0)
	};
	private int level;
	private int pass;
	private int difficulty;

	public MiGongLevel(int level,int pass,int difficulty){
		this.level=level;
		this.pass=pass;
		this.difficulty=difficulty;
	}
	public int getLevel(){return level;}
	public void setLevel(int level){this.level=level;}
	public int getPass(){return pass;}
	public void setPass(int pass){this.pass=pass;}
	public int getDifficulty(){return difficulty;}
	public void setDifficulty(int difficulty){this.difficulty=difficulty;}
}
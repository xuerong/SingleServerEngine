using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class EnergyUpdable : MonoBehaviour {
	public Text timeText;
	public Text energyText;

	private int currShowEnergy;
	private float shengyu;

	// 
	void OnEnable () {
		//Debug.Log("start ----------"+currShowEnergy+","+Params.energy+","+Params.energyUpdateTime);
		if (currShowEnergy == Params.energy && Params.energyUpdateTime != 0) {
			showTimeText();
		}
	}
	private void showTimeText() { 
		//timeText
		long now = Util.ConvertDateTimeToInt(DateTime.Now);
		float guole = (int)((now - Params.energyUpdateTime) / 1000f - Params.disFromServerTime);
		shengyu = Params.energyRecoverTime - guole;
		Debug.Log("iiiii:"+shengyu);
		if (shengyu <= 0)
		{
			shengyu = 0;
			timeText.text = "0:0";
		}
		else
		{
			timeText.gameObject.SetActive(true);
			int time = (int)shengyu;

			timeText.text = (time / 60) + ":" + (time % 60);
		}
	}
	// Update is called once per frame
	void Update () {
		if (Params.energyUpdateTime == 0 && currShowEnergy == Params.energy) {
			return;
		}
		if (currShowEnergy != Params.energy)
		{
			currShowEnergy = Params.energy;
			energyText.text = Params.energy.ToString();
			if (Params.energyUpdateTime == 0)
			{
				shengyu = 0;
				timeText.gameObject.SetActive(false);
			}
			else
			{
				showTimeText();
			}
		}
		else {
			//Debug.Log("shengyu2:"+shengyu);
			if (shengyu != 0f) {
				shengyu -= Time.deltaTime;
				if (shengyu <= 0)
				{
					shengyu = 0;
				}
				int time = (int)shengyu;
				timeText.text = (time / 60) + ":" + (time % 60);
			}
		}
	}
}

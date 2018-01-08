using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Util : MonoBehaviour {

	public static void clearChildren(GameObject go){
		clearChildren (go.transform);
	}

	public static void clearChildren(Transform transform){
		for (int i = 0; i < transform.childCount; i++) {
			Destroy (transform.GetChild(i).gameObject);		
		}
	}
	/// <summary>  
	/// 将c# DateTime时间格式转换为Unix时间戳格式  
	/// </summary>  
	/// <param name="time">时间</param>  
	/// <returns>long</returns>  
	public static long ConvertDateTimeToInt(DateTime time)
	{
		System.DateTime startTime = TimeZone.CurrentTimeZone.ToLocalTime(new System.DateTime(1970, 1, 1, 0, 0, 0, 0));
		long t = (time.Ticks - startTime.Ticks) / 10000;   //除10000调整为13位      
		return t; 
	}

}

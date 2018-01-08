using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System;

class JobAction{
	public string key;
	public Delegate d;
	public float time;
	public object[] args;
	public bool cancel;
}
public delegate void VoidAction();

public class Job : MonoBehaviour {
	static int MaxPerUpdate = 10;
	static int keyIndex = 0;
	static Dictionary<string,JobAction> dic = new Dictionary<string,JobAction>();
	static Dictionary<string, JobAction> dic2 = new Dictionary<string, JobAction>();	
	
	// Update is called once per frame
	void Update () {
		if (dic.Count > 0) { 
			lock (dic)
			{
				int count = 0;
				List<string> delKeys = new List<string>();
				foreach (string key in dic.Keys) {
					JobAction jobAction = dic[key];
					StartCoroutine(doAction (jobAction));

					delKeys.Add(key);
					if (count++ >= MaxPerUpdate) {
						break;
					}
				}

				foreach (string delKey in delKeys) { 
					dic2.Add(delKey,dic[delKey]);
					dic.Remove(delKey);
				}

			}
		}
	}

	static string defaultKey() {
		return "defaultKey_" + (keyIndex++);
	}

	public static void startJob(Delegate d,float time,object[] args){
		JobAction jobAction = new JobAction ();
		jobAction.d = d;
		jobAction.time = time;
		jobAction.args = args;
		jobAction.cancel = false;
		lock (dic) {
			dic.Add(defaultKey(),jobAction);
		}
	}


	public static void startJob(string key, VoidAction d, float time) 
	{
		lock (dic) {
			if (dic.ContainsKey(key)) {
				dic.Remove(key);
			}else if (dic2.ContainsKey(key)) {
				dic2[key].cancel = true;
				dic2.Remove(key);
			}
			JobAction jobAction = new JobAction();
			jobAction.key = key;
			jobAction.d = d;
			jobAction.time = time;
			jobAction.cancel = false;

			dic.Add (key,jobAction);
		}
	}
	public static void cancelJob(string key){
		lock(dic)
		{
			if (dic.ContainsKey(key))
			{
				dic[key].cancel = true;
			}
			else if (dic2.ContainsKey(key))
			{
				dic2[key].cancel = true;
			}
		}
	}

	public static void startJob(Delegate d,float time){
		startJob (d, time, new object[0]);
	}
	public static void startJob(Delegate d,float time,object arg1){
		startJob (d, time, new object[]{arg1});
	}
	public static void startJob(Delegate d,float time,object arg1,object arg2){
		startJob (d, time, new object[]{arg1,arg2});
	}
	public static void startJob(Delegate d,float time,object arg1,object arg2,object arg3){
		startJob (d, time, new object[]{arg1,arg2,arg3});
	}
	public static void startJob(Delegate d,float time,object arg1,object arg2,object arg3,object arg4){
		startJob (d, time, new object[]{arg1,arg2,arg3,arg4});
	}

	IEnumerator doAction(JobAction jobAction){
		yield return new WaitForSeconds (jobAction.time);
		if (!jobAction.cancel)
		{
			jobAction.d.DynamicInvoke(jobAction.args);
		}
		lock(dic){
			dic2.Remove(jobAction.key);
		}
	}

}

using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System;

class JobAction{
	public Delegate d;
	public float time;
	public object[] args;
}

public class Job : MonoBehaviour {
	static int MaxPerUpdate = 10;
	static Queue<JobAction> queue = new Queue<JobAction>();
	
	// Update is called once per frame
	void Update () {
		if (queue.Count > 0) {
			lock (queue) {
				int count = 0;
				while (queue.Count > 0 && count++ < MaxPerUpdate) {
					StartCoroutine (doAction (queue.Dequeue()));
				}
			}
		}
	}

	public static void startJob(Delegate d,float time,object[] args){
		JobAction jobAction = new JobAction ();
		jobAction.d = d;
		jobAction.time = time;
		jobAction.args = args;
		lock (queue) {
			queue.Enqueue (jobAction);
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
		jobAction.d.DynamicInvoke (jobAction.args);
	}

}

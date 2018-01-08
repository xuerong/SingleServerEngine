using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class TextCollision : MonoBehaviour {

	// Use this for initialization
	void Start () {
		
	}
	
	// Update is called once per frame
	void Update () {
		
	}
	void OnTriggerEnter2D(Collider2D collider)
	{
		//进入触发器执行的代码
		Debug.Log("OnTriggerEnter2D:" + collider);
	}
	void OnCollisionEnter2D(Collision2D collision)
	{
		//进入碰撞器执行的代码
		Debug.Log("OnCollisionEnter2D:" + collision);
	}
}

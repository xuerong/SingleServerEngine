using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Enemy: MonoBehaviour {
	public MapCreate mapCreate;
	public int[][] map;

	public bool running = true;
	Vector3 targetPos; // 目标点

	int lastTargetPosRow; // 
	int lastTargetPosCol; // 
	float speed = 0.1f;
	// Use this for initialization
	void Start () {
		lastTargetPos = targetPos = gameObject.transform.localPosition;
	}
	
	// Update is called once per frame
	void Update () {
		if (!running) {
			return;
		}
		if (gameObject.transform.localPosition == targetPos) {
			newTargetPos();
		}
		float step = speed * Time.deltaTime;
		Vector3.MoveTowards(gameObject.transform.localPosition, targetPos, step);

	}

	private void newTargetPos() {
		Vector2 pos = mapCreate.getPointByPosition(gameObject.transform.localPosition.x,gameObject.transform.localPosition.y);
		int row = (int)pos.x;
		int col = (int)pos.y;
		//
		int[] dirs = new int[4];
		int count = 0;
		if (row > 1 &&( map[row - 1][col] & 2 == 0 )&& (row - 1 != lastTargetPosRow || col != lastTargetPosCol)) {
			dirs[count++] = 0;
		}
		if (col > 1 && map[row][col - 1] & 1 == 0 && (row != lastTargetPosRow || col - 1 != lastTargetPosCol)) {
			dirs[count++] = 1;
		}
		if (map[row][col] & 2 == 0 && (row +1 != lastTargetPosRow || col != lastTargetPosCol)) {
			dirs[count++] = 2;
		}
		if ( map[row][col] & 1 == 0 && (row != lastTargetPosRow || col +1 != lastTargetPosCol)) {
			dirs[count++] = 3;
		}
		//if(count > 0
	}
}

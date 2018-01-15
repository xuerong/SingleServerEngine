using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Enemy: MonoBehaviour {
	public MapCreate mapCreate;
	public int[][] map;

	Vector3 targetPos; // 目标点

	int lastTargetPosRow; // 
	int lastTargetPosCol; // 
	public float speed = 2.6f;
	// Use this for initialization
	void Start () {
		targetPos = gameObject.transform.localPosition;
	}
	
	// Update is called once per frame
	void Update () {
        if (mapCreate.gameOver) {
			return;
		}
		if (gameObject.transform.localPosition == targetPos) {
			newTargetPos();
		}
		float step = speed * Time.deltaTime;
		Vector3 pos = Vector3.MoveTowards(gameObject.transform.localPosition, targetPos, step);
		gameObject.transform.localPosition = pos;
	}

	private void newTargetPos() {
		Vector2 pos = mapCreate.getPointByPosition(gameObject.transform.localPosition.x,gameObject.transform.localPosition.y);
		int row = (int)pos.x;
		int col = (int)pos.y;
        //Debug.Log("row:"+row+",col:"+col);
        //Debug.Log("x:" + gameObject.transform.localPosition.x + ",y:" + gameObject.transform.localPosition.y);
		//
		int[] dirs = new int[4];
		int count = 0;
		if (row > 1 && ( (map[row - 1][col] & 2) == 0 )&& (row - 1 != lastTargetPosRow || col != lastTargetPosCol)) {
			dirs[count++] = 0;
            //Debug.Log("--"+0);
		}
		if (col > 1 && (map[row][col - 1] & 1) == 0 && (row != lastTargetPosRow || col - 1 != lastTargetPosCol)) {
			dirs[count++] = 1;
            //Debug.Log("--" + 1);
		}
		if ((map[row][col] & 2) == 0 && (row +1 != lastTargetPosRow || col != lastTargetPosCol)) {
			dirs[count++] = 2;
            //Debug.Log("--" + 2);
		}
		if ( (map[row][col] & 1) == 0 && (row != lastTargetPosRow || col +1 != lastTargetPosCol)) {
			dirs[count++] = 3;
            //Debug.Log("--" + 3);
		}
		if(count == 0){
			// 原路返回
            //Debug.Log("lastTargetPosRow:" + lastTargetPosRow + ",lastTargetPosCol:" + lastTargetPosCol);
            Vector2 newPos = mapCreate.getPositionByPoint(lastTargetPosRow, lastTargetPosCol);
			//gameObject.transform.localPosition = new Vector3(newPos.x,newPos.y,0);
			targetPos = new Vector3(newPos.x, newPos.y, 0);
		}else{
			int index = (int)Random.Range(0f, (float)count);
			int dir = dirs[index];
			int targetRow = row, targetCol = col;
			if (dir == 0)
			{
				targetRow = row - 1;
			}
			else if (dir == 1)
			{
				targetCol = col - 1;
			}
			else if (dir == 2)
			{
				targetRow = row + 1;
			}
			else {
				targetCol = col + 1;
			}
            //Debug.Log("targetRow:" + targetRow + ",targetCol:" + targetCol);
			Vector2 newPos = mapCreate.getPositionByPoint(targetRow, targetCol);
            //Debug.Log("x:" + newPos.x + ",y:" + newPos.y);
			targetPos = new Vector3(newPos.x, newPos.y, 0);
		}
		lastTargetPosRow = row;
		lastTargetPosCol = col;
	}
}

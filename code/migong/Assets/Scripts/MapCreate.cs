using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class MapCreate : MonoBehaviour {
	public int[][] map = new int[][]{ 
		new int[]{0,2	,2,	2,		2},
		new int[]{1,1	,0,	2,		1},
		new int[]{1,1,	2,	2	,	1},
		new int[]{1,0,	1,	0,		1},
		new int[]{1,3,	2,	3,		3}
	};

	int x = 0,y = 0;
	static float myScale = 0.5f;
	static float nodeX = 1.9f * myScale,nodeY = 1.9f * myScale;
	// Use this for initialization
	void Start () {
//		transform.localScale = new Vector3 (0.1f,0.1f,1); // 这样为啥不行
		int tr=map.Length;											//行数和列数
		int td=map[0].Length;
		for (int i = 0; i < tr; i++) {										//绘制墙
			for (int j = 0; j < td; j++) {
//				if ((map[i][j] & 8) == 8) {								//注意这里的为运算的用法
//					int w = tr - i-1;
////					g.drawLine (x + j * nodeX, y + i * nodeY, x + (j + 1) * nodeX, y + i * nodeY);
//					GameObject up = Instantiate(Resources.Load("up")) as GameObject;
//					up.transform.parent = transform;
//					up.transform.position = new Vector3 (x + j * nodeX, y + w * nodeY,0);
//					up.transform.localScale = new Vector3 (0.1f,0.1f,1);
//				}
//				if ((map[i][j] & 4) == 4) {
////					g.drawLine (x + j * nodeX, y + i * nodeY, x + j * nodeX, y + (i + 1) * nodeY);
//					int w = tr - i-1;
//					GameObject up = Instantiate(Resources.Load("left")) as GameObject;
//					up.transform.parent = transform;
//					up.transform.position = new Vector3 (x + j * nodeX, y + w * nodeY,0);
//					up.transform.localScale = new Vector3 (0.1f,0.1f,1);
//				}
				if ((map[i][j] & 2) == 2) {
//					g.drawLine (x + j * nodeX, y + (i + 1) * nodeY, x + (j + 1) * nodeX, y + (i + 1) * nodeY);
					int w = tr - i-1;
					GameObject up = Instantiate(Resources.Load("down")) as GameObject;
					up.transform.parent = transform;
					up.transform.position = new Vector3 (x + j * nodeX, y + w * nodeY,0);
					up.transform.localScale = new Vector3 (myScale,myScale,1);
				}
				if ((map[i][j] & 1) == 1) {
//					g.drawLine (x + (j + 1) * nodeX, y + i * nodeY, x + (j + 1) * nodeX, y + (i + 1) * nodeY);
					int w = tr - i-1;
					GameObject up = Instantiate(Resources.Load("right")) as GameObject;
					up.transform.parent = transform;
					up.transform.position = new Vector3 (x + j * nodeX, y + w * nodeY,0);
					up.transform.localScale = new Vector3 (myScale,myScale,1);
				}
			}
		}
	}
	
	// Update is called once per frame
	void Update () {
		
	}
}

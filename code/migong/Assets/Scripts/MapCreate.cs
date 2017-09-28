using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using Example;
using com.protocol;
using System.Threading;

public class MapCreate : MonoBehaviour {
	public int[][] map = new int[][]{ 
		new int[]{0,2	,2,	2,		2},
		new int[]{1,1	,0,	2,		1},
		new int[]{1,1,	2,	2	,	1},
		new int[]{1,0,	1,	0,		1},
		new int[]{1,3,	2,	3,		3}
	};

	public int Level;
	public int Pass;

	int x = 0,y = 0;
	static float myScale = 0.2f;
	static float nodeX = 1.9f * myScale,nodeY = 1.9f * myScale;

	Camera ca;
	float defaultOrthographicSize = 5;

	int tr = 0,td = 0;
	// Use this for initialization
	void Start () {
		createMap ();
	}
	// 注意顺序，从上向下，从下向上
	public int getPointByPosition(Vector2 pos){
		int x = (int)(pos.x / nodeX);
		int y = (int)(pos.y / nodeY);
		int i = (tr - y - 1);
		return i * td + x;
	}

	private void createMap(){
		//		transform.localScale = new Vector3 (0.1f,0.1f,1); // 这样为啥不行
		tr=map.Length;											//行数和列数
		td=map[0].Length;
		Object down = Resources.Load ("down");
		Object right = Resources.Load ("right");
		for (int i = 0; i < tr; i++) {										//绘制墙
			for (int j = 0; j < td; j++) {
				if ((map[i][j] & 2) == 2) {
					int w = tr - i-1;
					GameObject up = Instantiate(down) as GameObject;
					up.transform.parent = transform;
					up.transform.position = new Vector3 (x + j * nodeX, y + w * nodeY,0);
					up.transform.localScale = new Vector3 (myScale,myScale,1);
				}
				if ((map[i][j] & 1) == 1) {
					int w = tr - i-1;
					GameObject up = Instantiate(right) as GameObject;
					up.transform.parent = transform;
					up.transform.position = new Vector3 (x + j * nodeX, y + w * nodeY,0);
					up.transform.localScale = new Vector3 (myScale,myScale,1);
				}
			}
		}
		// 修改相机位置
		GameObject camera = transform.parent.Find("mapCamera").gameObject;
		camera.transform.position = new Vector3(nodeX*(tr-1)/2,nodeY * (td-1)/2,camera.transform.position.z);
		//
		ca = camera.GetComponent<Camera>();


		defaultOrthographicSize = ca.orthographicSize;
		ca.orthographicSize = defaultOrthographicSize * Mathf.Max (tr, td) / 20;
	}

	public void OnSliderChange(float value){
		ca.orthographicSize = defaultOrthographicSize * Mathf.Max (tr, td) / 10 * (value *0.5f+0.25f);
	}
}

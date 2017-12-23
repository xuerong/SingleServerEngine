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


}

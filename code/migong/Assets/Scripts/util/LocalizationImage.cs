using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class LocalizationImage : MonoBehaviour {

	public string imageKey;

	// Use this for initialization
	void Start () {
		if (imageKey == null || imageKey.Length==0) {
			Debug.LogError ("image key is empty ,gameObject name = "+gameObject.name);
		}
		string name = LocalizationImageManager.getImage (imageKey);
		Sprite sp = Resources.Load ("Image/" + name, typeof(Sprite)) as Sprite;
		GetComponent<Image> ().sprite = sp;
	}
}

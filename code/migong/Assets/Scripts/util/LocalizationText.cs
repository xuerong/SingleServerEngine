using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class LocalizationText : MonoBehaviour {

	public string messageKey;

	// Use this for initialization
	void Start () {
		if (messageKey == null || messageKey.Length==0) {
			Debug.LogError ("message key is empty ,gameObject name = "+gameObject.name);
		}
		GetComponent<Text> ().text = Message.getText (messageKey);
	}
}

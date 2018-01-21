using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class PacketItem : MonoBehaviour {

	public int itemId;
	public int count;

	// Use this for initialization
	void Start () {
		transform.Find ("count").GetComponent<Text> ().text = count.ToString ();
		Image image = transform.Find ("image").GetComponent<Image> ();

        Sprite sp = Resources.Load (ShopItem.getSpritePath(itemId), typeof(Sprite)) as Sprite;
		image.sprite = sp;
	}

	public void decCount(int delta){
		count -= delta;
		transform.Find ("count").GetComponent<Text> ().text = count.ToString ();
	}
}

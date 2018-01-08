using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class ButtonIndex : MonoBehaviour {

//	private static Sprite lightSprite = null;
//	private static Sprite offSprite = null;

	public int pass;
	public int star;

	public Image[] starImages;

	void Start(){
		//Debug.Log ("pass="+pass+",star="+star);
//		if (lightSprite == null) {
//			Sprite sp = Resources.Load ("levelImage/lightSprite", typeof(Sprite)) as Sprite;
//			lightSprite = sp;
//		}
//		if (offSprite == null) {
//			Sprite sp = Resources.Load ("levelImage/offSprite", typeof(Sprite)) as Sprite;
//			offSprite = sp;
//		}
		for (int i = 0; i < starImages.Length; i++) {
			if (star > i) {
				starImages [i].sprite = SpriteCache.getLightSprite();
			} else {
				starImages [i].sprite = SpriteCache.getOffSprite ();
			}
		}
	}
}

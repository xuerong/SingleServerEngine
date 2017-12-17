using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class SpriteCache {

	private static Sprite lightSprite = null;
	private static Sprite offSprite = null;

	public static Sprite getLightSprite(){
		if (lightSprite == null) {
			Sprite sp = Resources.Load ("levelImage/lightSprite", typeof(Sprite)) as Sprite;
			lightSprite = sp;
		}
		return lightSprite;
	}
	public static Sprite getOffSprite(){
		if (offSprite == null) {
			Sprite sp = Resources.Load ("levelImage/offSprite", typeof(Sprite)) as Sprite;
			offSprite = sp;
		}
		return offSprite;
	}
}

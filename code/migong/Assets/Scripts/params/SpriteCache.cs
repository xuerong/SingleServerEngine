using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class SpriteCache {

	private static Sprite lightSprite = null;
	private static Sprite offSprite = null;

	private static Dictionary<string,Sprite> sps = new Dictionary<string, Sprite>();

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

	public static Sprite getSprite(string path){
		if (sps.ContainsKey (path)) {
			return sps[path];
		}
		Sprite sp = Resources.Load (path, typeof(Sprite)) as Sprite;
		sps.Add (path,sp);
		return sp;
	}
}

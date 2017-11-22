using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Test : MonoBehaviour {

	// Use this for initialization
	void Start () {
		Renderer renderer = GetComponent<Renderer> ();
		renderer.material.SetTextureOffset ("_Mask0",new Vector2(0,-0.5f));
		Texture texture = Resources.Load("guideMaskView") as Texture;

		renderer.material.CopyPropertiesFromMaterial (renderer.material);
	}
	
	// Update is called once per frame
	void Update () {
		
	}
}

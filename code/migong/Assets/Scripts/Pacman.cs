using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Pacman : MonoBehaviour {

	public float speed = 0.1f;

	Vector2 dest = Vector2.zero;


	void Start () {
		speed = 0.1f;
		transform.position = new Vector3 (0.84f,0,0);
		dest = transform.position;
		transform.localScale = transform.localScale * 0.06f;

	}

	void FixedUpdate () {

		// Move closer to Destination

//		transform.position = p;
		// Check for Input if not moving

//		Debug.Log(transform.position+"--"+dest);
//		if ((Vector2)transform.position == dest) {
//			
//		}
		if (Input.GetKey (KeyCode.UpArrow) ) {
			dest = (Vector2)transform.position + Vector2.up*speed;
//			Vector2 p = Vector2.MoveTowards(transform.position, dest, speed);
			GetComponent<Rigidbody2D>().MovePosition(dest);
		}
		else if (Input.GetKey (KeyCode.RightArrow) ) {
			dest = (Vector2)transform.position + Vector2.right*speed;
//			Vector2 p = Vector2.MoveTowards(transform.position, dest, speed);
			GetComponent<Rigidbody2D>().MovePosition(dest);
		}
		else if (Input.GetKey (KeyCode.DownArrow) ) {
			dest = (Vector2)transform.position - Vector2.up*speed;
//			Vector2 p = Vector2.MoveTowards(transform.position, dest, speed);
			GetComponent<Rigidbody2D>().MovePosition(dest);
		}
		else if (Input.GetKey (KeyCode.LeftArrow) ) {
			dest = (Vector2)transform.position - Vector2.right*speed;
//			Vector2 p = Vector2.MoveTowards(transform.position, dest, speed);
			GetComponent<Rigidbody2D>().MovePosition(dest);
		}





		// Animation Parameters
		Vector2 dir = dest - (Vector2)transform.position;
		GetComponent<Animator>().SetFloat("DirX", dir.x);
		GetComponent<Animator>().SetFloat("DirY", dir.y);
	}
	bool valid(Vector2 dir) {
		// Cast Line from 'next to Pac-Man' to 'Pac-Man'
//		Vector2 pos = transform.position;
//		RaycastHit2D hit = Physics2D.Linecast(pos + dir, pos);
//		return (hit.collider == GetComponent<Collider2D>());
		return true;
	}
}

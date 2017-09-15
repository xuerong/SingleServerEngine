using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System.Text;

public class Pacman : MonoBehaviour {

	public MapCreate mapCreate;
	public float speed = 0.04f;

	Vector2 dest = Vector2.zero;

	CircleCollider2D c;
	float radius = 0;

	bool isControl = false;

	int lastPoint = 0;// 上一个点
	List<int> route = new List<int>();
	void Start () {
		speed = 0.04f;
		transform.position = new Vector3 (0.38f,0.01f,0);
		dest = transform.position;
		transform.localScale = transform.localScale * 0.12f;

		c = GetComponent<CircleCollider2D> ();
		radius = c.radius * Mathf.Max (transform.localScale.x, transform.localScale.y);
	}

	void FixedUpdate () {
		isControl = false;

		int dir = getDir ();
		if (dir > 0) {
			isControl = true;
			switch (dir) {
			case 1:dest = (Vector2)transform.position + Vector2.up*speed;
				break;
			case 2:dest = (Vector2)transform.position + Vector2.right*speed;
				break;
			case 3:dest = (Vector2)transform.position - Vector2.up*speed;
				break;
			case 4:dest = (Vector2)transform.position - Vector2.right*speed;
				break;
			}

			// Animation Parameters
			Vector2 dirVec = dest - (Vector2)transform.position;
			GetComponent<Animator>().SetFloat("DirX", dirVec.x);
			GetComponent<Animator>().SetFloat("DirY", dirVec.y);

			// 记录行踪
			int curPoint = mapCreate.getPointByPosition(new Vector2(transform.position.x + radius/2,transform.position.y + radius/2));
			if (curPoint != lastPoint) {
				route.Add (curPoint);
				lastPoint = curPoint;
				if (route.Count % 10 == 0) {
					StringBuilder sb = new StringBuilder ();
					foreach(int po in route){
						sb.Append (po+",");
					}
					Debug.Log (sb.ToString());
				}
			}
		}
		if (dest != (Vector2)transform.position) {
			if (isControl && valid (dest)) {
				GetComponent<Rigidbody2D> ().MovePosition (dest);
			} else {
				dest = (Vector2)transform.position;
			}
		}
	}

	int getDir(){
		if (Input.GetKey (KeyCode.UpArrow) ) {
			return 1;
		}
		else if (Input.GetKey (KeyCode.RightArrow) ) {
			return 2;
		}
		else if (Input.GetKey (KeyCode.DownArrow) ) {
			return 3;
		}
		else if (Input.GetKey (KeyCode.LeftArrow) ) {
			return 4;
		}
		return 0;
	}

	void fixDest(){
		Vector2 pos = transform.position;
		RaycastHit2D hit = Physics2D.Linecast(pos, dest*100);
		float radius = c.radius * Mathf.Max (transform.localScale.x, transform.localScale.y);
		if (dest.x - pos.x > 0) {
			if (dest.x + radius > hit.point.x) {
				dest = new Vector2 (hit.point.x - radius,dest.y);
			}
		} else if (dest.x - pos.x < 0) {
			if (dest.x - radius < hit.point.x) {
				dest = new Vector2 (hit.point.x + radius,dest.y);
			}
		} else if (dest.y - pos.y > 0) {
			if (dest.y + radius > hit.point.y) {
				dest = new Vector2 (dest.x,hit.point.y - radius);
			}
		} else if (dest.y - pos.y < 0) {
			if (dest.y - radius < hit.point.y) {
				Debug.Log ("before"+pos+""+dest+","+hit.point+","+radius);
				dest = new Vector2 (dest.x ,hit.point.y + radius);
				Debug.Log ("after"+dest);
			}
		}
	}
	bool valid(Vector2 dir) {
		// Cast Line from 'next to Pac-Man' to 'Pac-Man'
		Vector2 pos = transform.position;
		RaycastHit2D hit = Physics2D.Linecast(dir, pos);
		Collider2D c = GetComponent<Collider2D> ();
//		Debug.Log (dir+","+pos+","+c+","+hit.collider);
		return (hit.collider == c);
//		return true;
	}
}

using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System.Text;
using Example;
using com.protocol;

public class Pacman : MonoBehaviour {

	Dictionary<string,Pacman> pacmanMap ;
	List<CircleCollider2D> pacmanColliders ;

	public MapCreate mapCreate;
	public float speed = 0.04f;

	public int score = 0; // 用于记录分数，及对应星级

	public int Dir = 0;
	public int LastDir = 0;
	// 起点和终点
	public int inX;
	public int inY;
	public int outX;
	public int outY;

	public string userId = SocketManager.ACCOUNT_ID;

	Vector2 dest = Vector2.zero;

	CircleCollider2D c;
	float radius = 0;

	bool isControl = false;

	int lastPoint = 0;// 上一个点

	Animator animator;
	Rigidbody2D digidbody;

	List<int> route = new List<int>();

	bool finish;
	void Start () {
		pacmanMap = mapCreate.pacmanMap;
		pacmanColliders = mapCreate.pacmanColliders;

		if (userId == null || userId.Length == 0) {
			this.userId = SocketManager.ACCOUNT_ID;
		}
		speed = 0.2f;

		transform.position = mapCreate.getStartPointWithScale (inX,inY);

		dest = transform.position;
		transform.localScale = transform.localScale * 0.6f;

		c = GetComponent<CircleCollider2D> ();
		radius = c.radius * Mathf.Max (transform.localScale.x, transform.localScale.y);

		animator = GetComponent<Animator> ();
		digidbody = GetComponent<Rigidbody2D> ();

		// 联网模式
		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCUserMove, userMoveAction);
		//
		if(pacmanColliders.Count > 0 ){
			foreach(CircleCollider2D cc in pacmanColliders){
				Physics2D.IgnoreCollision (cc,c);
			}
		}
		pacmanColliders.Add (c);

		pacmanMap.Add (userId,this);
		mapCreate.addScoreShow (userId);

		mapCreate.setEndEffect (outX, outY);

		Debug.Log (inX+","+inY+","+outX+","+outY+ "  ");
	}

	void FixedUpdate () {
		if (finish) {
			return;
		}
		isControl = false;
		if (userId == SocketManager.ACCOUNT_ID) {
			keyboardDir ();
		}
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
			animator.SetFloat("DirX", dirVec.x);
			animator.SetFloat("DirY", dirVec.y);

			// 记录行踪:自己的人才记录
			if (userId == SocketManager.ACCOUNT_ID) {
				int curPoint = mapCreate.getPointByPosition (new Vector2 (transform.position.x, transform.position.y));
				if (curPoint != lastPoint) {
					route.Add (curPoint);
					lastPoint = curPoint;
					mapCreate.checkEatBean (curPoint);
//				if (route.Count % 10 == 0) {
//					StringBuilder sb = new StringBuilder ();
//					foreach(int po in route){
//						sb.Append (po+",");
//					}
//					Debug.Log (sb.ToString());
//				}
					if (curPoint == outX * mapCreate.size + outY) {
						Debug.Log (inX + "," + inY + "," + outX + "," + outY + "  finish,666");
						finish = true;
						mapCreate.passFinish (finish, route);
					}

				}
			}
		}
		if (dest != (Vector2)transform.position) {
			if (isControl && valid (dest)) {
				digidbody.MovePosition (dest);
			} else {
				dest = (Vector2)transform.position;
			}
		}
	}
	void keyboardDir(){
		if (Input.GetKey (KeyCode.UpArrow)) {
			setDir (1,0);
		} else if (Input.GetKey (KeyCode.RightArrow)) {
			setDir (2,0);
		} else if (Input.GetKey (KeyCode.DownArrow)) {
			setDir (3,0);
		} else if (Input.GetKey (KeyCode.LeftArrow)) {
			setDir (4,0);
		} else {
			setDir (0,0);
		}
	}
	int getDir(){
		return Dir;
	}

	public void setDir(int dir,int mode){
		if (mode == 1) {
			return;
		}
		if (LastDir == dir) {
			return;
		}
//		Debug.Log (LastDir+","+dir);
		if (mapCreate.Mode == MapMode.Level || mapCreate.Mode == MapMode.Unlimited) {
			this.Dir = dir;
		} else {
			this.Dir = dir;
			// 发送
			CSMove move = new CSMove();
			move.Dir = dir;
			move.PosX = transform.localPosition.x;
			move.PosY = transform.localPosition.y;
			Debug.Log ("send:"+move.PosX+","+move.PosY);
			move.Speed = 10;
			byte[] data = CSMove.SerializeToBytes (move);
			SocketManager.SendMessageAsyc((int)MiGongOpcode.CSMove,data,delegate(int opcode, byte[] reData) {
				
			});
		}
		LastDir = dir;
	}

	public void userMoveAction(int opcode, byte[] data){
		SCUserMove userMove = SCUserMove.Deserialize (data);
		foreach(PBUserMoveInfo userMoveInfo in userMove.UserMoveInfos){
//			userMoveInfo.Frame
			if(userMoveInfo.UserId.Equals(SocketManager.ACCOUNT_ID)){
//				this.Dir = userMoveInfo.Dir;
//				transform.localPosition = new Vector3 (userMoveInfo.PosX,userMoveInfo.PosY,transform.localPosition.z);
			}else{
				Pacman pacman = pacmanMap [userMoveInfo.UserId];
				pacman.Dir = userMoveInfo.Dir;
				Debug.Log ("receive:"+userMoveInfo.PosX+","+userMoveInfo.PosY);
				pacman.transform.localPosition = new Vector3 (userMoveInfo.PosX,userMoveInfo.PosY,pacman.transform.localPosition.z);
			}
		}
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
//		Physics2D.lin
//		Collider2D c = GetComponent<Collider2D> ();
//		Debug.Log (dir+","+pos+","+c+","+hit.collider);
		foreach(Collider2D cc in pacmanColliders){
			if (hit.collider == cc) {
				return true;
			}
		}
//		return (hit.collider == c);
		return true;
	}
}

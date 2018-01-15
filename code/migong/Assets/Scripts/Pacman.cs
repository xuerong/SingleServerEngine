using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System.Text;
using Example;
using com.protocol;

public class Pacman : MonoBehaviour {

	List<CircleCollider2D> pacmanColliders ;

	public MapCreate mapCreate;
	public float speed = 0.04f;
	private float defaultSpeed ; // 默认速度，这个在使用道具之后，使得加速不能叠加

	public int score = 0; // 用于记录分数，及对应星级

	public int Dir = 0;
	public int LastDir = 0;

	public int JocDir = 0;


	public Vector3 MovePosiNorm = Vector3.zero;
	private Vector3 lastMovePosiNorm = Vector3.zero;
	// 起点和终点
	public int inX;
	public int inY;
	public int outX;
	public int outY;

	public string userId;

	Vector2 dest = Vector2.zero;

	CircleCollider2D c;

	bool isControl = false;

	int lastPoint = 0;// 上一个点

	Animator animator;
	Rigidbody2D digidbody;

	public List<int> route = new List<int>();

	public bool finish;

	public int mulBean = 1;

	void Start () {
		pacmanColliders = mapCreate.pacmanColliders;
		if (userId == null || userId.Length == 0) {
			this.userId = SocketManager.accountId;
		}
		//speed /= 100f;
		defaultSpeed = speed;

		transform.localPosition = mapCreate.getStartPointWithScale (inX,inY);

		dest = transform.localPosition;
		transform.localScale = transform.localScale * 1f;

		c = GetComponent<CircleCollider2D> ();

		animator = GetComponent<Animator> ();
		digidbody = GetComponent<Rigidbody2D> ();

		// 联网模式
//		SocketManager.AddServerSendReceive ((int)MiGongOpcode.SCUserMove, userMoveAction);
		//
		if(pacmanColliders.Count > 0 ){
			foreach(CircleCollider2D cc in pacmanColliders){
				Physics2D.IgnoreCollision (cc,c);
			}
		}
		pacmanColliders.Add (c);

		mapCreate.addScoreShow (userId);

		mapCreate.setEndEffect (outX, outY);

		Debug.Log (inX+","+inY+","+outX+","+outY+ "  ");
	}

	public void addSpeed(int delta){
		//speed = defaultSpeed + delta / 100f;
        speed = defaultSpeed + delta;
	}

	void Update () {
        if(mapCreate.gameOver){
            return;
        }
		if (finish) {
			return;
		}
		isControl = false;
//		if (userId == SocketManager.accountId) {
//			keyboardDir ();
//		}
//		int dir = getDir ();
//		if (dir > 0) {
//			isControl = true;
//			switch (dir) {
//			case 1:dest = (Vector2)transform.position + Vector2.up*speed;
//				break;
//			case 2:dest = (Vector2)transform.position + Vector2.right*speed;
//				break;
//			case 3:dest = (Vector2)transform.position - Vector2.up*speed;
//				break;
//			case 4:dest = (Vector2)transform.position - Vector2.right*speed;
//				break;
//			}
//
//			// Animation Parameters
//			Vector2 dirVec = dest - (Vector2)transform.position;
//			animator.SetFloat("DirX", dirVec.x);
//			animator.SetFloat("DirY", dirVec.y);
//
//			// 记录行踪:自己的人才记录
//			if (userId == SocketManager.accountId) {
//				int curPoint = mapCreate.getPointByPosition (new Vector2 (transform.localPosition.x, transform.localPosition.y));
//				if (curPoint != lastPoint) {
//					route.Add (curPoint);
//					lastPoint = curPoint;
//					mapCreate.checkEatBean (curPoint);
////				if (route.Count % 10 == 0) {
////					StringBuilder sb = new StringBuilder ();
////					foreach(int po in route){
////						sb.Append (po+",");
////					}
////					Debug.Log (sb.ToString());
////				}
//					if (curPoint == outX * mapCreate.size + outY) {
//						Debug.Log (inX + "," + inY + "," + outX + "," + outY + "  finish,666");
//						finish = true;
//						mapCreate.selfArrive (finish, route,true);
//						StringBuilder sb = new StringBuilder ();
//						foreach(int po in route){
//							sb.Append (po+",");
//						}
//						Debug.Log (sb.ToString());
//					}
//
//				}
//			}
//		}




		if (MovePosiNorm != Vector3.zero) {
			isControl = true;
            float step = speed*Time.deltaTime;
            dest = (Vector2)transform.position + new Vector2(step*MovePosiNorm.x,step*MovePosiNorm.y);
			// Animation Parameters
			Vector2 dirVec = dest - (Vector2)transform.position;
			animator.SetFloat("DirX", dirVec.x);
			animator.SetFloat("DirY", dirVec.y);

			// 记录行踪:自己的人才记录
			if (userId == SocketManager.accountId) {
				int curPoint = mapCreate.getPointByPosition (new Vector2 (transform.localPosition.x, transform.localPosition.y));
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
						mapCreate.selfArrive (finish, route,true);
						StringBuilder sb = new StringBuilder ();
						foreach(int po in route){
							sb.Append (po+",");
						}
						Debug.Log (sb.ToString());
					}

				}
			}
		}



		if (dest != (Vector2)transform.position) {
			if (isControl && valid (dest)) {
				Sound.playSound(SoundType.Move);
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
	public void setMovePosiNorm(Vector3 movePosiNorm){
		if (lastMovePosiNorm == movePosiNorm) {
			return;
		}
		if (mapCreate.Mode == MapMode.Level || mapCreate.Mode == MapMode.Unlimited) {
			this.MovePosiNorm = movePosiNorm;
		} else {
			this.MovePosiNorm = movePosiNorm;
			// 发送
			CSMove move = new CSMove();
			move.DirX= movePosiNorm.x;
			move.DirY= movePosiNorm.y;
			move.PosX = transform.localPosition.x;
			move.PosY = transform.localPosition.y;
			Debug.Log ("send:"+move.PosX+","+move.PosY);
			move.Speed = 10;
			byte[] data = CSMove.SerializeToBytes (move);
			SocketManager.SendMessageAsyc((int)MiGongOpcode.CSMove,data,delegate(int opcode, byte[] reData) {

			});
		}
		lastMovePosiNorm = movePosiNorm;
	}
	// 暫時不用
	public void setDir(int dir,int mode){
		if (mode == 1) {
			this.JocDir = dir;
		}
		if (dir == 0) {
			dir = this.JocDir;
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
//			move.Dir = dir;
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
		
	void fixDest(){
		Vector2 pos = transform.localPosition;
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
		Vector2 pos = transform.localPosition;
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

	void OnTriggerEnter2D(Collider2D collider)
	{
		//进入触发器执行的代码
        if(mapCreate.gameOver){
            //Debug.LogWarning("ok here!!");
            return;
        }
		Debug.Log("OnTriggerEnter2D:"+collider);
        mapCreate.selfArrive(false,route,true);
	}
	//void OnCollisionEnter2D(Collision2D collision)
	//{
	//	//进入碰撞器执行的代码
	//	Debug.Log("OnCollisionEnter2D:"+collision);
	//}
}

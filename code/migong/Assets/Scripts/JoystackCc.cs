﻿using UnityEngine;

using System.Collections;

// EasyTouch
public class JoystackCc : MonoBehaviour {

	private Vector3 Origin;


	Transform mTrans;


	private Vector3 _deltaPos;

	private bool _drag = false;


	private Vector3 deltaPosition;


	float dis;

	[SerializeField]

	private float MoveMaxDistance = 80;            //最大拖动距离


	[HideInInspector]

	public Vector3 FiexdMovePosiNorm; //固定8个角度移动的距离


	[HideInInspector]

	public Vector3 MovePosiNorm;  //标准化移动的距离

	[SerializeField]

	private float ActiveMoveDistance = 1;              //激活移动的最低距离

	private Pacman pacman;

    public MapCreate mapCreate;

	void Awake()

	{
//		EventTriggerListener.Get(gameObject).onDrag = OnDrag;
//
//		EventTriggerListener.Get(gameObject).onDragOut = OnDragOut;
//
//
//		EventTriggerListener.Get(gameObject).onDown = OnMoveStart;

	}



	// Use this for initialization

	void Start () {
        mapCreate = transform.parent.parent.parent.Find("content/map").gameObject.GetComponent<MapCreate>();
        if (mapCreate.Mode == MapMode.Online)
        {
            Vector3 old = transform.parent.localPosition;
            transform.parent.localPosition = new Vector3(old.x,old.y - 20,old.z);
        }

		Origin = transform.localPosition;//设置原点

		mTrans = transform;

		pacman = transform.parent.parent.parent.Find("content/pacman").gameObject.GetComponent<Pacman>();



	}



	// Update is called once per frame

	void Update()

	{

		dis = Vector3.Distance(transform.localPosition, Origin);//拖动距离，这不是最大的拖动距离，是根据触摸位置算出来的

//		if(dis >= MoveMaxDistance)       //如果大于可拖动的最大距离
//
//		{
//			Vector3 vec = Origin + (transform.localPosition - Origin) * MoveMaxDistance / dis;  //求圆上的一点：(目标点-原点) * 半径/原点到目标点的距离
//			transform.localPosition = vec;
//		}

		if (Vector3.Distance (transform.localPosition, Origin) > ActiveMoveDistance) { //距离大于激活移动的距离
            Vector3 dir = transform.localPosition - Origin;
            if(true){
                if(Mathf.Abs(dir.x) > Mathf.Abs(dir.y)){
                    dir = new Vector3(dir.x, 0, 0);
                }else{
                    dir = new Vector3(0, dir.y, 0);
                }

            }
            MovePosiNorm = dir.normalized;
		} else {
			MovePosiNorm = Vector3.zero;
		}
		// 根据MovePosiNorm设置Paceman的Dir
//		pacman.Dir
//		if (MovePosiNorm == Vector3.zero) {
//			pacman.setDir (0,1);
//		} else if (Mathf.Abs (MovePosiNorm.x) >= Mathf.Abs (MovePosiNorm.y)) {
//			if (MovePosiNorm.x > 0) {
//				pacman.setDir (2,1);
//			} else {
//				pacman.setDir (4,1);
//			}
//		} else {
//			if (MovePosiNorm.y > 0) {
//				pacman.setDir (1,1);
//			} else {
//				pacman.setDir (3,1);
//			}
//		}
		pacman.setMovePosiNorm(MovePosiNorm);
//		if (MovePosiNorm == Vector3.zero) {
//			pacman.Dir2 = -1;
//		} else if (Mathf.Abs (MovePosiNorm.x) >= Mathf.Abs (MovePosiNorm.y)) {
//			if (MovePosiNorm.x > 0) {
//				pacman.setDir (2,1);
//			} else {
//				pacman.setDir (4,1);
//			}
//		} else {
//			if (MovePosiNorm.y > 0) {
//				pacman.setDir (1,1);
//			} else {
//				pacman.setDir (3,1);
//			}
//		}
	}

	void MiouseDown()

	{

		if((Input.touchCount > 0 && Input.GetTouch(0).phase == TouchPhase.Moved))

		{

		}

		else

			mTrans.localPosition = Origin;

	}

	Vector3 result;

	private Vector3 _checkPosition(Vector3 movePos, Vector3 _offsetPos)

	{

		result = movePos + _offsetPos;

		return result;

	}


	void OnDrag(GameObject go, Vector2 delta)

	{

		if(!_drag)

		{
			_drag = true;
		}

		_deltaPos = delta;


		mTrans.localPosition +=new Vector3(_deltaPos.x, _deltaPos.y, 0);

	}


	void OnDragOut(GameObject go)

	{

		_drag =false;

		mTrans.localPosition = Origin;

//		if(PlayerMoveControl.moveEnd != null) PlayerMoveControl.moveEnd();

	}


	void OnMoveStart(GameObject go)

	{

//		if(PlayerMoveControl.moveStart != null) PlayerMoveControl.moveStart();

	}

}
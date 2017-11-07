using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;

public class DragControl : MonoBehaviour, IBeginDragHandler, IDragHandler, IEndDragHandler {
	Transform caTran;
	Camera ca;
	RectTransform canvasTran;
	MapCreate mapCreate;
	// Use this for initialization
	void Start () {
		mapCreate = transform.parent.parent.Find ("content/map").GetComponent<MapCreate>();
		ca = transform.parent.parent.Find ("mapCamera").GetComponent<Camera>();
		caTran = ca.transform;
		canvasTran = (RectTransform)(transform.parent);
	}

	public void OnBeginDrag(PointerEventData eventData)
	{
		
		Debug.Log ("eventData.position:"+eventData.position);
	}
	public void OnDrag(PointerEventData data)
	{
		
		float cameraScale = canvasTran.rect.height /2/ ca.orthographicSize;
		float deltaX = data.delta.x/cameraScale;
		float deltaY = data.delta.y/cameraScale;

		float canvasWidth = canvasTran.rect.width /  cameraScale;
		float canvasHeight = canvasTran.rect.height / cameraScale;

		Vector3 caPos = caTran.localPosition - new Vector3 (deltaX,deltaY,0);
		// 如果够小，直接pass\
		if (mapCreate.mapRect.width < canvasWidth) {
			deltaX = 0;
		} else {
			if (caPos.x < canvasWidth / 2) {
				deltaX = caTran.localPosition.x - canvasWidth / 2;
			} else if (caPos.x > mapCreate.mapRect.width - canvasWidth / 2) {
				deltaX = caTran.localPosition.x - (mapCreate.mapRect.width - canvasWidth / 2);
			}
		}

		if (mapCreate.mapRect.height < canvasHeight) {
			deltaY = 0;
		} else {
//			Debug.Log ("canvasHeight="+canvasHeight+",mapCreate.nodeY="+mapCreate.nodeY);
			if (caPos.y < canvasHeight / 2 - mapCreate.nodeY) {
				deltaY = caTran.localPosition.y - (canvasHeight / 2-mapCreate.nodeY);
			} else if (caPos.y > mapCreate.mapRect.height - canvasHeight / 2-mapCreate.nodeY) {
				deltaY = caTran.localPosition.y - (mapCreate.mapRect.height - canvasHeight / 2-mapCreate.nodeY);
			}
		}

		caTran.localPosition -= new Vector3 (deltaX,deltaY,0);
	}
	public void OnEndDrag(PointerEventData eventData)
	{
		
	}
}

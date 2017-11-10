using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using UnityEditor;
public class simulateSwitchToBackground : MonoBehaviour {
	void sendApplicationPauseMessage(bool isPause){
		Transform[] transList= GameObject.FindObjectsOfType<Transform>(); 
		for (int i = 0; i < transList.Length; i++) {
			Transform trans = transList [i];
			//Note that messages will not be sent to inactive objects
			trans.SendMessage ("OnApplicationPause",isPause,SendMessageOptions.DontRequireReceiver);
		}
	}
	void sendApplicationFocusMessage(bool isFocus){
		Transform[] transList= GameObject.FindObjectsOfType<Transform>(); 
		for (int i = 0; i < transList.Length; i++) {
			Transform trans = transList [i];
			//Note that messages will not be sent to inactive objects
			trans.SendMessage ("OnApplicationFocus",isFocus,SendMessageOptions.DontRequireReceiver);
		}
	}

	public void sendEnterBackgroundMessage(){
		sendApplicationPauseMessage (true);
		sendApplicationFocusMessage (false);

	}
	public void sendEnterFoegroundMessage(){
		sendApplicationFocusMessage (true);
		sendApplicationPauseMessage (false);

	}

}



//simulateSwitchToBackgroundEditor.cs


[CustomEditor(typeof(simulateSwitchToBackground))]
public class simulateSwitchToBackgroundEditor : Editor
{

	void OnEnable(){
	}

	public override void OnInspectorGUI()
	{


		DrawDefaultInspector();
		serializedObject.Update ();
		serializedObject.ApplyModifiedProperties ();//now varibles in script have been updated


		if (GUILayout.Button ("send enter background message")) {
			if (Application.isPlaying) {
				((simulateSwitchToBackground)target).sendEnterBackgroundMessage ();
			}
		}
		if (GUILayout.Button ("send enter foeground message")) {
			if (Application.isPlaying) {
				((simulateSwitchToBackground)target).sendEnterFoegroundMessage ();
			}
		}
	}


}
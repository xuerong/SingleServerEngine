using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Message {

	private static string systemLanguage = Application.systemLanguage.ToString();

	private static Dictionary<string, string> dic = new Dictionary<string, string>();    
	/**
	 * Afrikaans,
		Arabic,
		Basque,
		Belarusian,
		Bulgarian,
		Catalan,
		Chinese,
		Czech,
		Danish,
		Dutch,
		English,
		Estonian,
		Faroese,
		Finnish,
		French,
		German,
		Greek,
		Hebrew,
		Hugarian,
		Icelandic,
		Indonesian,
		Italian,
		Japanese,
		Korean,
		Latvian,
		Lithuanian,
		Norwegian,
		Polish,
		Portuguese,
		Romanian,
		Russian,
		SerboCroatian,
		Slovak,
		Slovenian,
		Spanish,
		Swedish,
		Thai,
		Turkish,
		Ukrainian,
		Vietnamese,
		ChineseSimplified,
		ChineseTraditional,
		Unknown,
		Hungarian = 18
	 */
	static Message(){
		systemLanguage = Application.systemLanguage.ToString();
//		systemLanguage = "ddd";

        //WarnDialog.showWarnDialog(systemLanguage);

		TextAsset ta = Resources.Load<TextAsset> ("Message/"+systemLanguage);
		if (ta == null) {
			ta = Resources.Load<TextAsset> ("Message/English"); // 没配置的国家用英文
		}
		string text = ta.text;

		string[] lines = text.Split('\n');    
		foreach (string line in lines)    
		{    
			if (line == null || !line.Contains("="))    
			{    
				continue;    
			}    
			string[] keyAndValue = line.Split('=');
			string value = keyAndValue [1].Trim();
			if(value.Contains("\\n")){ // 处理换行符
				value = value.Replace ("\\n","\n");
			}
			dic.Add(keyAndValue[0].Trim(), value);    
		}    
	}

	public static string getText(string key){
		if (!dic.ContainsKey (key)) {
			return null;
		}
		return dic[key]; 
	}
	public static string getText(string key,object arg0){
		if (!dic.ContainsKey (key)) {
			return null;
		}
		return string.Format(dic[key],arg0); 
	}
	public static string getText(string key,object arg0,object arg1){
		if (!dic.ContainsKey (key)) {
			return null;
		}
		return string.Format(dic[key],arg0,arg1); 
	}
	public static string getText(string key,object arg0,object arg1,object arg2){
		if (!dic.ContainsKey (key)) {
			return null;
		}
		return string.Format(dic[key],arg0,arg1,arg2); 
	}
	public static string getText(string key,params object[] args){
		if (!dic.ContainsKey (key)) {
			return null;
		}
		return string.Format(dic[key],args); 
	}
}

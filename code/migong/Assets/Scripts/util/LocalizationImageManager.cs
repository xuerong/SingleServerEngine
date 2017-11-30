using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class LocalizationImageManager {

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
	static LocalizationImageManager(){
		systemLanguage = Application.systemLanguage.ToString();
//		systemLanguage = "ddd";

		TextAsset ta = Resources.Load<TextAsset> ("Image/"+systemLanguage);
		if (ta == null) {
			ta = Resources.Load<TextAsset> ("Image/English"); // 没配置的国家用英文
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
			dic.Add(keyAndValue[0].Trim(), value);    
		}    
	}

	public static string getImage(string key){
		if (!dic.ContainsKey (key)) {
			return null;
		}
		return dic[key]; 
	}
}

#if UNITY_ANDROID || UNITY_IPHONE || UNITY_STANDALONE_OSX || UNITY_TVOS
// WARNING: Do not modify! Generated file.

namespace UnityEngine.Purchasing.Security {
    public class GooglePlayTangle
    {
        private static byte[] data = System.Convert.FromBase64String("Bmd2HzksPAAnfNvkWd5721qPoJHUd0IOGGn1yyc2iDLKCAHXF0E/Oc1//N/N8Pv013u1ewrw/Pz8+P3+f/zy/c1//Pf/f/z8/TD+shC6X0mn0Ku4KE9NLdjR6jcGBsZoFKPf6EBznNIM+fywCtvc2RxwyDtQfw579EPhCZTg203OVeNTenCIPKgTiN2sc97tKrjE0kIk2jQfr5o8EyO+R4aUDzASBs4XZYsT2F2gS0Nu/R1Dr8CLK0A47XCyL09Wbx9BbLAohGoN98poQ1vD8kp/AQoDQvFqnp4TSaZMngqT/o1L1BWa2RyT2Hbw5eyhpPggWMGOYhPMrcKUxavSbuVkWPYdVN4Zh+T5swkBihWs+ZD1CfJb8gVIpl0uBEpSuP/+/P38");
        private static int[] order = new int[] { 8,7,8,7,6,13,7,11,8,11,11,13,12,13,14 };
        private static int key = 253;

        public static readonly bool IsPopulated = true;

        public static byte[] Data() {
        	if (IsPopulated == false)
        		return null;
            return Obfuscator.DeObfuscate(data, order, key);
        }
    }
}
#endif

using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.Purchasing;
using UnityEngine.Purchasing.Security;

// Placing the Purchaser class in the CompleteProject namespace allows it to interact with ScoreManager, 
// one of the existing Survival Shooter scripts.
// Deriving the Purchaser class from IStoreListener enables it to receive messages from Unity Purchasing.
//脚本需在调用购买方法之前初始化
public class Purchaser : MonoBehaviour, IStoreListener

{  
    

    //定义商品
    private const string product_1 = "s_1";
    private const string product_2 = "s_2";
    private const string product_3 = "s_3";
    private const string product_6 = "s_6";

    private static IStoreController m_StoreController;          // The Unity Purchasing system.
    private static IExtensionProvider m_StoreExtensionProvider; // The store-specific Purchasing subsystems.

    public static string kProductIDConsumable = "consumable";
    public static string kProductIDNonConsumable = "nonconsumable";
    public static string kProductIDSubscription = "subscription";
    // Apple App Store-specific product identifier for the subscription product.
    private static string kProductNameAppleSubscription = "com.unity3d.subscription.new";
    // Google Play Store-specific product identifier subscription product.
    private static string kProductNameGooglePlaySubscription = "com.unity3d.subscription.original";


    void Start()
    {
        // If we haven't set up the Unity Purchasing reference
        if (m_StoreController == null)
        {
            // Begin to configure our connection to Purchasing
            InitializePurchasing();
        }
    }
    public void InitializePurchasing()
    {
        // If we have already connected to Purchasing ...
        if (IsInitialized())
        {
            // ... we are done here.
            return;
        }
        // Create a builder, first passing in a suite of Unity provided stores.
        var builder = ConfigurationBuilder.Instance(StandardPurchasingModule.Instance());

        //添加商品ID和类型 对应定义的商品ID
        builder.AddProduct(product_1, ProductType.Consumable, new IDs
            {
            {"s_1", GooglePlay.Name }
            });
        builder.AddProduct(product_2, ProductType.Consumable, new IDs
            {
            {"s_2", GooglePlay.Name }
            });
        builder.AddProduct(product_3, ProductType.Consumable, new IDs
            {
            {"s_3", GooglePlay.Name }
            });
        builder.AddProduct(product_6, ProductType.Consumable, new IDs
            {
            {"s_6", GooglePlay.Name }
            });


        // Kick off the remainder of the set-up with an asynchrounous call, passing the configuration 
        // and this class' instance. Expect a response either in OnInitialized or OnInitializeFailed.
        //WarnDialog.showWarnDialog("Initialize before");
        UnityPurchasing.Initialize(this, builder);
        //UnityPurchasing.I

        //WarnDialog.showWarnDialog("Initialize end");
    }
    private bool IsInitialized()
    {
        // Only say we are initialized if both the Purchasing references are set.
        return m_StoreController != null && m_StoreExtensionProvider != null;
    }
    public void BuyConsumable()
    {
        // Buy the consumable product using its general identifier. Expect a response either 
        // through ProcessPurchase or OnPurchaseFailed asynchronously.
        BuyProductID(kProductIDConsumable);
    }
    public void BuyNonConsumable()
    {
        // Buy the non-consumable product using its general identifier. Expect a response either 
        // through ProcessPurchase or OnPurchaseFailed asynchronously.
        BuyProductID(kProductIDNonConsumable);
    }
    public void BuySubscription()
    {
        // Buy the subscription product using its the general identifier. Expect a response either 
        // through ProcessPurchase or OnPurchaseFailed asynchronously.
        // Notice how we use the general product identifier in spite of this ID being mapped to
        // custom store-specific identifiers above.
        BuyProductID(kProductIDSubscription);
    }

    //购买商品调用的方法
    public void BuyProductID(string productId)
    {
        // If Purchasing has been initialized ...
        if (IsInitialized())
        {
            // ... look up the Product reference with the general product identifier and the Purchasing 
            // system's products collection.
            Product product = m_StoreController.products.WithID(productId);

            // If the look up found a product for this device's store and that product is ready to be sold ... 
            if (product != null && product.availableToPurchase)
            {
                Debug.Log(string.Format("Purchasing product asychronously: '{0}'", product.definition.id));
                // ... buy the product. Expect a response either through ProcessPurchase or OnPurchaseFailed 
                // asynchronously.
                m_StoreController.InitiatePurchase(product);
                //m_StoreController.
                //m_StoreController.
                //product.a
            }
            // Otherwise ...
            else
            {
                // ... report the product look-up failure situation  
                Debug.Log("BuyProductID: FAIL. Not purchasing product, either is not found or is not available for purchase");
            }
        }
        // Otherwise ...
        else
        {
            // ... report the fact Purchasing has not succeeded initializing yet. Consider waiting longer or 
            // retrying initiailization.
            WarnDialog.showWarnDialog("BuyProductID FAIL. Not initialized.");
            Debug.Log("BuyProductID FAIL. Not initialized.");
        }
    }


    // Restore purchases previously made by this customer. Some platforms automatically restore purchases, like Google. 
    // Apple currently requires explicit purchase restoration for IAP, conditionally displaying a password prompt.
    public void RestorePurchases()
    {
        // If Purchasing has not yet been set up ...
        if (!IsInitialized())
        {
            // ... report the situation and stop restoring. Consider either waiting longer, or retrying initialization.
            Debug.Log("RestorePurchases FAIL. Not initialized.");
            return;
        }

        // If we are running on an Apple device ... 
        if (Application.platform == RuntimePlatform.IPhonePlayer ||
            Application.platform == RuntimePlatform.OSXPlayer)
        {
            // ... begin restoring purchases
            Debug.Log("RestorePurchases started ...");

            // Fetch the Apple store-specific subsystem.
            var apple = m_StoreExtensionProvider.GetExtension<IAppleExtensions>();
            // Begin the asynchronous process of restoring purchases. Expect a confirmation response in 
            // the Action<bool> below, and ProcessPurchase if there are previously purchased products to restore.
            apple.RestoreTransactions((result) => {
                // The first phase of restoration. If no more responses are received on ProcessPurchase then 
                // no purchases are available to be restored.
                Debug.Log("RestorePurchases continuing: " + result + ". If no further messages, no purchases available to restore.");
            });
        }
        // Otherwise ...
        else
        {
            // We are not running on an Apple device. No work is necessary to restore purchases.
            Debug.Log("RestorePurchases FAIL. Not supported on this platform. Current = " + Application.platform);
        }
    }
    //  
    // --- IStoreListener
    //
    public void OnInitialized(IStoreController controller, IExtensionProvider extensions)
    {
        //WarnDialog.showWarnDialog("OnInitialized: PASS");
        // Purchasing has succeeded initializing. Collect our Purchasing references.
        Debug.Log("OnInitialized: PASS");

        // Overall Purchasing system, configured with products for this application.
        m_StoreController = controller;


        //把产品的价格显示在界面 
        foreach(Product product in controller.products.all){
            //product.
            //string log = "product.availableToPurchase:"+product.availableToPurchase+","+product.definition.
            Debug.Log("product.availableToPurchase:" + product.availableToPurchase);
            Debug.Log("product.definition.enabled:"+product.definition.enabled);
            Debug.Log("product.definition.id:" + product.definition.id);
            Debug.Log("product.definition.storeSpecificId:" + product.definition.storeSpecificId);
            Debug.Log("product.definition.type:" + product.definition.type);
            Debug.Log("product.hasReceipt:" + product.hasReceipt);
            Debug.Log("roduct.metadata.isoCurrencyCode:" + product.metadata.isoCurrencyCode);
            Debug.Log("product.metadata.localizedDescription:" + product.metadata.localizedDescription);
            Debug.Log("product.metadata.localizedPrice:" + product.metadata.localizedPrice);
            Debug.Log("product.metadata.localizedPriceString:" + product.metadata.localizedPriceString);
            Debug.Log("product.metadata.localizedTitle:" + product.metadata.localizedTitle);
            Debug.Log("product.receipt:" + product.receipt);
            Debug.Log("product.transactionID:" + product.transactionID);
        }

        // Store specific subsystem, for accessing device-specific store features.
        m_StoreExtensionProvider = extensions;
    }


    public void OnInitializeFailed(InitializationFailureReason error)
    {
        WarnDialog.showWarnDialog("OnInitializeFailed InitializationFailureReason:" + error);
        // Purchasing set-up has not succeeded. Check error for reason. Consider sharing this reason with the user.
        Debug.Log("OnInitializeFailed InitializationFailureReason:" + error);
    }

    //购买不同商品结束后的处理方法 对应定义的商品
    public PurchaseProcessingResult ProcessPurchase(PurchaseEventArgs e)
    {//https://docs.unity3d.com/Manual/UnityIAPValidatingReceipts.html


        Debug.Log("e.purchasedProduct.availableToPurchase:" + e.purchasedProduct.availableToPurchase);
        Debug.Log("e.purchasedProduct.definition:" + e.purchasedProduct.definition);
        Debug.Log("e.purchasedProduct.hasReceipt:" + e.purchasedProduct.hasReceipt);
        Debug.Log("e.purchasedProduct.metadata:" + e.purchasedProduct.metadata);

        Debug.Log("e.purchasedProduct.receipt:" + e.purchasedProduct.receipt);
        Debug.Log("e.purchasedProduct.transactionID:" + e.purchasedProduct.transactionID);

        Debug.Log("enabled:" + e.purchasedProduct.definition.enabled);
        Debug.Log("id:" +e.purchasedProduct.definition.id);
        Debug.Log("storeSpecificId:" + e.purchasedProduct.definition.storeSpecificId);
        Debug.Log("type:" + e.purchasedProduct.definition.type);

        Debug.Log("isoCurrencyCode:" + e.purchasedProduct.metadata.isoCurrencyCode);
        Debug.Log("localizedDescription:" + e.purchasedProduct.metadata.localizedDescription);
        Debug.Log("localizedPrice:" + e.purchasedProduct.metadata.localizedPrice);
        Debug.Log("localizedPriceString:" + e.purchasedProduct.metadata.localizedPriceString);
        Debug.Log("localizedTitle:" + e.purchasedProduct.metadata.localizedTitle);

        bool validPurchase = true; // Presume valid for platforms with no R.V.

        // Unity IAP's validation logic is only included on these platforms.
        #if UNITY_ANDROID || UNITY_IOS || UNITY_STANDALONE_OSX
        // Prepare the validator with the secrets we prepared in the Editor
        // obfuscation window.
        var validator = new CrossPlatformValidator(GooglePlayTangle.Data(),
            AppleTangle.Data(), Application.identifier);

        try
        {
            // On Google Play, result has a single product ID.
            // On Apple stores, receipts contain multiple products.
            var result = validator.Validate(e.purchasedProduct.receipt);
            // For informational purposes, we list the receipt(s)
            Debug.Log("Receipt is valid. Contents:");
            foreach (IPurchaseReceipt productReceipt in result)
            {
                Debug.Log(productReceipt.productID);
                Debug.Log(productReceipt.purchaseDate);
                Debug.Log(productReceipt.transactionID);

                GooglePlayReceipt google = productReceipt as GooglePlayReceipt;
                if (null != google)
                {
                    // This is Google's Order ID.
                    // Note that it is null when testing in the sandbox
                    // because Google's sandbox does not provide Order IDs.
                    Debug.Log(google.transactionID);
                    Debug.Log(google.purchaseState);
                    Debug.Log(google.purchaseToken);
                }

                AppleInAppPurchaseReceipt apple = productReceipt as AppleInAppPurchaseReceipt;
                if (null != apple)
                {
                    Debug.Log(apple.originalTransactionIdentifier);
                    Debug.Log(apple.subscriptionExpirationDate);
                    Debug.Log(apple.cancellationDate);
                    Debug.Log(apple.quantity);
                }
            }
        }
        catch (IAPSecurityException)
        {
            Debug.Log("Invalid receipt, not unlocking content");
            validPurchase = false;
        }
        #endif

        if (validPurchase)
        {
            // Unlock the appropriate content here.
        }

        //if (Application.platform == RuntimePlatform.Android){
        //    try {  
        //        var result = validator.Validate (e.purchasedProduct.receipt);  
        //        Debug.Log ("Receipt is valid. Contents:");  
        //        foreach (IPurchaseReceipt productReceipt in result) {  
        //            Debug.Log(productReceipt.productID);  
        //            Debug.Log(productReceipt.purchaseDate);  
        //            Debug.Log(productReceipt.transactionID);  

        //            AppleInAppPurchaseReceipt apple = productReceipt as AppleInAppPurchaseReceipt;  
        //            if (null != apple) {  
        //                Debug.Log(apple.originalTransactionIdentifier);  
        //                Debug.Log(apple.subscriptionExpirationDate);  
        //                Debug.Log(apple.cancellationDate);  
        //                Debug.Log(apple.quantity);  


        //                //如果有服务器，服务器用这个receipt去苹果验证。  
        //                //var receiptJson = JSONObject.Parse(e.purchasedProduct.receipt);  
        //                //var receipt = receiptJson.GetString("Payload");  
        //            }  

        //            GooglePlayReceipt google = productReceipt as GooglePlayReceipt;  
        //            if (null != google) {  
        //                Debug.Log(google.purchaseState);  
        //                Debug.Log(google.purchaseToken);  
        //            }  
        //        }  
        //        return PurchaseProcessingResult.Complete;  
        //    } catch (IAPSecurityException) {  
        //        Debug.Log("Invalid receipt, not unlocking content");  
        //        return PurchaseProcessingResult.Complete;  
        //    }  
        //}

        //if (Application.platform == RuntimePlatform.IPhonePlayer)
        //{

        //}


        //WarnDialog.showWarnDialog(e.purchasedProduct.definition.id+","+e.purchasedProduct.receipt+","+e.purchasedProduct.metadata.localizedPrice);

        //try {  
        //    var result = validator.Validate (e.purchasedProduct.receipt);  
        //    Debug.Log ("Receipt is valid. Contents:");  
        //    foreach (IPurchaseReceipt productReceipt in result) {  
        //        Debug.Log(productReceipt.productID);  
        //        Debug.Log(productReceipt.purchaseDate);  
        //        Debug.Log(productReceipt.transactionID);  
      
        //        AppleInAppPurchaseReceipt apple = productReceipt as AppleInAppPurchaseReceipt;  
        //        if (null != apple) {  
        //            Debug.Log(apple.originalTransactionIdentifier);  
        //            Debug.Log(apple.subscriptionExpirationDate);  
        //            Debug.Log(apple.cancellationDate);  
        //            Debug.Log(apple.quantity);  
                      
      
        //            //如果有服务器，服务器用这个receipt去苹果验证。  
        //            //var receiptJson = JSONObject.Parse(e.purchasedProduct.receipt);  
        //            //var receipt = receiptJson.GetString("Payload");  
        //        }  
                  
        //        GooglePlayReceipt google = productReceipt as GooglePlayReceipt;  
        //        if (null != google) {  
        //            Debug.Log(google.purchaseState);  
        //            Debug.Log(google.purchaseToken);  
        //        }  
        //    }  
        //    return PurchaseProcessingResult.Complete;  
        //} catch (IAPSecurityException) {  
        //    Debug.Log("Invalid receipt, not unlocking content");  
        //    return PurchaseProcessingResult.Complete;  
        //}  
        //return PurchaseProcessingResult.Complete;  



        //// A consumable product has been purchased by this user.
        //if (String.Equals(args.purchasedProduct.definition.id, product_1, StringComparison.Ordinal))
        //{
        //    //商品1购买成功逻辑
        //    Debug.Log(args.purchasedProduct.definition.id+","+product_1);
        //}
        //else if (String.Equals(args.purchasedProduct.definition.id, product_2, StringComparison.Ordinal))
        //{

        //    //商品2购买成功逻辑   
        //    Debug.Log(args.purchasedProduct.definition.id + "," + product_2);
        //}
        //else if (String.Equals(args.purchasedProduct.definition.id, product_3, StringComparison.Ordinal))
        //{
        //    //商品3购买成功逻辑
        //    Debug.Log(args.purchasedProduct.definition.id + "," + product_3);
        //}
        //else
        //{
        //    Debug.Log(string.Format("ProcessPurchase: FAIL. Unrecognized product: '{0}'", args.purchasedProduct.definition.id));
        //}

        //// Return a flag indicating whether this product has completely been received, or if the application needs 
        //// to be reminded of this purchase at next app launch. Use PurchaseProcessingResult.Pending when still 
        //// saving purchased products to the cloud, and when that save is delayed. 
        return PurchaseProcessingResult.Complete;
    }

    public void OnPurchaseFailed(Product product, PurchaseFailureReason failureReason)
    {
        string arg = string.Format("OnPurchaseFailed: FAIL. Product: '{0}', PurchaseFailureReason: {1}", product.definition.storeSpecificId, failureReason);
        WarnDialog.showWarnDialog(arg);
        // A product purchase attempt did not succeed. Check failureReason for more detail. Consider sharing 
        // this reason with the user to guide their troubleshooting actions.
        Debug.Log(arg);
    }
}
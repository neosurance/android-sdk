package eu.neosurance.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NSRCallbackManagerImpl implements NSRCallbackManager{
    private static final String INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
    private Map<Integer, Callback> callbacks = new HashMap<>();
    private static Map<Integer, Callback> staticCallbacks = new HashMap<>();

//    public synchronized static void registerStaticCallback(int requestCode, Callback callback) {
//        if(callback == null){
//            throw new NullPointerException("Argument Callback cannot be null");
//        }
//        if (staticCallbacks.containsKey(requestCode)) {
//            return;
//        }
//        staticCallbacks.put(requestCode, callback);
//    }

//    private static synchronized Callback getStaticCallback(Integer requestCode) {
//        return staticCallbacks.get(requestCode);
//    }

//    private static boolean runStaticCallback(int requestCode, int resultCode, Intent data) {
//        Callback callback = getStaticCallback(requestCode);
//        if (callback != null) {
//            return callback.onActivityResult(resultCode, data);
//        }
//        return false;
//    }

    public  void registerCallback(int requestCode, Callback callback){
        if(callback == null){
            throw new NullPointerException("Argument Callback cannot be null");
        }
        if (callbacks.containsKey(requestCode)) {
            return;
        }
        callbacks.put(requestCode, callback);
    }

    public void unregisterCallback(int requestCode) {
        callbacks.remove(requestCode);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (isPurchaseIntent(data)) {
//            requestCode = RequestCodeOffset.InAppPurchase.toRequestCode();
//        }
        Callback callback = callbacks.get(requestCode);
        if (callback != null) {
            return callback.onActivityResult(resultCode, data);
        }
        return false;
        //return runStaticCallback(requestCode, resultCode, data);
    }

//    private static boolean isPurchaseIntent(Intent data) {
//        final String purchaseData;
//        if (data == null || (purchaseData = data.getStringExtra(INAPP_PURCHASE_DATA)) == null) {
//            return false;
//        }
//
//        try {
//            JSONObject o = new JSONObject(purchaseData);
//            return o.has("orderId") && o.has("packageName") && o.has("productId")
//                    && o.has("purchaseTime") && o.has("purchaseState")
//                    && o.has("developerPayload") && o.has("purchaseToken");
//        }
//        catch (JSONException e) {
//            Log.e(NSR.TAG, "Error parsing intent data.", e);
//        }
//
//        return false;
//    }

    public interface Callback {
        public boolean onActivityResult(int resultCode, Intent data);
    }
}

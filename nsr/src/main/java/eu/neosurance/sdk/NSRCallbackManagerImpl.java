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
        Callback callback = callbacks.get(requestCode);
        if (callback != null) {
            return callback.onActivityResult(resultCode, data);
        }
        return false;
        //return runStaticCallback(requestCode, resultCode, data);
    }

    public synchronized static void registerStaticCallback(int requestCode, Callback callback) {
        if(callback == null){
            throw new NullPointerException("Argument Callback cannot be null");
        }
        if (staticCallbacks.containsKey(requestCode)) {
            return;
        }
        staticCallbacks.put(requestCode, callback);
    }

    private static synchronized Callback getStaticCallback(Integer requestCode) {
        return staticCallbacks.get(requestCode);
    }

    private static boolean runStaticCallback(int requestCode, int resultCode, Intent data) {
        Callback callback = getStaticCallback(requestCode);
        if (callback != null) {
            return callback.onActivityResult(resultCode, data);
        }
        return false;
    }

    public interface Callback {
        public boolean onActivityResult(int resultCode, Intent data);
    }
}

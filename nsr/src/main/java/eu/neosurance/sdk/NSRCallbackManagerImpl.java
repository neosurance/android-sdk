package eu.neosurance.sdk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NSRCallbackManagerImpl implements NSRCallbackManager{
    private Map<Integer, Object> callbacks = new HashMap<>();
    private static Map<Integer, Object> staticCallbacks = new HashMap<>();


    public  void registerCallback(int requestCode, Object callback){
        if(callback == null){
            throw new NullPointerException("Argument Callback cannot be null");
        }
        //if (callbacks.containsKey(requestCode)) {
        //    return;
        //}
        callbacks.put(requestCode, callback);
    }

    public void unregisterCallback(int requestCode) {
        callbacks.remove(requestCode);
    }

    public synchronized static void registerStaticCallback(int requestCode, Object callback) {
        if(callback == null){
            throw new NullPointerException("Argument Callback cannot be null");
        }
        //if (staticCallbacks.containsKey(requestCode)) {
        //    return;
        //}
        staticCallbacks.put(requestCode, callback);
    }

    private static synchronized Object getStaticCallback(Integer requestCode) {
        return staticCallbacks.get(requestCode);
    }


    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        Callback callback = (Callback) callbacks.get(requestCode);
        if (callback != null) {
            return callback.onActivityResult(resultCode, data);
        }
        return runStaticCallback(requestCode, resultCode, data);
    }

    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        PermissionCallback permCallback = (PermissionCallback) callbacks.get(requestCode);
        if (permCallback != null) {
            return permCallback.onRequestPermissionsResult(permissions, grantResults);
        }
        return runStaticCallback(requestCode, permissions, grantResults);
    }

    private static boolean runStaticCallback(int requestCode, int resultCode, Intent data) {
        Callback callback = (Callback) getStaticCallback(requestCode);
        if (callback != null) {
            return callback.onActivityResult(resultCode, data);
        }
        return false;
    }

    private static boolean runStaticCallback(int requestCode, String[] permissions, int[] grantResults) {
        PermissionCallback permCallback = (PermissionCallback) getStaticCallback(requestCode);
        if (permCallback != null) {
            return permCallback.onRequestPermissionsResult(permissions, grantResults);
        }
        return false;
    }

    public interface Callback {
        public boolean onActivityResult(int resultCode, Intent data);
    }

    public interface PermissionCallback {
        public boolean onRequestPermissionsResult(String[] permissions, int[] grantResults);
    }
}

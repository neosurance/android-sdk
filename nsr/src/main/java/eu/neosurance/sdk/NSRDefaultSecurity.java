package eu.neosurance.sdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Iterator;

import cz.msebera.android.httpclient.Header;

public class NSRDefaultSecurity implements NSRSecurityDelegate {

    public void secureRequest(Context ctx, String endpoint, JSONObject payload, JSONObject headers, final NSRSecurityResponse completionHandler) throws Exception {

        final AsyncHttpClient client = new AsyncHttpClient();
        final String url = NSR.getInstance(ctx).getSettings().getString("base_url") + endpoint + (payload != null ? "?payload=" + URLEncoder.encode(payload.toString(), "UTF-8") : "");
        if(headers != null) {
            Iterator<String> keys = headers.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                client.addHeader(key, headers.getString(key));
            }
        }
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = new Runnable() {
            public void run() {
                client.get(url, new AsyncHttpResponseHandler() {
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                        try {
                            JSONObject json = new JSONObject(new String(response, "UTF-8"));
                            completionHandler.completionHandler(json, null);
                        } catch (Exception e) {
                            Log.e("nsr", e.getMessage(), e);
                        }
                    }

                    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable error) {
                        try {
                            completionHandler.completionHandler(null, error.toString());
                        } catch (Exception e) {
                        }
                    }
                });
            }
        };
        mainHandler.post(myRunnable);
    }

}

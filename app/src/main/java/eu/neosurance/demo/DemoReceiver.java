package eu.neosurance.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.clickntap.tap.web.TapWebView;

import org.json.JSONObject;

import eu.neosurance.sdk.NSR;
import eu.neosurance.sdk.NSRActivityWebView;

public class DemoReceiver {
    public static final String ACTION_DEMO_RECEIVER = "NSRDemoReceiver";
    private DemoActivity activity;
    private TapWebView webView;
    private String resultCallback;

    private BroadcastReceiver demoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                String service = intent.getExtras().getString("service");
                if ("base64Image".equals(service)) {
                    String base64Image = NSR.getInstance(activity).getData("base64Image", "");
                    webView.evaluateJavascript(resultCallback + "('" + base64Image + "')", null);
                    resultCallback = null;
                    NSR.getInstance(activity).setData("base64Image", null);
                }
            } catch (Exception e) {
                Log.d(NSR.TAG, e.getMessage());
            }
        }
    };

    public DemoReceiver(DemoActivity activity, final TapWebView webView) {
        this.activity = activity;
        this.webView = webView;
        activity.registerReceiver(demoReceiver, ACTION_DEMO_RECEIVER);
    }

    @JavascriptInterface
    public void postMessage(String json) {
        NSRActivityWebView.postMessage(activity, webView, json);
    }
}

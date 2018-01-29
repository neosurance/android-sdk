package eu.neosurance.app;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.clickntap.tap.AppActivity;
import com.clickntap.tap.web.TapWebView;

import org.json.JSONObject;


public class NSRActivityWebView extends AppActivity {
    private TapWebView webView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.nsr_activity_webview);
        try {
            registerReceiver(appjsReceiver, "appjs-notification");
            Bundle extras = getIntent().getExtras();
            JSONObject json = new JSONObject(extras.getString("json"));
            webView = (TapWebView) findViewById(R.id.webView);
            webView.add(getApp(), this, "NSSdk");
            webView.loadUrl(json.getString("url"));
        } catch (Exception e) {
            Log.e("nsr", e.getMessage(), e);
        }

    }

    private BroadcastReceiver appjsReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                final JSONObject body = new JSONObject(intent.getExtras().getString("json"));
                if (body.has("what")) {
                    if ("init".equals(body.getString("what"))) {
                        NSR.getInstance().token(new NSRToken() {
                            public void token(String token) throws Exception {
                                JSONObject settings = NSR.getInstance().getSettings();
                                JSONObject message = new JSONObject();
                                message.put("api", settings.getString("base_url"));
                                message.put("token", token);
                                message.put("lang", "it");
                                webView.evaluateJavascript(body.getString("callBack") + "(" + message.toString() + ")", null);
                            }
                        });
                    }
                    if ("close".equals(body.getString("what"))) {
                        finish();
                    }
                }
            } catch (Exception e) {
                Log.e("nsr", e.getMessage(), e);
            }
        }
    };
}

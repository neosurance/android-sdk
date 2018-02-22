package eu.neosurance.sdk;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.clickntap.tap.AppActivity;
import com.clickntap.tap.web.TapWebView;

import org.json.JSONObject;


public class NSRActivityWebView extends AppActivity {
    private TapWebView webView;
    private NSRCallbackManager callbackManager;
    private String resultCallback;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nsr_activity_webview);

        try {
            registerReceiver(appjsReceiver, "appjs-notification");
            Bundle extras = getIntent().getExtras();
            JSONObject json = new JSONObject(extras.getString("json"));
            webView = (TapWebView) findViewById(R.id.webView);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.setWebContentsDebuggingEnabled(true);
            }
            webView.add(getApp(), this, "NSSdk");
            webView.loadUrl(json.getString("url"));
            webView.setWebViewClient(new WebViewClient(){
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if(url.endsWith(".pdf")){
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(url), "application/pdf");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(intent);
                        return true;
                    }else{
                        return false;
                    }
                }
            });

            callbackManager = NSRCallbackManager.Factory.create();
            NSR.getInstance(this).registerCallback(callbackManager, new NSRBase64Image.Callback() {
                public void onSuccess(String base64Image) {
                    webView.evaluateJavascript(resultCallback+"('"+base64Image+"')",null);
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                }
                public void onCancel() {
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                }
                public void onError() {
                }
            });

            idle();
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
                        NSR.getInstance(NSRActivityWebView.this).token(new NSRToken() {
                            public void token(String token) throws Exception {
                                JSONObject settings = NSR.getInstance(NSRActivityWebView.this).getSettings();
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
                    }else if ("refresh".equals(body.getString("what"))) {
                        NSR.getInstance(NSRActivityWebView.this).resetAll();
                        finish();
                        startActivity(getIntent());
                    }else if ("photo".equals(body.getString("what"))) {
                        if(body.has("callBack")){
                            resultCallback = body.getString("callBack");
                            NSR.getInstance(NSRActivityWebView.this).takePicture();
                        }
                    }else if ("location".equals(body.getString("what"))) {
                        JSONObject location = NSR.getInstance(NSRActivityWebView.this).getCurrentLocation();
                        if(body.has("callBack") && location.has("latitude") && location.has("longitude")){
                            webView.evaluateJavascript(body.getString("callBack")+"("+location.toString()+")",null);
                        }
                    }else if ("code".equals(body.getString("what"))) {
                        if(body.has("callBack")) {
                            final String code = NSR.getInstance(NSRActivityWebView.this).getDemoSettings().getString("code");
                            webView.evaluateJavascript(body.getString("callBack")+"('"+code+"')",null);
                        }
                    }else if("showapp".equals(body.getString("what"))){
                        if(body.has("params")){
                            NSR.getInstance(NSRActivityWebView.this).showApp(body.getJSONObject("params"));
                        }else{
                            NSR.getInstance(NSRActivityWebView.this).showApp();
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(NSR.TAG, e.getMessage());
            }
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void idle(){
        new Handler().postDelayed(new Runnable() {
            public void run() {
                webView.evaluateJavascript("(function() { return (window.document.body.className.indexOf('NSR') == -1 ? false : true); })();", new ValueCallback<String>() {
                    public void onReceiveValue(String value) {
                        if("true".equals(value)){
                            idle();
                        }else{
                            finish();
                        }
                    }
                });
            }
        },15*1000);
    }
}

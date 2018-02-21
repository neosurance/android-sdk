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
                    }
                    if ("refresh".equals(body.getString("what"))) {
                        NSR.getInstance(NSRActivityWebView.this).resetAll();
                        finish();
                        startActivity(getIntent());
                    }
                    if ("photo".equals(body.getString("what"))) {
                        resultCallback = body.getString("callBack");
                        if(resultCallback != null){
                            NSR.getInstance(NSRActivityWebView.this).takePicture(NSRActivityWebView.this);
                        }
                    }
                    if ("location".equals(body.getString("what"))) {
                        JSONObject location = NSR.getInstance(NSRActivityWebView.this).getCurrentLocation();
                        if(body.has("callBack") && location.has("latitude") && location.has("longitude")){
                            webView.evaluateJavascript(body.getString("callBack")+"("+location.toString()+")",null);
                        }

                    }
                }
            } catch (Exception e) {
                Log.e("nsr", e.getMessage(), e);
            }
        }
    };

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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        NSR.getInstance(this).pictureProcessed(this, new NSRUtils.NSRPictureProcessed() {
            public void onStart(){
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            }
            public void onSuccess(String base64){
                webView.evaluateJavascript(resultCallback+"('"+base64+"')",null);
                findViewById(R.id.progressBar).setVisibility(View.GONE);
                resultCallback = null;
            }
        }, requestCode, resultCode);
    }

}

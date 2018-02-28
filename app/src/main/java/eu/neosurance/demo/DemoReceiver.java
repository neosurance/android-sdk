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

public class DemoReceiver {
    public static final String ACTION_DEMO_RECEIVER = "NSRDemoReceiver";
    private DemoActivity activity;
    private TapWebView webView;
    private String resultCallback;

    private BroadcastReceiver demoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                String service = intent.getExtras().getString("service");
                if("base64Image".equals(service)){
                    String base64Image = NSR.getInstance(activity).getData("base64Image", "");
                    webView.evaluateJavascript(resultCallback+"('"+base64Image+"')",null);
                    resultCallback = null;
                    NSR.getInstance(activity).setData("base64Image", null);
                }

            } catch (Exception e) {
                Log.d(NSR.TAG, e.getMessage());
            }
        }
    };

    public DemoReceiver(DemoActivity activity, final TapWebView webView){
        this.activity = activity;
        this.webView = webView;
        activity.registerReceiver(demoReceiver, ACTION_DEMO_RECEIVER);
    }

    @JavascriptInterface
    public void postMessage(String json) {
        try {
            final JSONObject body = new JSONObject(json);
            if (!body.has("what")) {
                NSR.getInstance(activity).sendCustomEvent(body.getString("event"), body.getJSONObject("payload"));
            }else{
                if ("refresh".equals(body.getString("what"))) {
                    NSR.getInstance(activity).resetAll();
                    activity.finish();
                    activity.startActivity(activity.getIntent());
                }if ("photo".equals(body.getString("what"))) {
                    if(body.has("callBack")) {
                        resultCallback = body.getString("callBack");
                        activity.takePhotoRequestPermissions();
                    }
                }else if ("location".equals(body.getString("what"))) {
                    JSONObject location = NSR.getInstance(activity).getCurrentLocation();
                    if(body.has("callBack") && location.has("latitude") && location.has("longitude")){
                        eval(body.getString("callBack")+"("+location.toString()+")");
                    }
                }else if ("code".equals(body.getString("what"))) {
                    if(body.has("callBack")) {
                        final String code = NSR.getInstance(activity).getUser().getCode();
                        eval(body.getString("callBack")+"('"+code+"')");
                    }
                }else if("showapp".equals(body.getString("what"))){
                    if(body.has("params")){
                        NSR.getInstance(activity).showApp(body.getJSONObject("params"));
                    }else{
                        NSR.getInstance(activity).showApp();
                    }
                }
            }
        } catch (Exception e) {
            Log.d(NSR.TAG, e.getMessage());
        }
    }

    private void eval(final String code){
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = new Runnable() {
            public void run() {
                try{
                    webView.evaluateJavascript(code,null);
                }catch(Exception e){
                    Log.d(NSR.TAG, e.getMessage(), e);
                }
            }
        };
        mainHandler.post(myRunnable);
    }
}

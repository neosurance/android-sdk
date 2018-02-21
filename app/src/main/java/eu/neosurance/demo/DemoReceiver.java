package eu.neosurance.demo;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import com.clickntap.tap.web.TapWebView;
import org.json.JSONObject;
import eu.neosurance.sdk.NSR;

public class DemoReceiver {
    private DemoActivity activity;
    private TapWebView webView;

    public DemoReceiver(DemoActivity activity, TapWebView webView){
        this.activity = activity;
        this.webView = webView;
    }

    @JavascriptInterface
    public void postMessage(String json) {
        try {
            final JSONObject body = new JSONObject(json);
            if (!body.has("what")) {
                NSR.getInstance(activity).sendCustomEvent(body.getString("event"), body.getJSONObject("payload"));
            }else{
                if ("refresh".equals(body.getString("what"))) {
                    activity.refreshApp();
                }else if ("photo".equals(body.getString("what"))) {
                    activity.takePhoto(body);
                }else if ("location".equals(body.getString("what"))) {
                    JSONObject location = NSR.getInstance(activity).getCurrentLocation();
                    if(body.has("callBack") && location.has("latitude") && location.has("longitude")){
                        eval(body.getString("callBack")+"("+location.toString()+")");
                    }
                }else if ("code".equals(body.getString("what"))) {
                    if(body.has("callBack")) {
                        final String code = NSR.getInstance(activity).getDemoSettings().getString("code");
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
            Log.d(NSR.TAG, e.getMessage(), e);
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

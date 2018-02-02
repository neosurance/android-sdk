package eu.neosurance.app;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class NSR {
    private static final String PREFS_NAME = "NSRSDK";
    private static NSR instance = null;
    private JSONObject settings = null;
    private JSONObject demoSettings = null;
    private JSONObject authSettings = null;
    private NSRUser user = null;
    private Context ctx = null;

    private NSR() {

    }

    public JSONObject getSettings() {
        return settings;
    }

    public JSONObject getAuthSettings() {
        return authSettings;
    }

    public NSRUser getUser() {
        return user;
    }

    public void setUser(NSRUser user) {
        this.user = user;
    }

    public void token(final NSRToken delegate) throws Exception {
        authorize(new NSRAuth() {
            public void authorized(boolean authorized) throws Exception {
                if(authorized) {
                    delegate.token(authSettings.getJSONObject("auth").getString("token"));
                } else {
                    delegate.token(null);
                }
            }
        });
   }

    public void authorize(final NSRAuth delegate) throws Exception {
        this.authSettings = new JSONObject(getData("authSettings", "{}"));
        int remainingSeconds = NSRUtils.tokenRemainingSeconds(this.authSettings);
        if(remainingSeconds > 0) {
            delegate.authorized(true);
        } else {
            try {
                JSONObject payload = new JSONObject();
                payload.put("user_code",getUser().getCode());
                payload.put("code", settings.getString("code"));
                payload.put("secret_key", settings.getString("secret_key"));
                JSONObject sdkPayload = new JSONObject();
                sdkPayload.put("version", getVersion());
                sdkPayload.put("dev", settings.getString("dev_mode"));
                sdkPayload.put("os", getOs());
                payload.put("sdk", sdkPayload);
                AsyncHttpClient client = new AsyncHttpClient();
                String url = settings.getString("base_url")+"authorize?payload="+ URLEncoder.encode(payload.toString(), "UTF-8");
                client.get(url, new AsyncHttpResponseHandler() {
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                        try {
                            authSettings = new JSONObject(new String(response, "UTF-8"));
                            setData("authSettings", authSettings.toString());
                            int remainingSeconds = NSRUtils.tokenRemainingSeconds(authSettings);
                            delegate.authorized(remainingSeconds > 0);
                        } catch (Exception e) {
                        }
                    }
                    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    }
                });
            } catch(Exception e) {
                Log.e("nsr", e.getMessage(), e);
                delegate.authorized(false);
            }
        }
    }

    public String getOs() {
        return "Android";
    }

    public String getVersion() {
        return "1.0";
    }

    public static NSR getInstance() {
        if(instance == null) {
            instance = new NSR();
        }
        return instance;
    }

    public JSONObject getDemoSettings() {
        return demoSettings;
    }

    public void setup(final JSONObject settings, final Context ctx) throws Exception {
        this.settings = settings;
        this.ctx = ctx;
        this.settings.put("ns_lang", Locale.getDefault().getDisplayLanguage());
        if(!this.settings.has("dev_mode")) {
            this.settings.put("dev_mode", 0);
        }
        if(settings.has("base_demo_url")) {
            String demoCode = getData("demo_code", "");
            final String url = settings.getString("base_demo_url")+demoCode;
            Log.d("nsr", url);

            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = new Runnable() {
                public void run() {
                    AsyncHttpClient client = new AsyncHttpClient();
                    client.get(url, new AsyncHttpResponseHandler() {
                        public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                            try {
                                demoSettings = new JSONObject(new String(response, "UTF-8"));
                                Intent intent = new Intent();
                                intent.setAction("NSRIncomingDemoSettings");
                                intent.putExtra("json", demoSettings.toString());
                                ctx.sendBroadcast(intent);
                                Log.d("nsr", demoSettings.toString());
                                setData("demo_code", demoSettings.getString("code"));
                                JSONObject configuration = new JSONObject();
                                configuration.put("base_url", settings.getString("base_url"));
                                configuration.put("secret_key", demoSettings.getString("secretKey"));
                                configuration.put("code", demoSettings.getString("communityCode"));
                                configuration.put("dev_mode", demoSettings.getString("devMode"));
                                setup(configuration, ctx);
                            } catch (Exception e) {
                            }
                        }
                        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                        }
                    });

                }
            };
            mainHandler.post(myRunnable);


        } else {
            NSRUser user = new NSRUser();
            user.setEmail(demoSettings.getString("email"));
            user.setCode(demoSettings.getString("code"));
            user.setFirstname(demoSettings.getString("firstname"));
            user.setLastname(demoSettings.getString("lastname"));
            setUser(user);
            authorize(new NSRAuth() {
                 public void authorized(boolean authorized) throws Exception {
                }
            });
           }
    }

    public String getData(String key, String defVal) {
        return getSharedPreferences().getString(key, defVal);
    }

    private SharedPreferences getSharedPreferences() {
        return ctx.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE);
    }

    public void setData(String key, String value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        if(value != null) {
            editor.putString(key, value);
        } else {
            editor.remove(key);
        }
        editor.commit();
    }

    public void showApp(Context context) throws Exception {
        Intent intent = new Intent(context, NSRActivityWebView.class);
        JSONObject json = new JSONObject();
        JSONObject settings = NSR.getInstance().getAuthSettings();
        json.put("url", settings.getString("app_url"));
        intent.putExtra("json", json.toString());
        context.startActivity(intent);
    }

    public void  sendCustomEvent(Context context, String name, JSONObject payload) throws Exception {
        NSRRequest request = new NSRRequest(NSRUtils.makeEvent(name, payload));
        request.send(context);
    }
}

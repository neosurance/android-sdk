package eu.neosurance.sdk;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;

import cz.msebera.android.httpclient.Header;

public class NSRRequest {

    private final JSONObject event;

    public NSRRequest(JSONObject event) {
        this.event = event;
    }

    public void send(final Context ctx) throws Exception {
        NSR.getInstance(ctx).token(new NSRToken() {
            public void token(String token) throws Exception {
                NSRUser user = NSR.getInstance(ctx).getUser();
                JSONObject payload = new JSONObject();
                JSONObject userPayload = new JSONObject();
                userPayload.put("firstname", user.getFirstname());
                userPayload.put("lastname", user.getLastname());
                userPayload.put("email", user.getEmail());
                userPayload.put("code", user.getCode());
                JSONObject devicePayLoad = new JSONObject();
                devicePayLoad.put("uid", Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID));
                devicePayLoad.put("os", NSR.getInstance(ctx).getOs());
                devicePayLoad.put("version", Build.VERSION.RELEASE + " " + Build.VERSION_CODES.class.getFields()[android.os.Build.VERSION.SDK_INT].getName());
                devicePayLoad.put("model", Build.MODEL);
                payload.put("event", event);
                payload.put("user", userPayload);
                payload.put("device", devicePayLoad);
                final JSONObject settings = NSR.getInstance(ctx).getSettings();
                final AsyncHttpClient client = new AsyncHttpClient();
                client.addHeader("ns_token", token);
                client.addHeader("ns_lang", settings.getString("ns_lang"));
                final String url = settings.getString("base_url") + "event?payload=" + URLEncoder.encode(payload.toString(), "UTF-8");
                //Log.d("nsr", event.toString());
                //Log.d("nsr", url);
                Log.d("nsr", "send event: "+event.getString("event"));
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = new Runnable() {
                    public void run() {
                        client.get(url, new AsyncHttpResponseHandler() {
                            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                try {
                                    JSONObject json = new JSONObject(new String(response, "UTF-8"));
                                    if ("ok".equals(json.getString("status"))) {
                                        boolean hasPushes = json.has("pushes");
                                        if (hasPushes) {
                                            boolean skipPush = !json.has("skipPush") || json.getBoolean("skipPush");
                                            if (!skipPush) {
                                                MediaPlayer.create(ctx, R.raw.push).start();
                                                JSONArray pushes = json.getJSONArray("pushes");
                                                for (int i = 0; i < pushes.length(); i++) {
                                                    JSONObject notification = pushes.getJSONObject(i);
                                                    //Log.d("nsr", notification.toString());
                                                    String title = notification.getString("title");
                                                    String body = notification.getString("body");
                                                    String imageUrl = notification.has("imageUrl") && !"".equals(notification.getString("imageUrl")) ? notification.getString("imageUrl") : "";
                                                    String url = notification.has("url") && !"".equals(notification.getString("url")) ? notification.getString("url") : "";
                                                    if (!"".equals(url) && !"".equals(imageUrl) || !"".equals(url)) {
                                                        Intent intent = new Intent(ctx, NSRActivityWebView.class);
                                                        intent.putExtra("json", notification.toString());
                                                        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                                        if (!"".equals(imageUrl)) {
                                                            NSRNotification.sendNotification(ctx, title, body, imageUrl, pendingIntent);
                                                        } else {
                                                            NSRNotification.sendNotification(ctx, title, body, pendingIntent);
                                                        }
                                                    } else {
                                                        NSRNotification.sendNotification(ctx, title, body);
                                                    }
                                                    break;
                                                }
                                            } else {
                                                JSONArray pushes = json.getJSONArray("pushes");
                                                for (int i = 0; i < pushes.length(); i++) {
                                                    JSONObject notification = pushes.getJSONObject(i);
                                                    Log.d("nsr", notification.toString());
                                                    Intent intent = new Intent(ctx, NSRActivityWebView.class);
                                                    JSONObject params = new JSONObject();
                                                    params.put("url", notification.getString("url"));
                                                    intent.putExtra("json", params.toString());
                                                    ctx.startActivity(intent);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.d("nsr", e.getMessage(), e);
                                }
                            }

                            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                                try {
                                    Log.d("nsr", new String(errorResponse, "UTF-8"));
                                } catch (Exception e1) {
                                }
                                Log.d("nsr", e.getMessage(), e);
                            }
                        });

                    }
                };
                mainHandler.post(myRunnable);
            }

        });
    }
}

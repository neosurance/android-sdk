package eu.neosurance.sdk;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;


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
                JSONObject devicePayLoad = new JSONObject();
                devicePayLoad.put("uid", Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID));
                devicePayLoad.put("os", NSR.getInstance(ctx).getOs());
                devicePayLoad.put("version", Build.VERSION.RELEASE + " " + Build.VERSION_CODES.class.getFields()[android.os.Build.VERSION.SDK_INT].getName());
                devicePayLoad.put("model", Build.MODEL);
                payload.put("event", event);
                payload.put("user", user.toJsonObject());
                payload.put("device", devicePayLoad);

                JSONObject headers = new JSONObject();
                headers.put("ns_token", token);
                headers.put("ns_lang", NSR.getInstance(ctx).getSettings().getString("ns_lang"));

                NSR.getInstance(ctx).getSecurityDelegate().secureRequest(ctx, "event", payload, headers, new NSRSecurityResponse() {
                    public void completionHandler(JSONObject json, String error) throws Exception {
                        try {
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
                            Log.e("nsr", error, e);
                        }
                    }
                });
            }
        });
    }
}
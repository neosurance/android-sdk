package eu.neosurance.sdk;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.clickntap.tap.web.TapWebView;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import id.zelory.compressor.Compressor;

public class NSR {
    public static final int REQUEST_IMAGE_CAPTURE = 0x1256;
    public static final String FILENAME_IMAGE_CAPTURE = "nsr-photo.jpg";
    public static final int PERMISSIONS_MULTIPLE_REQUEST = 123;
    public static final int PERMISSIONS_MULTIPLE_IMAGECAPTURE = 0x1616;
    //public static final int UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    //public static final int FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    public static final int REQUEST_CHECK_SETTINGS = 0x1;

    public static final String TAG = "nsr";
    private static final String PREFS_NAME = "NSRSDK";
    private static NSR instance = null;
    private JSONObject settings = null;
    private JSONObject demoSettings = null;
    private JSONObject authSettings = null;
    private NSRUser user = null;
    private Context ctx = null;
    private JSONObject currentLocation = null;
    private JSONObject lastLocation = null;
    private boolean stillPositionSent = false;

    private NSR(Context ctx) {
        this.ctx = ctx;
    }

    public static NSR getInstance(Context ctx) {
        if(instance == null) {
            instance = new NSR(ctx);
        }
        return instance;
    }

    public JSONObject getCurrentLocation() {
        try {
            if (currentLocation == null) {
                currentLocation = new JSONObject(getData("currentLocation", "{}"));
            }
            return currentLocation;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    protected void setCurrentLocation(JSONObject currentLocation){
        this.currentLocation = currentLocation;
        setData("currentLocation", currentLocation == null ? "" : currentLocation.toString());
    }

    public JSONObject getLastLocation() {
        try {
            if (lastLocation == null) {
                lastLocation = new JSONObject(getData("lastLocation", "{}"));
            }
            return lastLocation;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    protected void setLastLocation(JSONObject lastLocation){
        this.lastLocation = lastLocation;
        setData("lastLocation", lastLocation == null ? "" : lastLocation.toString());
    }

    public String getOs() {
        return "Android";
    }

    public String getVersion() {
        return "1.0";
    }

    public JSONObject getSettings() {
        try {
            if (settings == null) {
                settings = new JSONObject(getData("settings", "{}"));
            }
            return settings;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    protected void setSettings(JSONObject settings){
        this.settings = settings;
        setData("settings", settings == null ? "" : settings.toString());
    }

    public JSONObject getDemoSettings() {
        try{
            if(demoSettings == null){
                demoSettings = new JSONObject(getData("demoSettings", "{}"));
            }
            return demoSettings;
        }catch(Exception e){
            return new JSONObject();
        }
    }

    protected void setDemoSettings(JSONObject demoSettings){
        this.demoSettings = demoSettings;
        setData("demoSettings", demoSettings == null ? "" : demoSettings.toString());
    }

    public JSONObject getAuthSettings() {
        try{
            if(authSettings == null){
                authSettings = new JSONObject(getData("authSettings", "{}"));
            }
            return authSettings;
        }catch(Exception e){
            return new JSONObject();
        }
    }

    protected void setAuthSettings(JSONObject authSettings){
        this.authSettings = authSettings;
        setData("authSettings", authSettings == null ? "" : authSettings.toString());
    }


    public boolean getStillPositionSent(){
        if(!stillPositionSent){
            stillPositionSent = Boolean.parseBoolean(getData("stillPositionSent", "false"));
        }
        return stillPositionSent;
    }

    public void setStillPositionSent(boolean stillPositionSent){
        this.stillPositionSent = stillPositionSent;
        setData("stillPositionSent",  ""+stillPositionSent);
    }

    public NSRUser getUser() {
        try{
            if(user == null){
                user = new Gson().fromJson(getData("user", "{}"), NSRUser.class);
            }
            return user;
        }catch(Exception e){
            return new NSRUser();
        }
    }

    protected void setUser(NSRUser user){
        this.user = user;
        setData("user", user == null ? "" : new Gson().toJson(user));
    }

    public void token(final NSRToken delegate){
        try{
            authorize(new NSRAuth() {
                public void authorized(boolean authorized) throws Exception {
                    if(authorized) {
                        delegate.token(getAuthSettings().getJSONObject("auth").getString("token"));
                    } else {
                        delegate.token(null);
                    }
                }
            });
        }catch(Exception e){
        }
    }

    public void authorize(final NSRAuth delegate) throws Exception {
        int remainingSeconds = NSRUtils.tokenRemainingSeconds(getAuthSettings());
        if(remainingSeconds > 0) {
            delegate.authorized(true);
        }else{
            try{
                JSONObject payload = new JSONObject();
                payload.put("user_code", getUser().getCode());
                payload.put("code", getSettings().getString("code"));
                payload.put("secret_key", getSettings().getString("secret_key"));

                JSONObject sdkPayload = new JSONObject();
                sdkPayload.put("version", getVersion());
                sdkPayload.put("dev", getSettings().getString("dev_mode"));
                sdkPayload.put("os", getOs());
                payload.put("sdk", sdkPayload);

                final AsyncHttpClient client = new AsyncHttpClient();
                final String url = getSettings().getString("base_url")+"authorize?payload="+ URLEncoder.encode(payload.toString(), "UTF-8");
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = new Runnable() {
                    public void run(){

                        client.get(url, new AsyncHttpResponseHandler() {
                            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                try {
                                    setAuthSettings(new JSONObject(new String(response, "UTF-8")));
                                    int remainingSeconds = NSRUtils.tokenRemainingSeconds(getAuthSettings());
                                    delegate.authorized(remainingSeconds > 0);
                                } catch(Exception e) {
                                    Log.e("nsr", e.getMessage(), e);
                                }
                            }
                            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            }
                        });
                    }
                };
                mainHandler.post(myRunnable);
            }catch(Exception e){
                delegate.authorized(false);
                Log.e("nsr", e.getMessage(), e);
            }
        }
    }

    public void setup(JSONObject settings){
        try{
            if(!settings.has("ns_lang")) {
                settings.put("ns_lang", Locale.getDefault().getDisplayLanguage());
            }
            if(!settings.has("dev_mode")) {
                settings.put("dev_mode", 0);
            }
            setStillPositionSent(false);
            setAuthSettings(null);
            setUser(null);
            setSettings(settings);
            if(settings.has("base_demo_url")) {
                JSONObject demoSettings = getDemoSettings();
                String demoCode = demoSettings.has("code") ? demoSettings.getString("code") : "";
                final String url = getSettings().getString("base_demo_url") + demoCode;
                Log.d("nsr", "url "+url);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = new Runnable() {
                    public void run() {
                        AsyncHttpClient client = new AsyncHttpClient();
                        client.get(url, new AsyncHttpResponseHandler() {
                            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                try {
                                    JSONObject demoSettings = new JSONObject(new String(response, "UTF-8"));

                                    Intent intent = new Intent();
                                    intent.setAction("NSRIncomingDemoSettings");
                                    intent.putExtra("json", demoSettings.toString());
                                    ctx.sendBroadcast(intent);
                                    setDemoSettings(demoSettings);

                                    JSONObject appSettings = new JSONObject();
                                    appSettings.put("base_url", getSettings().getString("base_url"));
                                    appSettings.put("secret_key", getDemoSettings().getString("secretKey"));
                                    appSettings.put("code", getDemoSettings().getString("communityCode"));
                                    appSettings.put("dev_mode", getDemoSettings().getString("devMode"));
                                    setup(appSettings);

                                    NSRUser user = new NSRUser();
                                    user.setEmail(getDemoSettings().getString("email"));
                                    user.setCode(getDemoSettings().getString("code"));
                                    user.setFirstname(getDemoSettings().getString("firstname"));
                                    user.setLastname(getDemoSettings().getString("lastname"));
                                    registerUser(user);
                                } catch (Exception e) {
                                    Log.e("nsr", e.getMessage(), e);
                                }
                            }
                            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            }
                        });

                    }
                };
                mainHandler.post(myRunnable);
            }
        }catch(Exception e){
            Log.e("nsr", e.getMessage(), e);
        }
    }

    public void registerUser(NSRUser user){
        try{
            setUser(user);
            authorize(new NSRAuth() {
                public void authorized(boolean authorized) throws Exception {
                    try{
                        ctx.startService(new Intent(ctx, NSRService.class));// run now

                        JSONObject conf = getAuthSettings().getJSONObject("conf");
                        final PendingIntent pIntent = PendingIntent.getBroadcast(ctx, NSRSync.REQUEST_CODE, new Intent(ctx, NSRSync.class), PendingIntent.FLAG_UPDATE_CURRENT);
                        AlarmManager alarm = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),conf.getInt("time")*1000, pIntent);
                    }catch(Exception e){
                        Log.e("nsr", e.getMessage(), e);
                    }
                }
            });
        }catch(Exception e){
            Log.d("nsr", e.getMessage(), e);
        }
    }

    public void showApp() throws Exception {
        showApp(null);
    }

    public void showApp(Map<String, String> params) throws Exception {
        String url = getAuthSettings().getString("app_url")+"?";
        if(params != null && params.size() > 0){
            for(Map.Entry<String, String> entry :  params.entrySet()){
                url += entry.getKey()+"="+URLEncoder.encode(entry.getValue(), "UTF-8")+"&";
            }
            url = url.substring(0, url.length()-1);
        }
        Intent intent = new Intent(ctx, NSRActivityWebView.class);
        JSONObject json = new JSONObject();
        json.put("url", url);
        intent.putExtra("json", json.toString());
        ctx.startActivity(intent);
    }

    public void takePicture(Activity activity, int requestCode){
        if(requestCode == NSR.PERMISSIONS_MULTIPLE_IMAGECAPTURE){
            takePicture(activity);
        }
    }

    public void takePicture(final Activity activity){
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog alertDialog = new AlertDialog.Builder(ctx).create();
                alertDialog.setTitle("Camera Permissions");
                alertDialog.setCancelable(false);
                alertDialog.setMessage("Please Grant Permissions");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Allow",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final List<String> permissionsList = new ArrayList<String>();
                                NSRUtils.addPermission(ctx, permissionsList, Manifest.permission.CAMERA);
                                NSRUtils.addPermission(ctx, permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                                if (permissionsList.size() > 0) {
                                    ActivityCompat.requestPermissions(activity, permissionsList.toArray(new String[permissionsList.size()]), PERMISSIONS_MULTIPLE_IMAGECAPTURE);
                                }
                            }
                        });
                alertDialog.show();
            } else {
                final List<String> permissionsList = new ArrayList<String>();
                NSRUtils.addPermission(ctx, permissionsList, Manifest.permission.CAMERA);
                NSRUtils.addPermission(ctx, permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permissionsList.size() > 0) {
                    ActivityCompat.requestPermissions(activity, permissionsList.toArray(new String[permissionsList.size()]), PERMISSIONS_MULTIPLE_IMAGECAPTURE);
                }
            }
        } else{
            try{
                Intent mIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (mIntent.resolveActivity(ctx.getPackageManager()) != null) {
                    mIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(ctx, ctx.getPackageName()+".provider", getFileTemp()));
                    activity.startActivityForResult(mIntent, REQUEST_IMAGE_CAPTURE);
                }
            }catch(Exception e){
                Log.d(NSR.TAG, e.toString());
            }
        }
    }

    public File getFileTemp(){
        final File path = new File(Environment.getExternalStorageDirectory(), ctx.getPackageName());
        if(!path.exists()){
            path.mkdir();
        }
        return new File(path, FILENAME_IMAGE_CAPTURE);
    }

    public void pictureProcessed(Activity activity, NSRUtils.NSRPictureProcessed pictureProcessed, int requestCode, int resultCode) {
        if(requestCode == NSR.REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK){
            pictureProcessed(activity, pictureProcessed);
        }
    }

    public void pictureProcessed(final Activity activity, final NSRUtils.NSRPictureProcessed pictureProcessed){
        pictureProcessed.onStart();
        new Thread(new Runnable() {
            public void run() {
                try {
                    final String imageProcessed = ctx.getCacheDir().getAbsolutePath()+"/"+FILENAME_IMAGE_CAPTURE;
                    new Compressor(ctx)
                            .setMaxWidth(512)
                            .setMaxHeight(512)
                            .setQuality(60)
                            .setCompressFormat(Bitmap.CompressFormat.JPEG)
                            .setDestinationDirectoryPath(ctx.getCacheDir().getAbsolutePath())
                            .compressToFile(getFileTemp());

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap bitmap = BitmapFactory.decodeFile(imageProcessed, options);;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                    final String imageBase64 = "data:image/jpeg;base64,"+encodedImage;
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            pictureProcessed.onSuccess(imageBase64);
                        }
                    });

                } catch (Exception e) {
                }
            }
        }).start();
    }

    public void resetAll() throws Exception {
        setAuthSettings(null);
    }

    public void  sendCustomEvent(String name, JSONObject payload) throws Exception {
        NSRRequest request = new NSRRequest(NSRUtils.makeEvent(name, payload));
        request.send(ctx);
    }

    public String getData(String key, String defVal) {
        return getSharedPreferences().getString(key, defVal);
    }

    public SharedPreferences getSharedPreferences() {
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
}

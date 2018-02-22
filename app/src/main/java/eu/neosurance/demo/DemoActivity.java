package eu.neosurance.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.clickntap.tap.AppActivity;
import com.clickntap.tap.web.TapWebView;
import com.clickntap.tap.web.TapWebViewDelegate;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.neosurance.sdk.*;

public class DemoActivity extends CustomDemoActivity{
    private NSRCallbackManager callbackManager;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        registerReceiver(settingsReceiver, "NSRIncomingDemoSettings");

        loadUi();
        initializeGPS();
        initializeSpeech();
        initializeMap();
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    public void initializeSDK(){
        try {
            JSONObject configuration = new JSONObject();
            configuration.put("base_url", "https://sandbox.neosurancecloud.net/sdk/api/v1.0/");
            configuration.put("base_demo_url", "https://sandbox.neosurancecloud.net/demo/conf?code=");
            NSR.getInstance(this).setup(configuration);

            callbackManager = NSRCallbackManager.Factory.create();
            NSR.getInstance(this).registerCallback(callbackManager, new NSRBase64Image.Callback() {
                public void onSuccess(String base64Image) {
                    NSR.getInstance(DemoActivity.this).setData("base64Image", base64Image);
                    Intent intent = new Intent();
                    intent.setAction(DemoReceiver.ACTION_DEMO_RECEIVER);
                    intent.putExtra("service", "base64Image");
                    sendBroadcast(intent);
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                }
                public void onCancel() {
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    Log.d(NSR.TAG, "onCancel");
                }
                public void onError() {
                    Log.d(NSR.TAG, "onError");
                }
            });
        } catch (Exception e) {
            Log.d(NSR.TAG, e.getMessage(), e);
        }
    }

    protected void initializeGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Access Location Permissions");
                alertDialog.setCancelable(false);
                alertDialog.setMessage("Please Grant Permissions for access location");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Allow",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final List<String> permissionsList = new ArrayList<String>();
                                NSRUtils.addPermission(DemoActivity.this, permissionsList, Manifest.permission.ACCESS_FINE_LOCATION);
                                NSRUtils.addPermission(DemoActivity.this, permissionsList, Manifest.permission.ACCESS_COARSE_LOCATION);
                                if (permissionsList.size() > 0) {
                                    requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), PERMISSIONS_MULTIPLE_ACCESSLOCATION);
                                }
                            }
                        });
                alertDialog.show();

            } else {
                final List<String> permissionsList = new ArrayList<String>();
                NSRUtils.addPermission(this, permissionsList, Manifest.permission.ACCESS_FINE_LOCATION);
                NSRUtils.addPermission(this, permissionsList, Manifest.permission.ACCESS_COARSE_LOCATION);
                if (permissionsList.size() > 0) {
                    requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), PERMISSIONS_MULTIPLE_ACCESSLOCATION);
                }
            }
        }else{
            initializeSDK();
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        public void onSuccess(Location location) {
                            if (location != null) {
                                Log.d(NSR.TAG, "Ottenuta l'ultima posizione GPS");
                                onLocationChanged(location);
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                public void onFailure(Exception e) {
                    Log.d(NSR.TAG, "Errore durante il tentativo di ottenere l'ultima posizione GPS");
                }
            });
        }
    }

    public void takePhotoRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Camera Permissions");
                alertDialog.setCancelable(false);
                alertDialog.setMessage("Please Grant Permissions");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Allow", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        final List<String> permissionsList = new ArrayList<String>();
                        NSRUtils.addPermission(DemoActivity.this, permissionsList, Manifest.permission.CAMERA);
                        NSRUtils.addPermission(DemoActivity.this, permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (permissionsList.size() > 0) {
                            ActivityCompat.requestPermissions(DemoActivity.this, permissionsList.toArray(new String[permissionsList.size()]), PERMISSIONS_MULTIPLE_IMAGECAPTURE);
                        }
                    }
                });
                alertDialog.show();
            } else {
                final List<String> permissionsList = new ArrayList<String>();
                NSRUtils.addPermission(this, permissionsList, Manifest.permission.CAMERA);
                NSRUtils.addPermission(this, permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permissionsList.size() > 0) {
                    ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]), PERMISSIONS_MULTIPLE_IMAGECAPTURE);
                }
            }
        } else {
            NSR.getInstance(this).takePicture();
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(requestCode == PERMISSIONS_MULTIPLE_ACCESSLOCATION){
            initializeGPS();
        }
        if(requestCode == PERMISSIONS_MULTIPLE_IMAGECAPTURE){
            takePhotoRequestPermissions();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                JSONObject settings = new JSONObject(intent.getExtras().getString("json"));
                if(!settings.has("hideWheel") || settings.getInt("hideWheel") == 0){
                    findViewById(R.id.menuButtonFrame).setVisibility(View.VISIBLE);
                }
                webView = (TapWebView) findViewById(R.id.webView);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.setWebContentsDebuggingEnabled(true);
                }
                webView.loadUrl(settings.getString("url"));
                webView.addJavascriptInterface(new DemoReceiver(DemoActivity.this, webView), "NSSdk");
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
            } catch (JSONException e) {
                Log.e(NSR.TAG, e.getMessage(), e);
            }
        }
    };


}

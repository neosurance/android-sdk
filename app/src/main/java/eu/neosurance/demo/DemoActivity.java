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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import eu.neosurance.sdk.*;

public class DemoActivity extends AppActivity implements View.OnTouchListener {
    private static final int REQUEST_IMAGE_CAPTURE = 0x1256;
    private static final String FILENAME_IMAGE_CAPTURE = "nsr-photo.jpg";
    private static final int PERMISSIONS_MULTIPLE_REQUEST = 123;
    private static final int PERMISSIONS_MULTIPLE_IMAGECAPTURE = 0x1616;
    //private static final int UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    //private static final int FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    private TapWebView webView;
    private TapWebView demoWebView;
    private int xDelta;
    private int yDelta;
    private int downX;
    private int downY;
    private boolean menuOpened;
    private boolean uiOpened;
    private boolean uiMapOpened;

    private MediaPlayer btnSound;
    private MediaPlayer pushSound;
    private MediaPlayer expandSound;
    private MediaPlayer collapseSound;

    private TextToSpeech tts;
    private GoogleMap googleMap;
    private double latitude = -1;
    private double longitude = -1;

    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private String resultCallback = null;

    public void playPushSound() {
        pushSound.seekTo(0);
        pushSound.start();
    }

    private void playBtnSound() {
        if (btnSound != null) {
            btnSound.seekTo(0);
            btnSound.start();
        }
    }

    public void playExpandSound() {
        if (expandSound != null) {
            expandSound.seekTo(0);
            expandSound.start();
        }
    }

    public void playCollapseSound() {
        if (collapseSound != null) {
            collapseSound.seekTo(0);
            collapseSound.start();
        }
    }

    private BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                JSONObject settings = new JSONObject(intent.getExtras().getString("json"));
                webView = (TapWebView) findViewById(R.id.webView);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.setWebContentsDebuggingEnabled(true);
                }
                webView.loadUrl(settings.getString("url"));
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
                Log.e("nsr", e.getMessage(), e);
            }
        }
    };

    private BroadcastReceiver appjsReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                JSONObject body = new JSONObject(intent.getExtras().getString("json"));
                if (!body.has("what")) {
                    NSR.getInstance(DemoActivity.this).sendCustomEvent(body.getString("event"), body.getJSONObject("payload"));
                }else{
                    if ("refresh".equals(body.getString("what"))) {
                        NSR.getInstance(DemoActivity.this).resetAll();
                        finish();
                        startActivity(getIntent());
                    }else if ("photo".equals(body.getString("what"))) {
                        resultCallback = body.getString("callback");
                        if(resultCallback != null){
                            NSR.getInstance(DemoActivity.this).takePicture(DemoActivity.this);
                        }
                    }else if ("location".equals(body.getString("what"))) {
                        JSONObject location = NSR.getInstance(DemoActivity.this).getCurrentLocation();
                        if(body.has("callback") && location.has("latitude") && location.has("longitude")){
                            demoWebView.evaluateJavascript(body.getString("callback")+"("+location.toString()+")",null);
                        }

                    }
                }
            } catch (Exception e) {
                Log.d("nsr", e.getMessage(), e);
            }
        }
    };


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiOpened = menuOpened = uiMapOpened = false;
        setContentView(R.layout.activity_demo);

        btnSound = MediaPlayer.create(getApplicationContext(), R.raw.button4);
        pushSound = MediaPlayer.create(getApplicationContext(), R.raw.push);
        expandSound = MediaPlayer.create(getApplicationContext(), R.raw.expand);
        collapseSound = MediaPlayer.create(getApplicationContext(), R.raw.collapse);

        demoWebView = (TapWebView) findViewById(R.id.demoWebView);
        demoWebView.add(getApp(), this, "NSSdk");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            demoWebView.setWebContentsDebuggingEnabled(true);
        }
        findViewById(R.id.menuButtonFrame).setOnTouchListener(this);
        setFrame(R.id.menuButtonFrame, 160, 160, 80, 80);
        setFrame(R.id.menuFrame, 100, 100, 200, 200);
        setFrame(R.id.btnWifi, 75, 7, 50, 50);
        setFrame(R.id.btnHands, 125, 25, 50, 50);
        setFrame(R.id.btnBattery, 145, 75, 50, 50);
        setFrame(R.id.btnSettings, 125, 125, 50, 50);
        setFrame(R.id.btnMap, 75, 145, 50, 50);
        setFrame(R.id.btnDashboard, 25, 125, 50, 50);
        setFrame(R.id.btnList, 5, 75, 50, 50);
        setFrame(R.id.btnCamera, 25, 25, 50, 50);
        setFrame(R.id.demoWeb, (getDeviceWidth() - 300) / 2, (getDeviceHeight() - 400) / 2, 300, 400);
        setFrame(R.id.demoGoogleMap, (getDeviceWidth() - 300) / 2, (getDeviceHeight() - 400) / 2, 300, 400);
        setFrame(R.id.demoWebRound, (getDeviceWidth() - 340) / 2, (getDeviceHeight() - 440) / 2, 340, 440);
        setFrame(R.id.hairCrossMap, 130, 180, 40, 40);
        setFrame(R.id.btnCloseDemoWebUi, (getDeviceWidth() - 80) / 2, ((getDeviceHeight() - 80) / 2) + 215, 80, 80);

        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(160);
        shape.setColor(Color.WHITE);
        findViewById(R.id.btnCloseDemoWebUi).setBackground(shape);

        try {
            registerReceiver(settingsReceiver, "NSRIncomingDemoSettings");
            registerReceiver(appjsReceiver, "appjs-notification");

            JSONObject configuration = new JSONObject();
            configuration.put("base_url", "https://sandbox.neosurancecloud.net/sdk/api/v1.0/");
            configuration.put("base_demo_url", "https://sandbox.neosurancecloud.net/demo/conf?code=");
            NSR.getInstance(this).setup(configuration);
        } catch (Exception e) {
            Log.e("nsr", e.getMessage(), e);
        }



        checkPermissions();
        initializeGPS();
        initializeSpeech();
        initializeMap();
    }


    protected void onDestroy() {
        super.onDestroy();
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        NSR.getInstance(this).takePicture(this, requestCode);

        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean perm1 = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean perm2 = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (perm1 && perm2) {
                        initializeGPS();
                    } else {
                        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                        alertDialog.setTitle("Permissions");
                        alertDialog.setMessage("Please Grant Permissions...");
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Allow", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final List<String> permissionsList = new ArrayList<String>();
                                NSRUtils.addPermission(DemoActivity.this, permissionsList, Manifest.permission.ACCESS_FINE_LOCATION);
                                NSRUtils.addPermission(DemoActivity.this, permissionsList, Manifest.permission.ACCESS_COARSE_LOCATION);
                                if (permissionsList.size() > 0) {
                                    requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), PERMISSIONS_MULTIPLE_REQUEST);
                                }
                            }
                        });
                        alertDialog.show();
                    }
                }
            break;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        NSR.getInstance(this).pictureProcessed(this, new NSRUtils.NSRPictureProcessed() {
            public void onStart(){
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            }
            public void onSuccess(String base64){
                demoWebView.evaluateJavascript(resultCallback+"('"+base64+"')",null);
                findViewById(R.id.progressBar).setVisibility(View.GONE);
                resultCallback = null;
            }
        }, requestCode, resultCode);
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Permissions");
                alertDialog.setCancelable(false);
                alertDialog.setMessage("Please Grant Permissions");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Allow",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final List<String> permissionsList = new ArrayList<String>();
                                NSRUtils.addPermission(DemoActivity.this, permissionsList, Manifest.permission.ACCESS_FINE_LOCATION);
                                NSRUtils.addPermission(DemoActivity.this, permissionsList, Manifest.permission.ACCESS_COARSE_LOCATION);
                                if (permissionsList.size() > 0) {
                                    requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), PERMISSIONS_MULTIPLE_REQUEST);
                                }
                            }
                        });
                alertDialog.show();
            } else {
                final List<String> permissionsList = new ArrayList<String>();
                NSRUtils.addPermission(this, permissionsList, Manifest.permission.ACCESS_FINE_LOCATION);
                NSRUtils.addPermission(this, permissionsList, Manifest.permission.ACCESS_COARSE_LOCATION);
                if (permissionsList.size() > 0) {
                    requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), PERMISSIONS_MULTIPLE_REQUEST);
                }
            }
        }
    }


    private void initializeSpeech() {
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            public void onInit(int status) {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.getDefault());
            }
            }
        });
    }

    private void initializeMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.googleMap);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            public void onMapReady(GoogleMap map) {
                googleMap = map;
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                if (currentLocation != null) {
                    latitude = currentLocation.getLatitude();
                    longitude = currentLocation.getLongitude();
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 4));
                }
                googleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                    public void onCameraIdle() {
                        if (uiMapOpened) {
                            Address address = NSRUtils.getAddress(getApplicationContext(), googleMap.getCameraPosition().target.latitude, googleMap.getCameraPosition().target.longitude);
                            if (address != null) {
                                latitude = googleMap.getCameraPosition().target.latitude;
                                longitude = googleMap.getCameraPosition().target.longitude;
                                tts.speak(address.getCountryName(), TextToSpeech.QUEUE_FLUSH, null, null);
                            }
                        }
                    }
                });
            }
        });
    }

    private void initializeGPS() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
           ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    public void onSuccess(Location location) {
                        if (location != null) {
                            onLocationChanged(location);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    public void onFailure(Exception e) {
                        Log.d("nsr", "Errore durante il tentativo di ottenere l'ultima posizione GPS");
                    }
                });
            }
    }

    public void onLocationChanged(Location location) {
        currentLocation = location;
        if (googleMap != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 4));
        }
    }

    private void showDemoUi(final int n) {
        uiOpened = true;
        demoWebView.evaluateJavascript("setPage(0);", null);
        demoWebView.setOnloadCode("setContentData(" + NSR.getInstance(DemoActivity.this).getDemoSettings().toString() + ");setPage(" + n + ");");
        demoWebView.setDelegate(new TapWebViewDelegate() {
            public void onLoad() {
                final AppActivity activity = DemoActivity.this;
                final View spinner = activity.findViewById(R.id.progressBar);
                spinner.setVisibility(View.GONE);
            }
        });
        demoWebView.loadUrl("file:///android_asset/appui.html");
        demoWebView.setVisibility(View.VISIBLE);
        playBtnSound();
        Animation fade = new AlphaAnimation(0.00f, 1.00f);
        fade.setDuration(500);
        findViewById(R.id.demoWeb).setVisibility(View.VISIBLE);
        findViewById(R.id.demoWebUi).startAnimation(fade);
        findViewById(R.id.demoWebUi).setVisibility(View.VISIBLE);
    }

    public void hideDemoUi(View view) {
        if (uiOpened) {
            uiMapOpened = false;
            uiOpened = false;
            playBtnSound();
            Animation fade = new AlphaAnimation(1.00f, 0.00f);
            fade.setDuration(500);
            findViewById(R.id.demoWebUi).startAnimation(fade);
            findViewById(R.id.demoWebUi).setVisibility(View.INVISIBLE);
            findViewById(R.id.demoWeb).setVisibility(View.INVISIBLE);
            findViewById(R.id.demoGoogleMap).setVisibility(View.INVISIBLE);
        }
    }

    public void onWifi(View view) {
        showDemoUi(8);
    }

    public void onCamera(View view){
        showDemoUi(9);
    }

    public void onHands(View view) throws Exception {
        playBtnSound();
        NSR.getInstance(this).showApp();
    }

    public void onBattery(View view) {
        showDemoUi(2);
    }

    public void onSettings(View view) {
        showDemoUi(3);
    }

    public void onMap(View view) {
        uiMapOpened = true;
        uiOpened = true;
        playBtnSound();
        Animation fade = new AlphaAnimation(0.00f, 1.00f);
        fade.setDuration(500);
        findViewById(R.id.demoGoogleMap).setVisibility(View.VISIBLE);
        findViewById(R.id.demoWebUi).startAnimation(fade);
        findViewById(R.id.demoWebUi).setVisibility(View.VISIBLE);
    }

    public void sendLocation(View view) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("latitude", latitude);
            payload.put("longitude", longitude);
            NSR.getInstance(this).sendCustomEvent("position", payload);
        } catch (Exception e) {
            Log.d("nsr", e.getMessage(), e);
        }
    }

    public void onDashboard(View view) {
        showDemoUi(5);
    }

    public void onList(View view) {
        showDemoUi(6);
    }



    public boolean onTouch(View view, MotionEvent motionEvent) {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        final int x = (int) (motionEvent.getRawX() / metrics.density);
        final int y = (int) (motionEvent.getRawY() / metrics.density);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                downX = layoutParams.leftMargin;
                downY = layoutParams.topMargin;
                xDelta = (int) (x - downX / metrics.density);
                yDelta = (int) (y - downY / metrics.density);
                break;
            case MotionEvent.ACTION_UP:
                int upX = layoutParams.leftMargin;
                int upY = layoutParams.topMargin;
                if (Math.abs(downX - upX) < 20 && Math.abs(downY - upY) < 20) {
                    if (menuOpened) {
                        playCollapseSound();
                        View menuView = findViewById(R.id.menuFrame);
                        AnimationSet animSet = new AnimationSet(true);
                        Animation rotate = new RotateAnimation(0.00f, 360.00f, 100 * metrics.density, 100 * metrics.density);
                        rotate.setDuration(500);
                        menuView.startAnimation(rotate);
                        Animation scale = new ScaleAnimation(1.0f, 0.5f, 1.0f, 0.5f, 100 * metrics.density, 100 * metrics.density);
                        scale.setDuration(500);
                        Animation fade = new AlphaAnimation(1.00f, 0.00f);
                        fade.setDuration(500);
                        animSet.addAnimation(fade);
                        animSet.addAnimation(scale);
                        animSet.addAnimation(rotate);
                        menuView.startAnimation(animSet);
                        menuView.setVisibility(View.INVISIBLE);
                    } else {
                        playExpandSound();
                        View menuView = findViewById(R.id.menuFrame);
                        AnimationSet animSet = new AnimationSet(true);
                        Animation rotate = new RotateAnimation(0.00f, 360.00f, 100 * metrics.density, 100 * metrics.density);
                        rotate.setDuration(500);
                        menuView.startAnimation(rotate);
                        Animation scale = new ScaleAnimation(0.5f, 1.0f, 0.5f, 1.0f, 100 * metrics.density, 100 * metrics.density);
                        scale.setDuration(500);
                        Animation fade = new AlphaAnimation(0.00f, 1.00f);
                        fade.setDuration(500);
                        animSet.addAnimation(fade);
                        animSet.addAnimation(scale);
                        animSet.addAnimation(rotate);
                        menuView.startAnimation(animSet);
                        menuView.setVisibility(View.VISIBLE);
                    }
                    Animation rotate = new RotateAnimation(0.00f, 180.0f, 40 * metrics.density, 40 * metrics.density);
                    rotate.setDuration(500);
                    view.startAnimation(rotate);
                    menuOpened = !menuOpened;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int newx = Math.max(60, Math.min(x - xDelta, getDeviceWidth() - 140));
                int newy = Math.max(60, Math.min(y - yDelta, getDeviceHeight() - 140));
                setFrame(R.id.menuButtonFrame, newx, newy, 80, 80);
                setFrame(R.id.menuFrame, newx - 60, newy - 60, 200, 200);
                break;
        }
        return true;
    }

}

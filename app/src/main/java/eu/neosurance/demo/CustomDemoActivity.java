package eu.neosurance.demo;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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

import org.json.JSONObject;

import java.util.Locale;

import eu.neosurance.sdk.NSR;
import eu.neosurance.sdk.NSRUtils;
public class CustomDemoActivity extends AppActivity  implements View.OnTouchListener {

    // private static final int REQUEST_IMAGE_CAPTURE = 0x1256;
    // private static final String FILENAME_IMAGE_CAPTURE = "nsr-photo.jpg";
    // private static final int PERMISSIONS_MULTIPLE_REQUEST = 123;
    // private static final int PERMISSIONS_MULTIPLE_IMAGECAPTURE = 0x1616;
    // private static final int UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    // private static final int FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    // private static final int REQUEST_CHECK_SETTINGS = 0x1;

    protected static final int PERMISSIONS_MULTIPLE_ACCESSLOCATION = 0x2043;
    protected static final int PERMISSIONS_MULTIPLE_IMAGECAPTURE = 0x2049;

    protected boolean menuOpened;
    protected boolean uiOpened;
    protected boolean uiMapOpened;

    protected TapWebView webView;
    protected TapWebView demoWebView;
    protected int xDelta;
    protected int yDelta;
    protected int downX;
    protected int downY;

    protected double latitude = -1;
    protected double longitude = -1;
    protected Location currentLocation;
    protected String resultCallback = null;
    protected GoogleMap googleMap;

    protected TextToSpeech tts;
    protected FusedLocationProviderClient fusedLocationClient;

    protected MediaPlayer btnSound;
    protected MediaPlayer pushSound;
    protected MediaPlayer expandSound;
    protected MediaPlayer collapseSound;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiOpened = menuOpened = uiMapOpened = false;
        btnSound = MediaPlayer.create(getApplicationContext(), R.raw.button4);
        pushSound = MediaPlayer.create(getApplicationContext(), R.raw.push);
        expandSound = MediaPlayer.create(getApplicationContext(), R.raw.expand);
        collapseSound = MediaPlayer.create(getApplicationContext(), R.raw.collapse);
    }

    protected void loadUi(){
        demoWebView = (TapWebView) findViewById(R.id.demoWebView);
        demoWebView.addJavascriptInterface(new DemoReceiver((DemoActivity) this, demoWebView), "NSSdk");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            demoWebView.setWebContentsDebuggingEnabled(true);
        }
        findViewById(R.id.menuButtonFrame).setOnTouchListener(this);
        findViewById(R.id.menuButtonFrame).setVisibility(View.GONE);
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    public void playPushSound() {
        pushSound.seekTo(0);
        pushSound.start();
    }

    public void playBtnSound() {
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

    protected void initializeSpeech() {
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.getDefault());
                }
            }
        });
    }

    protected void initializeMap() {
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

    public void sendLocation(View view) { // from map
        try {
            JSONObject payload = new JSONObject();
            payload.put("latitude", latitude);
            payload.put("longitude", longitude);
            NSR.getInstance(this).sendCustomEvent("position", payload);
        } catch (Exception e) {
            Log.d(NSR.TAG, e.getMessage(), e);
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
        demoWebView.setOnloadCode("setContentData(" + NSR.getInstance(this).getDemoSettings().toString() + ");setPage(" + n + ");");
        demoWebView.setDelegate(new TapWebViewDelegate() {
            public void onLoad() {
                final AppActivity activity = CustomDemoActivity.this;
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

    public void onHands(View view) throws Exception {
        playBtnSound();
        NSR.getInstance(this).showApp();
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
    public void onWifi(View view) {
        showDemoUi(8);
    }

    public void onCamera(View view){
        showDemoUi(9);
    }

    public void onBattery(View view) {
        showDemoUi(2);
    }

    public void onSettings(View view) {
        showDemoUi(3);
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

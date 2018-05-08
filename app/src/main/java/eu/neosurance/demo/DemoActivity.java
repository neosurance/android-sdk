package eu.neosurance.demo;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.clickntap.tap.web.TapWebView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.OnSuccessListener;
import org.json.JSONObject;


import eu.neosurance.sdk.*;

public class DemoActivity extends CustomDemoActivity {
	private NSRCallbackManager callbackManager;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		registerReceiver(settingsReceiver, "NSRIncomingDemoSettings");
		setContentView(R.layout.activity_demo);

		loadUi();
		initializeSDK();
		initializeSpeech();
		initializeMap();
	}

	public void initializeSDK() {
		try {
			final JSONObject configuration = new JSONObject();
			configuration.put("base_url", "https://sandbox.neosurancecloud.net/sdk/api/v1.0/");
			configuration.put("base_demo_url", "https://sandbox.neosurancecloud.net/demo/conf?code=");
			configuration.put("ask_permission", 1);
			NSR.getInstance(this).setup(configuration);

			callbackManager = NSRCallbackManager.Factory.create();
			NSR.getInstance(this).registerCallback(callbackManager, new NSRBase64Image.Callback() {
				public void onSuccess(String base64Image) {
					Log.d(NSR.TAG, "--- base64Image " + base64Image);
					NSR.getInstance(DemoActivity.this).setData("base64Image", base64Image);
					Intent intent = new Intent();
					intent.setAction(DemoReceiver.ACTION_DEMO_RECEIVER);
					intent.putExtra("service", "base64Image");
					sendBroadcast(intent);
					findViewById(R.id.progressBar).setVisibility(View.GONE);
				}

				public void onCancel() {
					findViewById(R.id.progressBar).setVisibility(View.GONE);
				}

				public void onError() {
				}
			});
		} catch (Exception e) {
			Log.d(NSR.TAG, e.getMessage(), e);
		}
	}

	public void takePhotoRequestPermissions() {
		NSR.getInstance(this).takePicture();
		findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
	}

	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		callbackManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		callbackManager.onActivityResult(requestCode, resultCode, data);
	}

	private BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			try {
				JSONObject settings = new JSONObject(intent.getExtras().getString("json"));
				if (!settings.has("hideWheel") || settings.getInt("hideWheel") == 0) {
					findViewById(R.id.menuButtonFrame).setVisibility(View.VISIBLE);
				}
				webView = (TapWebView) findViewById(R.id.webView);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					webView.setWebContentsDebuggingEnabled(true);
				}
				webView.loadUrl(settings.getString("url"));
				webView.addJavascriptInterface(new DemoReceiver(DemoActivity.this, webView), "NSSdk");
				webView.setWebViewClient(new WebViewClient() {
					public boolean shouldOverrideUrlLoading(WebView view, String url) {
						if (url.endsWith(".pdf")) {
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setDataAndType(Uri.parse(url), "application/pdf");
							intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
							startActivity(intent);
							return true;
						} else {
							return false;
						}
					}
				});
			} catch (Exception e) {
				Log.d(NSR.TAG, "settingsReceiver " + e.getMessage());
			}
		}
	};


}

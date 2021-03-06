package eu.neosurance.sdk;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

public class NSRActivityWebView extends AppCompatActivity {
	private WebView webView;
	private NSRCallbackManager callbackManager;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nsr_activity_webview);

		try {
			Bundle extras = getIntent().getExtras();
			JSONObject json = new JSONObject(extras.getString("json"));
			webView = (WebView) findViewById(R.id.webView);
			webView.addJavascriptInterface(this, "NSSdk");
			webView.getSettings().setJavaScriptEnabled(true);
			webView.getSettings().setAllowFileAccessFromFileURLs(true);
			webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
			webView.getSettings().setBuiltInZoomControls(false);
			webView.getSettings().setAppCacheEnabled(true);
			webView.getSettings().setBlockNetworkLoads(false);
			webView.getSettings().setDomStorageEnabled(true);
			webView.getSettings().setUserAgentString(this.getClass().getCanonicalName());
			webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				webView.setWebContentsDebuggingEnabled(true);
			}
			webView.loadUrl(json.getString("url"));
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

			callbackManager = NSRCallbackManager.Factory.create();
			NSR.getInstance(this).registerCallback(callbackManager, new NSRBase64Image.Callback() {
				public void onSuccess(String base64Image) {
					webView.evaluateJavascript(NSR.getInstance(NSRActivityWebView.this).getVariable("resultCallback") + "('" + base64Image + "')", null);
					findViewById(R.id.progressBar).setVisibility(View.GONE);
				}

				public void onCancel() {
					findViewById(R.id.progressBar).setVisibility(View.GONE);
				}

				public void onError() {
				}
			});

			idle();
		} catch (Exception e) {
			Log.e("nsr", e.getMessage(), e);
		}

	}


	@JavascriptInterface
	public void postMessage(String json) {
		NSRActivityWebView.postMessage(NSRActivityWebView.this, webView, json);
	}


	public static void eval(final WebView aWebView, final String code) {
		Handler mainHandler = new Handler(Looper.getMainLooper());
		Runnable myRunnable = new Runnable() {
			public void run() {
				try {
					aWebView.evaluateJavascript(code, null);
				} catch (Exception e) {
					Log.d(NSR.TAG, e.getMessage(), e);
				}
			}
		};
		mainHandler.post(myRunnable);
	}

	public static void postMessage(final Context context, final WebView aWebView, final String json) {
		try {
			final JSONObject body = new JSONObject(json);
			if (body.has("event") && body.has("payload")) {
				NSR.getInstance(context).sendCustomEvent(body.getString("event"), body.getJSONObject("payload"));
			}
			if (body.has("what")) {
				if ("init".equals(body.getString("what"))) {
					NSR.getInstance(context).token(new NSRToken() {
						public void token(String token) throws Exception {
							JSONObject settings = NSR.getInstance(context).getSettings();
							JSONObject message = new JSONObject();
							message.put("api", settings.getString("base_url"));
							message.put("token", token);
							message.put("lang", "it");
							message.put("deviceUid", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
							eval(aWebView, body.getString("callBack") + "(" + message.toString() + ")");
						}
					});
				}
				if ("close".equals(body.getString("what"))) {
					((Activity) context).finish();
				}
				if ("refresh".equals(body.getString("what"))) {
					NSR.getInstance(context).resetAll();
					((Activity) context).finish();
					((Activity) context).startActivity(((Activity) context).getIntent());
				}
				if ("photo".equals(body.getString("what"))) {
					if (body.has("callBack")) {
						NSR.getInstance(context).setVariable("resultCallback", body.getString("callBack"));
						NSR.getInstance(context).takePicture();
					}
				}
				if ("location".equals(body.getString("what"))) {
					JSONObject location = NSR.getInstance(context).getCurrentLocation();
					if (body.has("callBack") && location.has("latitude") && location.has("longitude")) {
						eval(aWebView, body.getString("callBack") + "(" + location.toString() + ")");
					}
				}
				if ("code".equals(body.getString("what"))) {
					if (body.has("callBack")) {
						final String code = NSR.getInstance(context).getDemoSettings().getString("code");
						eval(aWebView, body.getString("callBack") + "('" + code + "')");
					}
				}
				if ("user".equals(body.getString("what"))) {
					if (body.has("callBack")) {
						eval(aWebView, body.getString("callBack") + "(" + NSR.getInstance(context).getUser().toJsonObject().toString() + ")");
					}
				}
				if ("action".equals(body.getString("what"))) {
					NSR.getInstance(context).sendAction(body.getString("action"), body.getString("code"), body.getString("details"));
				}
				if ("showapp".equals(body.getString("what"))) {
					if (body.has("params")) {
						NSR.getInstance(context).showApp(body.getJSONObject("params"));
					} else {
						NSR.getInstance(context).showApp();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(NSR.TAG, e.getMessage());
		}
	}


	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		callbackManager.onActivityResult(requestCode, resultCode, data);
	}

	private void idle() {
		new Handler().postDelayed(new Runnable() {
			public void run() {
				webView.evaluateJavascript("(function() { return (window.document.body.className.indexOf('NSR') == -1 ? false : true); })();", new ValueCallback<String>() {
					public void onReceiveValue(String value) {
						if ("true".equals(value)) {
							idle();
						} else {
							finish();
						}
					}
				});
			}
		}, 15 * 1000);
	}

}

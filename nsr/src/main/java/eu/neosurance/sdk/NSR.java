package eu.neosurance.sdk;


import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import cz.msebera.android.httpclient.Header;

public class NSR {
	public static final String TAG = "nsr";
	private static final String PREFS_NAME = "NSRSDK";
	public static final int PERMISSIONS_MULTIPLE_ACCESSLOCATION = 0x2043;
	public static final int PERMISSIONS_MULTIPLE_IMAGECAPTURE = 0x2049;
	private static NSR instance = null;

	private JSONObject settings = null;
	private JSONObject demoSettings = null;
	private JSONObject authSettings = null;
	private NSRUser user = null;
	private Context ctx = null;
	private JSONObject currentLocation = null;
	private JSONObject lastLocation = null;
	private boolean stillPositionSent = false;
	private NSRSecurityDelegate securityDelegate = null;
	private JSONObject variables;

	private NSRServiceTask serviceTask = null;

	public void setServiceTask(NSRServiceTask serviceTask) {
		this.serviceTask = serviceTask;
	}

	public NSRServiceTask getServiceTask() {
		return serviceTask;
	}


	private NSR(Context ctx) {
		this.ctx = ctx;
		this.variables = new JSONObject();
	}

	public static NSR getInstance(Context ctx) {
		if (instance == null) {
			instance = new NSR(ctx);
			instance.setSecurityDelegate(new NSRDefaultSecurity());
		} else {
			instance.ctx = ctx;
		}
		return instance;
	}

	public NSRSecurityDelegate getSecurityDelegate() {
		return securityDelegate;
	}

	public void setSecurityDelegate(NSRSecurityDelegate securityDelegate) {
		this.securityDelegate = securityDelegate;
	}

	public void setVariable(String name, String value) {
		try {
			variables.put(name, value);
		} catch (JSONException e) {
		}
	}

	public String getVariable(String name) {
		try {
			return variables.getString(name);
		} catch (JSONException e) {
			return null;
		}
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

	protected void setCurrentLocation(JSONObject currentLocation) {
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

	protected void setLastLocation(JSONObject lastLocation) {
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

	protected void setSettings(JSONObject settings) {
		this.settings = settings;
		setData("settings", settings == null ? "" : settings.toString());
	}

	public JSONObject getDemoSettings() {
		try {
			if (demoSettings == null) {
				demoSettings = new JSONObject(getData("demoSettings", "{}"));
			}
			return demoSettings;
		} catch (Exception e) {
			return new JSONObject();
		}
	}

	protected void setDemoSettings(JSONObject demoSettings) {
		this.demoSettings = demoSettings;
		setData("demoSettings", demoSettings == null ? "" : demoSettings.toString());
	}

	public JSONObject getAuthSettings() {
		try {
			if (authSettings == null) {
				authSettings = new JSONObject(getData("authSettings", "{}"));
			}
			return authSettings;
		} catch (Exception e) {
			return new JSONObject();
		}
	}

	protected void setAuthSettings(JSONObject authSettings) {
		this.authSettings = authSettings;
		setData("authSettings", authSettings == null ? "" : authSettings.toString());
	}


	public boolean getStillPositionSent() {
		if (!stillPositionSent) {
			stillPositionSent = Boolean.parseBoolean(getData("stillPositionSent", "false"));
		}
		return stillPositionSent;
	}

	public void setStillPositionSent(boolean stillPositionSent) {
		this.stillPositionSent = stillPositionSent;
		setData("stillPositionSent", "" + stillPositionSent);
	}

	public NSRUser getUser() {
		try {
			if (user == null) {
				user = new Gson().fromJson(getData("user", "{}"), NSRUser.class);
			}
			return user;
		} catch (Exception e) {
			return new NSRUser();
		}
	}

	protected void setUser(NSRUser user) {
		this.user = user;
		setData("user", user == null ? "" : new Gson().toJson(user));
	}

	public void token(final NSRToken delegate) {
		try {
			authorize(new NSRAuth() {
				public void authorized(boolean authorized) throws Exception {
					if (authorized) {
						delegate.token(getAuthSettings().getJSONObject("auth").getString("token"));
					} else {
						delegate.token(null);
					}
				}
			});
		} catch (Exception e) {
		}
	}

	public void location(final NSRLocation location) throws Exception {
		if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
			ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			location.currentLocation(getCurrentLocation());
		} else {
			NSRCallbackManagerImpl.registerStaticCallback(NSR.PERMISSIONS_MULTIPLE_ACCESSLOCATION, new NSRCallbackManagerImpl.PermissionCallback() {
				public boolean onRequestPermissionsResult(String[] permissions, int[] grantResults) {
					startService();
					new Handler().postDelayed(new Runnable() {
						public void run() {
							try {
								location(location);
							} catch (Exception e) {
								Log.d(NSR.TAG, e.toString());
							}
						}
					}, 1000);
					return true;
				}
			});
			List<String> permissionsList = new ArrayList<String>();
			NSRUtils.addPermission(ctx, permissionsList, Manifest.permission.ACCESS_FINE_LOCATION);
			NSRUtils.addPermission(ctx, permissionsList, Manifest.permission.ACCESS_COARSE_LOCATION);
			if (permissionsList.size() > 0) {
				ActivityCompat.requestPermissions((Activity) ctx, permissionsList.toArray(new String[permissionsList.size()]), NSR.PERMISSIONS_MULTIPLE_ACCESSLOCATION);
			}
		}
	}

	public void authorize(final NSRAuth delegate) throws Exception {
		int remainingSeconds = NSRUtils.tokenRemainingSeconds(getAuthSettings());
		if (remainingSeconds > 0) {
			delegate.authorized(true);
		} else {
			try {
				JSONObject payload = new JSONObject();
				payload.put("user_code", getUser().getCode());
				payload.put("code", getSettings().getString("code"));
				payload.put("secret_key", getSettings().getString("secret_key"));

				JSONObject sdkPayload = new JSONObject();
				sdkPayload.put("version", getVersion());
				sdkPayload.put("dev", getSettings().getString("dev_mode"));
				sdkPayload.put("os", getOs());
				payload.put("sdk", sdkPayload);

				securityDelegate.secureRequest(this.ctx, "authorize", payload, null, new NSRSecurityResponse() {
					public void completionHandler(JSONObject response, String error) throws Exception {
						try {
							setAuthSettings(response);
							int remainingSeconds = NSRUtils.tokenRemainingSeconds(getAuthSettings());
							delegate.authorized(remainingSeconds > 0);
						} catch (Exception e) {
							Log.e("nsr", error, e);
						}
					}
				});
			} catch (Exception e) {
				delegate.authorized(false);
				Log.e("nsr", e.getMessage(), e);
			}
		}
	}

	public void setup(final JSONObject settings) {
		setup(settings, false);
	}

	private void setup(final JSONObject settings, boolean hard) {
		if (!hard && !settings.has("base_demo_url") && this.settings != null) {
			Log.d(NSR.TAG, "already setup");
			return;
		}
		Log.d(NSR.TAG, "setup");
		try {
			if ("0".equals(getData("permission_required", "0")) && settings.has("ask_permission") && settings.getInt("ask_permission") == 1) {
				setData("permission_required", "1");
				if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
					ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
					NSRCallbackManagerImpl.registerStaticCallback(NSR.PERMISSIONS_MULTIPLE_ACCESSLOCATION, new NSRCallbackManagerImpl.PermissionCallback() {
						public boolean onRequestPermissionsResult(String[] permissions, int[] grantResults) {
							setup(settings);
							return true;
						}
					});
					List<String> permissionsList = new ArrayList<String>();
					NSRUtils.addPermission(ctx, permissionsList, Manifest.permission.ACCESS_FINE_LOCATION);
					NSRUtils.addPermission(ctx, permissionsList, Manifest.permission.ACCESS_COARSE_LOCATION);
					if (permissionsList.size() > 0) {
						ActivityCompat.requestPermissions((Activity) ctx, permissionsList.toArray(new String[permissionsList.size()]), NSR.PERMISSIONS_MULTIPLE_ACCESSLOCATION);
					}
					return;
				}
			}

			if (!settings.has("ns_lang")) {
				settings.put("ns_lang", Locale.getDefault().getDisplayLanguage());
			}
			if (!settings.has("dev_mode")) {
				settings.put("dev_mode", 0);
			}
			setStillPositionSent(false);
			setSettings(settings);

			if (settings.has("base_demo_url")) {
				setAuthSettings(null);
				setUser(null);
				JSONObject demoSettings = getDemoSettings();
				final String demoCode = demoSettings.has("code") ? demoSettings.getString("code") : "";
				final String url = getSettings().getString("base_demo_url") + demoCode;
				Log.d("nsr", "url " + url);
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
									setup(appSettings,true);

									NSRUser user = new NSRUser();
									user.setEmail(getDemoSettings().getString("email"));
									user.setCode(getDemoSettings().getString("code"));
									user.setFirstname(getDemoSettings().getString("firstname"));
									user.setLastname(getDemoSettings().getString("lastname"));
									registerUser(user);
								} catch (Exception e) {
									Log.d(NSR.TAG, e.getMessage());
								}
							}

							public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
							}
						});

					}
				};
				mainHandler.post(myRunnable);
			}
		} catch (Exception e) {
			Log.d(NSR.TAG, e.getMessage());
		}
	}

	public void registerUser(NSRUser user) {
		try {
			clearUser();
			setUser(user);
			authorize(new NSRAuth() {
				public void authorized(boolean authorized) throws Exception {
					startService();
				}
			});


		} catch (Exception e) {
			Log.d("nsr", e.getMessage(), e);
		}
	}

	private void startService() {
		try {
			if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
				ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				JSONObject conf = getAuthSettings().getJSONObject("conf");

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					Log.d(NSR.TAG, "Start Job Service...");
					NSRJobService.schedule(ctx, 1000);
				} else {
					Log.d(NSR.TAG, "Start Service...");
					NSRService.start(ctx);
					NSRSync.register(ctx, conf.getInt("time") * 1000);
				}

			}

		} catch (Exception e) {
			Log.d(NSR.TAG, e.getMessage(), e);
		}
	}

	private void stopService() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			NSRJobService.cancel(ctx);
		} else {
			NSRSync.unregister(ctx);
		}
	}

	public void showApp() throws Exception {
		showApp(null);
	}

	public void showApp(JSONObject params) throws Exception {
		String url = getAuthSettings().getString("app_url") + "?";
		if (params != null && params.length() > 0) {
			Iterator<String> keys = params.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				url += key + "=" + URLEncoder.encode(params.getString(key), "UTF-8") + "&";
			}
			url = url.substring(0, url.length() - 1);
		}
		Intent intent = new Intent(ctx, NSRActivityWebView.class);
		JSONObject json = new JSONObject();
		Log.d(NSR.TAG, "url " + url);
		json.put("url", url);
		intent.putExtra("json", json.toString());
		ctx.startActivity(intent);
	}

	public void takePicture() {
		if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
			ActivityCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			NSRCallbackManagerImpl.registerStaticCallback(NSR.PERMISSIONS_MULTIPLE_IMAGECAPTURE, new NSRCallbackManagerImpl.PermissionCallback() {
				public boolean onRequestPermissionsResult(String[] permissions, int[] grantResults) {
					NSRBase64Image.getInstance(ctx).takePhoto();
					return true;
				}
			});
			List<String> permissionsList = new ArrayList<String>();
			NSRUtils.addPermission(ctx, permissionsList, Manifest.permission.CAMERA);
			NSRUtils.addPermission(ctx, permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE);
			if (permissionsList.size() > 0) {
				ActivityCompat.requestPermissions((Activity) ctx, permissionsList.toArray(new String[permissionsList.size()]), NSR.PERMISSIONS_MULTIPLE_IMAGECAPTURE);
			}
		} else {
			NSRBase64Image.getInstance(ctx).takePhoto();
		}
	}

	public void registerCallback(NSRCallbackManager callbackManager, NSRBase64Image.Callback callback) {
		NSRBase64Image.getInstance(ctx).registerCallback(callbackManager, callback);
	}

	public void resetAll() throws Exception {
		setAuthSettings(null);
	}

	public void clearUser() throws Exception {
		resetAll();
		setUser(null);
		stopService();
	}

	public void sendAction(final String name, final String policyCode, final String details) throws Exception {
		NSR.getInstance(ctx).token(new NSRToken() {
			public void token(String token) throws Exception {
				JSONObject payload = new JSONObject();

				payload.put("action", name);
				payload.put("code", policyCode);
				payload.put("details", details);
				payload.put("timezone", TimeZone.getDefault().getID());
				payload.put("action_time", System.currentTimeMillis());

				JSONObject headers = new JSONObject();
				headers.put("ns_token", token);
				headers.put("ns_lang", NSR.getInstance(ctx).getSettings().getString("ns_lang"));

				NSR.getInstance(ctx).getSecurityDelegate().secureRequest(ctx, "trace", payload, headers, new NSRSecurityResponse() {
					public void completionHandler(JSONObject json, String error) throws Exception {
						if (error != null) {
							Log.e("nsr", error);
						} else {
							Log.i("nsr", json.toString());
						}
					}
				});
			}
		});
	}

	public void sendCustomEvent(String name, JSONObject payload) throws Exception {
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
		if (value != null) {
			editor.putString(key, value);
		} else {
			editor.remove(key);
		}
		editor.commit();
	}

	public interface NSRLocation {
		public void currentLocation(JSONObject location) throws Exception;
	}

}

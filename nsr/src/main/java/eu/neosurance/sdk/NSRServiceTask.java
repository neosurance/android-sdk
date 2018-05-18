package eu.neosurance.sdk;

import android.Manifest;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.util.Map;

public class NSRServiceTask extends AsyncTask<String, Void, String> {
	private Context context;
	private JSONObject conf;

	private GoogleApiClient mGoogleApiClient;
	private PendingIntent mPendingIntent;
	private LocationRequest mLocationRequest;
	private LocationListener mLocationListener;

	private int nLocation = 0;

	public NSRServiceTask(Context context) {
		this.context = context;
		init();
	}

	public void init() {
		NSRServiceTask st = NSR.getInstance(context).getServiceTask();
		if (st != null) {
			Log.d(NSR.TAG, "init..shutDownRecognition");
			st.shutDownRecognition();
		}
		NSR.getInstance(context).setServiceTask(this);
		try {
			this.conf = NSR.getInstance(context).getAuthSettings().getJSONObject("conf");
			Log.d(NSR.TAG, "isGooglePlayServicesAvailable >> " + (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS));
			if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
				mLocationRequest = LocationRequest.create();
				mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
				mLocationRequest.setInterval(0);
				mLocationRequest.setFastestInterval(0);

				mLocationListener = new LocationListener() {
					public void onLocationChanged(Location location) {
						Log.d(NSR.TAG, "mLocationListener..nloc.. " + nLocation);
						if (location != null && (location.getAccuracy() <= 50 || nLocation > 1)) {
							Log.d(NSR.TAG, "mLocationListener.." + location.getAccuracy());
							try {
								float distanceInMeters = 0.0f;
								JSONObject locationAsJson = new JSONObject();
								locationAsJson.put("latitude", location.getLatitude());
								locationAsJson.put("longitude", location.getLongitude());

								NSR.getInstance(context).setCurrentLocation(locationAsJson);
								JSONObject lastLocation = NSR.getInstance(context).getLastLocation();

								if (lastLocation.has("latitude") && lastLocation.has("longitude")) {
									Location oldLoc = new Location("");
									oldLoc.setLatitude(lastLocation.getDouble("latitude"));
									oldLoc.setLongitude(lastLocation.getDouble("longitude"));

									Location newLoc = new Location("");
									newLoc.setLatitude(location.getLatitude());
									newLoc.setLongitude(location.getLongitude());

									distanceInMeters = oldLoc.distanceTo(newLoc);
									Log.d(NSR.TAG, "oldLocation: " + oldLoc.getLatitude() + "," + oldLoc.getLongitude());
									Log.d(NSR.TAG, "currentLocation: " + newLoc.getLatitude() + "," + newLoc.getLongitude());
									Log.d(NSR.TAG, "distanceInMeters: " + distanceInMeters);
								}

								if (distanceInMeters > conf.getJSONObject("position").getDouble("meters") || !(lastLocation.has("latitude") && lastLocation.has("longitude"))) {
									Log.d(NSR.TAG, "------ location sent: " + locationAsJson.getDouble("latitude") + "," + locationAsJson.getDouble("longitude"));
									JSONObject payload = new JSONObject(locationAsJson.toString());
									NSR.getInstance(context).sendCustomEvent("position", payload);
									NSR.getInstance(context).setStillPositionSent(false);
									NSR.getInstance(context).setLastLocation(locationAsJson);
								}
								activity();

							} catch (Exception e) {
								Log.d(NSR.TAG, "init 1 >> " + e.toString());
							}
							try {
								LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
							} catch (Exception e) {
								Log.d(NSR.TAG, "init 2 >> " + e.toString());
							}
						}
						nLocation++;
					}
				};

				mGoogleApiClient = new GoogleApiClient.Builder(context)
					.addApi(ActivityRecognition.API)
					.addApi(LocationServices.API)
					.addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
						public void onConnected(Bundle bundle) {
							position();
						}

						public void onConnectionSuspended(int i) {
							mGoogleApiClient.connect();
						}
					})
					.build();
			}else
				sendNoGps();

		} catch (Exception e) {
			Log.d(NSR.TAG, "init 3 >> " + e.toString());
		}
	}

	protected String doInBackground(String... params) {
		Log.d(NSR.TAG, "doInBackground.....");
		Log.d(NSR.TAG, "mGoogleApiClient >> " + mGoogleApiClient);
		Log.d(NSR.TAG, "Connectivity.isConnected >> " + Connectivity.isConnected(context));
		if (mGoogleApiClient != null && Connectivity.isConnected(context)) {
			if (battery()) {
				SystemClock.sleep(2000);
			}
			connection();
			mGoogleApiClient.connect();
		}
		return null;
	}

	protected void position() {
		try {
			if (mGoogleApiClient != null && conf.getJSONObject("position").getInt("enabled") == 1) {
				if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
					ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationListener);
					NSR.getInstance(context).setData("gpsState", "on");
				} else {
					sendNoGps();
				}
			} else {
				sendNoGps();
			}
		} catch (Exception e) {
			Log.d(NSR.TAG, "doInBackground >> " + e.toString());
			sendNoGps();
		}
	}

	private void sendNoGps() {
		try {
			if ("on".equals(NSR.getInstance(context).getData("gpsState", "on"))) {
				Log.d(NSR.TAG, "no_gps.....");
				NSR.getInstance(context).setData("gpsState", "off");
				NSR.getInstance(context).sendCustomEvent("no_gps", new JSONObject());
			}
		} catch (Exception e) {
			Log.d(NSR.TAG, "sendNoGps >> " + e.toString());
		}
	}

	protected void activity() {
		Log.d(NSR.TAG, "activity.....");
		try {
			Log.d(NSR.TAG, "activity in.....");

			if (mGoogleApiClient != null && conf.getJSONObject("activity").getInt("enabled") == 1) {
				Log.d(NSR.TAG, "activity in in.....");
				mPendingIntent = PendingIntent.getService(context, 0, new Intent(context, NSRActivityRecognitionService.class), PendingIntent.FLAG_UPDATE_CURRENT);
				ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 0, mPendingIntent);
			}
		} catch (Exception e) {
			Log.d(NSR.TAG, "activity >> " + e.toString());
		}
	}

	protected boolean battery() {
		try {
			if (conf.getJSONObject("power").getInt("enabled") == 1) {
				Log.d(NSR.TAG, "battery in.....");

				Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
				int currentLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

				JSONObject payload = new JSONObject();
				payload.put("type", (chargePlug > 0 ? "plugged" : "unplugged"));
				payload.put("level", "" + currentLevel);

				JSONObject lastPower = new JSONObject(NSR.getInstance(context).getData("lastPower", "{}"));
				String lastType = "";
				int differenceLevel = 0;

				if (lastPower.has("type") && lastPower.has("level")) {
					lastType = lastPower.getString("type");
					int lastLevel = lastPower.getInt("level");
					differenceLevel = lastLevel > currentLevel ? lastLevel - currentLevel : currentLevel - lastLevel;
				}

				if (!payload.getString("type").equals(lastType) || differenceLevel >= 5) {
					Log.d(NSR.TAG, "battery sending.....");
					NSR.getInstance(context).sendCustomEvent("power", payload);
					NSR.getInstance(context).setData("lastPower", payload.toString());
					return true;
				}
			}
		} catch (Exception e) {
			Log.d(NSR.TAG, "battery >> " + e.toString());
		}
		return false;
	}

	protected boolean connection() {
		try {
			if (conf.getJSONObject("connection").getInt("enabled") == 1) {
				Log.d(NSR.TAG, "connection in.....");
				JSONObject payload = new JSONObject();
				if (Connectivity.isConnectedWifi(context)) {
					payload.put("type", "wi-fi");
				} else {
					payload.put("type", "mobile");
				}

				if (!payload.getString("type").equals(NSR.getInstance(context).getData("lastConnection", ""))) {
					Log.d(NSR.TAG, "connection sending.....");

					NSR.getInstance(context).sendCustomEvent("connection", payload);
					NSR.getInstance(context).setData("lastConnection", payload.getString("type"));
					return true;
				}
			}
		} catch (Exception e) {
			Log.d(NSR.TAG, "connection >> " + e.toString());
		}
		return false;
	}

	public void shutDownRecognition() {
		Log.d(NSR.TAG, "shutDownRecognition...");
		try {
			ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, mPendingIntent);
		} catch (Exception e) {
		}
		try {
			LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
		} catch (Exception e) {
		}
		try {
			if (mGoogleApiClient.isConnected()) {
				mGoogleApiClient.disconnect();
			}
		} catch (Exception e) {
		}
	}

	private static class Connectivity {
		public static NetworkInfo getNetworkInfo(Context context) {
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			return cm.getActiveNetworkInfo();
		}

		public static boolean isConnected(Context context) {
			NetworkInfo info = Connectivity.getNetworkInfo(context);
			return (info != null && info.isConnected());
		}

		public static boolean isConnectedWifi(Context context) {
			NetworkInfo info = Connectivity.getNetworkInfo(context);
			return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
		}

		public static boolean isConnectedMobile(Context context) {
			NetworkInfo info = Connectivity.getNetworkInfo(context);
			return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE);
		}
	}
}

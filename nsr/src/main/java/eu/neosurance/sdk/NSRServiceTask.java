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

public class NSRServiceTask extends AsyncTask<String, Void, String> {
    private Context context;
    private JobService jobService;
    private JobParameters jobParameters;
    private JSONObject conf;

    private GoogleApiClient mGoogleApiClient;
    private PendingIntent mPendingIntent;
    private LocationRequest mLocationRequest;
    private LocationListener mLocationListener;

    private boolean requestActivityUpdates = false;
    private boolean requestLocationUpdates = false;


    public NSRServiceTask(JobService jobService, JobParameters jobParameters) {
        this.jobService = jobService;
        this.jobParameters = jobParameters;
        this.context = jobService.getApplicationContext();
        init();
    }

    public NSRServiceTask(Context context) {
        this.context = context;
        init();
    }

    public void init() {
        try {
            this.conf = NSR.getInstance(context).getAuthSettings().getJSONObject("conf");
            if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
                mLocationRequest = LocationRequest.create();
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                mLocationRequest.setInterval(0);
                mLocationRequest.setFastestInterval(0);

                mLocationListener = new LocationListener() {
                    public void onLocationChanged(Location location) {
                        if (location != null) {
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
                            }
                        }
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
            }

        } catch (Exception e) {
        }
    }

    protected String doInBackground(String... params) {
        Log.d(NSR.TAG, "doInBackground.....");
        if (Connectivity.isConnected(context)) {
            mGoogleApiClient.connect();
            battery();
            connection();
            SystemClock.sleep(5 * 1000);
        }
        return null;
    }

    protected void position() {
        try {
            if (mGoogleApiClient != null && !requestLocationUpdates && conf.getJSONObject("position").getInt("enabled") == 1) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationListener);
                    this.requestLocationUpdates = true;
                }
            }
        } catch (Exception e) {
        }
    }

    protected void activity() {
        try {
            if (mGoogleApiClient != null && !requestActivityUpdates && conf.getJSONObject("activity").getInt("enabled") == 1) {
                mPendingIntent = PendingIntent.getService(context, 0, new Intent(context, NSRActivityRecognitionService.class), PendingIntent.FLAG_UPDATE_CURRENT);
                ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 0, mPendingIntent);
                this.requestActivityUpdates = true;
            }
        } catch (Exception e) {
        }
    }

    protected void battery() {
        try {
            if (conf.getJSONObject("power").getInt("enabled") == 1) {
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
                    NSR.getInstance(context).sendCustomEvent("power", payload);
                    NSR.getInstance(context).setData("lastPower", payload.toString());
                }
            }
        } catch (Exception e) {
            Log.d(NSR.TAG, e.toString());
        }
    }

    protected void connection() {
        try {
            if (conf.getJSONObject("connection").getInt("enabled") == 1) {
                JSONObject payload = new JSONObject();
                if (Connectivity.isConnectedWifi(context)) {
                    payload.put("type", "wi-fi");
                } else {
                    payload.put("type", "mobile");
                }

                if (!payload.getString("type").equals(NSR.getInstance(context).getData("lastConnection", ""))) {
                    NSR.getInstance(context).sendCustomEvent("connection", payload);
                    NSR.getInstance(context).setData("lastConnection", payload.getString("type"));
                }
            }
        } catch (Exception e) {
        }
    }

    protected void onPostExecute(String result) {
        try {
            if (mGoogleApiClient != null) {
                if (requestActivityUpdates) {
                    ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, mPendingIntent);
                }
                if (requestLocationUpdates) {
                    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
                }
                if (mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }
            }
        } catch (Exception e) {
        }

        if (jobService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                NSRJobService.schedule(context, conf.getInt("time") * 1000);
            } catch (Exception e) {
            }
            jobService.jobFinished(jobParameters, false);
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

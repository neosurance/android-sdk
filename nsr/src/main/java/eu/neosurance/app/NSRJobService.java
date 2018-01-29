package eu.neosurance.app;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class NSRJobService extends JobService {
    private static final int JOB_ID = 1;
    private static final int ONE_MIN = 60 * 1000;
    private JobTask jobTask;

    public static void schedule(Context context) {
        ComponentName component = new ComponentName(context, NSRJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component)
                .setPeriodic(2*ONE_MIN)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        jobTask = new JobTask(this);
        jobTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (jobTask != null) {
            jobTask.cancel(true);
        }
        return false;
    }

    private static class JobTask extends AsyncTask<JobParameters, Void, JobParameters> {
        private final Context ctx;
        private boolean initiliaze = false;
        private final JobService jobService;
        private ActivityRecognitionClient activityRecognitionClient;
        private PendingIntent pendingIntentActivityRecognition;

        private FusedLocationProviderClient fusedLocationClient;
        private LocationRequest locationRequest;

        private JSONObject powerPayload = null;
        private JSONObject connectivityPayload = null;
        private JSONObject activityPayload = null;
        private JSONObject countryPayload = null;

        public JobTask(JobService jobService) {
            this.jobService = jobService;
            this.ctx = jobService.getApplicationContext();

            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction("NSRIncomingDemoSettings");
            this.ctx.registerReceiver(settingsReceiver, iFilter);

            IntentFilter iFilterActivities = new IntentFilter();
            iFilterActivities.addAction("NSRActivityRecognition");
            this.ctx.registerReceiver(activityRecognitionReceiver, iFilterActivities);

            if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx) == ConnectionResult.SUCCESS) {
                activityRecognitionClient = ActivityRecognition.getClient(ctx);
                pendingIntentActivityRecognition = PendingIntent.getService(ctx, 0, new Intent(ctx, NSRActivityRecognitionService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            }

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx);
            locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        }

        private BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                initiliaze = true;
            }
        };

        private BroadcastReceiver activityRecognitionReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                try {
                    activityPayload = new JSONObject();
                    activityPayload.put("type", intent.getExtras().getString("activity"));
                    activityPayload.put("conficende", intent.getExtras().getInt("conficende"));
                } catch (Exception e) {
                    Log.d("nsr", "activityRecognitionReceiver" + e.toString());
                    activityPayload = null;
                }
            }
        };

        private void initializeNSR() throws Exception {
            JSONObject configuration = new JSONObject();
            configuration.put("base_url", "https://sandbox.neosurancecloud.net/sdk/api/v1.0/");
            configuration.put("base_demo_url", "https://sandbox.neosurancecloud.net/demo/conf?code=");
            NSR.getInstance().setup(configuration, ctx);
        }

        public void batteryTask() {
            try {
                IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = jobService.registerReceiver(null, iFilter);
                int currentLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                powerPayload = new JSONObject();
                powerPayload.put("type", (chargePlug > 0 ? "plugged" : "unplugged"));
                powerPayload.put("level", currentLevel);
            } catch (Exception e) {
                powerPayload = null;
            }
        }

        private void connectivityTask() {
            try {
                connectivityPayload = new JSONObject();
                if (Connectivity.isConnectedWifi(ctx)) {
                    connectivityPayload.put("type", "wifi");
                } else {
                    connectivityPayload.put("type", "mobile");
                }
            } catch (Exception e) {
                connectivityPayload = null;
            }
        }

        public void activityTask() {
            if (activityRecognitionClient != null) {
                activityRecognitionClient.requestActivityUpdates(0, pendingIntentActivityRecognition);
            }
        }

        public void locationTask() {
//            if(ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
//                    ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
//                fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
//                    public void onSuccess(Location location) {
//                        if (location != null) {
//                            try{
//                                Address address = DemoActivity.getAddress(ctx, location.getLatitude(), location.getLongitude());
//                                countryPayload = new JSONObject();
//                                countryPayload.put("address", address.getThoroughfare());
//                                countryPayload.put("latitude", address.getLatitude());
//                                countryPayload.put("longitude", address.getLongitude());
//                            }catch(Exception e){
//                                countryPayload = null;
//                            }
//                        }
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//                    public void onFailure(Exception e) {
//                        Log.d("nsr", "Errore durante il tentativo di ottenere l'ultima posizione GPS");
//                    }
//                });
//            }
        }

        private void sendEvents(){
            try{
                if(powerPayload != null){
                    NSRRequest request = new NSRRequest(NSRUtils.makeEvent("power", powerPayload));
                    request.send(ctx);
                    NSRNotification.sendNotification(ctx, "Battery Level", "level " + powerPayload.getInt("level") + "% - " + powerPayload.getString("type"));
                }
                if(connectivityPayload != null){
                    NSRRequest request = new NSRRequest(NSRUtils.makeEvent("connection", connectivityPayload));
                    request.send(ctx);
                    NSRNotification.sendNotification(ctx, "Connectivity", "Is connected with "+connectivityPayload.getString("type"));
                }
                if(activityPayload != null){
                    NSRRequest request = new NSRRequest(NSRUtils.makeEvent("activity", activityPayload));
                    request.send(ctx);
                    NSRNotification.sendNotification(ctx, "Activity", "Activity "+activityPayload.getString("type") +" - conficende "+activityPayload.getInt("conficende")+"%");
                }
                if(countryPayload != null){
                    NSRNotification.sendNotification(ctx, "Address", ""+countryPayload.getString("address"));
                    Log.d("nsr", "address " + countryPayload.getString("address"));
                }
            }catch(Exception e){
                Log.d("nsr", "sendEvents "+e.getMessage());
            }
        }

        @Override
        protected JobParameters doInBackground(JobParameters... params) {
            try{
                if(Connectivity.isConnected(ctx)) {
                    initializeNSR();
                    batteryTask();
                    connectivityTask();
                    activityTask();
                    locationTask();
                    SystemClock.sleep(10000);
                }
            }catch(Exception e){
            }
            return params[0];
        }

        @Override
        protected void onPostExecute(JobParameters params) {
            ctx.unregisterReceiver(settingsReceiver);
            ctx.unregisterReceiver(activityRecognitionReceiver);

            if(activityRecognitionClient != null){
                activityRecognitionClient.removeActivityUpdates(pendingIntentActivityRecognition);
            }
            if(initiliaze){
                sendEvents();
            }
            Log.d("nsr", "------ jobFinished");
            jobService.jobFinished(params, false);
        }
    }

    private static class Connectivity{
        public static NetworkInfo getNetworkInfo(Context context){
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            return cm.getActiveNetworkInfo();
        }

        public static boolean isConnected(Context context){
            NetworkInfo info = Connectivity.getNetworkInfo(context);
            return (info != null && info.isConnected());
        }

        public static boolean isConnectedWifi(Context context){
            NetworkInfo info = Connectivity.getNetworkInfo(context);
            return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
        }

        public static boolean isConnectedMobile(Context context){
            NetworkInfo info = Connectivity.getNetworkInfo(context);
            return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE);
        }
    }
}

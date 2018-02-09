package eu.neosurance.sdk;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;


public class NSRActivityRecognitionService extends IntentService {

    public NSRActivityRecognitionService() {
        super("NSRDetectedActivities");
    }

    protected void onHandleIntent(Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)){
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbable = result.getMostProbableActivity();
            try{
                String type = getType(mostProbable.getType());
                if(!"".equals(type)){
                    Intent i = new Intent();
                    i.setAction("NSRActivityRecognition");
                    i.putExtra("activity", type);
                    i.putExtra("conficende", mostProbable.getConfidence());
                    sendBroadcast(i);
                }
            }catch (Exception e) {
                Log.d("nsr", "NSRActivityRecognitionService "+e.toString());
            }
        }
    }

    private String getType(int type){
       if(type == DetectedActivity.IN_VEHICLE)
            return "car";
        else if(type == DetectedActivity.ON_BICYCLE)
            return "bicycle";
        else if(type == DetectedActivity.ON_FOOT || type == DetectedActivity.WALKING)
            return "walk";
        else if(type == DetectedActivity.STILL)
            return "still";
        else if(type == DetectedActivity.RUNNING)
            return "run";
        else
            return "";
    }

}

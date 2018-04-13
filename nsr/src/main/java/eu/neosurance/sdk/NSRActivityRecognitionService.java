package eu.neosurance.sdk;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationResult;

import org.json.JSONObject;

public class NSRActivityRecognitionService extends IntentService {
	public static final String ACTION = "NSRActivityRecognition";

	public NSRActivityRecognitionService() {
		super("NSRDetectedActivities");
	}

	protected void onHandleIntent(Intent intent) {
		if (ActivityRecognitionResult.hasResult(intent)) {
			ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
			DetectedActivity mostProbable = result.getMostProbableActivity();
			try {
				String type = getType(mostProbable.getType());
				Log.d(NSR.TAG, "NSRActivityRecognitionService.." + type);
				JSONObject conf = NSR.getInstance(getApplicationContext()).getAuthSettings().getJSONObject("conf");
				Log.d(NSR.TAG, "confidence.." + mostProbable.getConfidence());
				Log.d(NSR.TAG, "confidence cut.." + conf.getJSONObject("activity").getInt("confidence"));
				if (!"".equals(type) && mostProbable.getConfidence() >= conf.getJSONObject("activity").getInt("confidence")) {
					JSONObject payload = new JSONObject();
					payload.put("type", type);
					payload.put("confidence", mostProbable.getConfidence());

					if (!payload.getString("type").equals(NSR.getInstance(getApplicationContext()).getData("lastActivity", ""))) {
						if ("still".equals(payload.getString("type"))) {
							if (!NSR.getInstance(getApplicationContext()).getStillPositionSent()) {
								JSONObject payloadPosition = new JSONObject();
								payloadPosition.put("still", 1);
								payloadPosition.put("latitude", NSR.getInstance(getApplicationContext()).getCurrentLocation().getDouble("latitude"));
								payloadPosition.put("longitude", NSR.getInstance(getApplicationContext()).getCurrentLocation().getDouble("longitude"));
								NSR.getInstance(getApplicationContext()).sendCustomEvent("position", payloadPosition);
							}
							NSR.getInstance(getApplicationContext()).setStillPositionSent(true);
							SystemClock.sleep(2000);
						}

						NSR.getInstance(getApplicationContext()).sendCustomEvent("activity", payload);
						NSR.getInstance(getApplicationContext()).setData("lastActivity", payload.getString("type"));
					}
				}
			} catch (Exception e) {
				Log.d(NSR.TAG, "NSRActivityRecognitionService " + e.toString());
			}
		}
	}

	private String getType(int type) {
		if (type == DetectedActivity.IN_VEHICLE)
			return "car";
		else if (type == DetectedActivity.ON_BICYCLE)
			return "bicycle";
		else if (type == DetectedActivity.ON_FOOT || type == DetectedActivity.WALKING)
			return "walk";
		else if (type == DetectedActivity.STILL)
			return "still";
		else if (type == DetectedActivity.RUNNING)
			return "run";
		else
			return "";
	}

}

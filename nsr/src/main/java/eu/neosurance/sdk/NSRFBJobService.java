package eu.neosurance.sdk;

import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

public class NSRFBJobService extends JobService {
	public static final String TAG = "NSRFBJobService";
	private NSRServiceTask serviceTask;

	@Override
	public boolean onStartJob(JobParameters jobParameters) {
		Log.d(NSR.TAG, "onStartJob");
		serviceTask = new NSRServiceTask(getApplicationContext());
		serviceTask.execute();
		return false;
	}

	@Override
	public boolean onStopJob(JobParameters jobParameters) {
		if (serviceTask != null) {
			serviceTask.shutDownRecognition();
			serviceTask.cancel(true);
		}
		return false;
	}
}

package eu.neosurance.sdk;


import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class NSRJobService extends JobService {
	public static int JOB_SERVICE_ID = 738;
	private NSRServiceTask serviceTask;

	public static void schedule(Context context, long intervalMillis) {
		JobInfo.Builder builder = new JobInfo.Builder(JOB_SERVICE_ID, new ComponentName(context, NSRJobService.class));
		builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
		builder.setMinimumLatency(intervalMillis);
		JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
		jobScheduler.schedule(builder.build());
	}

	public static void cancel(Context context) {
		JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
		jobScheduler.cancel(JOB_SERVICE_ID);
	}

	@Override
	public boolean onStartJob(JobParameters jobParameters) {
		Log.d(NSR.TAG, "onStartJob");

		//Context ctx = getApplicationContext();
		Context ctx = getBaseContext();

		try {
			JSONObject conf = NSR.getInstance(ctx).getAuthSettings().getJSONObject("conf");
			NSRJobService.schedule(ctx, conf.getInt("time") * 1000);
			serviceTask = new NSRServiceTask(ctx);
			serviceTask.execute();
		} catch (Exception e) {
			Log.d(NSR.TAG, e.toString());
		}
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

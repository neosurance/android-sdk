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


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class NSRJobService extends JobService {
    public static int JOB_SERVICE_ID = 738;
    private NSRServiceTask serviceTask;

    public static void schedule(Context context, long intervalMillis){
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_SERVICE_ID, new ComponentName(context, NSRJobService.class));
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setPeriodic(intervalMillis);
        jobScheduler.schedule(builder.build());
    }

    public static void cancel(Context context){
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(JOB_SERVICE_ID);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        serviceTask = new NSRServiceTask(this, jobParameters);
        serviceTask.execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if(serviceTask != null){
            serviceTask.cancel(true);
        }
        return false;
    }
}

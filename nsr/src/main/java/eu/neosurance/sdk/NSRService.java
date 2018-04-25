package eu.neosurance.sdk;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class NSRService extends IntentService {
    private NSRServiceTask serviceTask;

    public static void start(Context context){
        context.startService(new Intent(context, NSRService.class));
    }

    public NSRService() {
        super("nsr-service");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        serviceTask = new NSRServiceTask(this);
        serviceTask.execute();
    }
}

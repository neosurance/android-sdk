package eu.neosurance.sdk;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class NSRSync extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;

    public static void register(Context context, long intervalMillis){
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NSRSync.class);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), intervalMillis, pIntent);
    }

    public static void unregister(Context context){
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NSRSync.class);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarm.cancel(pIntent);
    }

    public void onReceive(Context context, Intent intent) {
        NSRService.start(context);
    }
}

package eu.neosurance.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class NSRSync extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;

    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, NSRService.class));
    }
}

package eu.neosurance.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupBootReceiver extends BroadcastReceiver {

	public void onReceive(Context context, Intent intent) {
		Log.d(NSR.TAG,"StartupBootReceiver...");
		NSR.getInstance(context).bootCompleted();
	}
}

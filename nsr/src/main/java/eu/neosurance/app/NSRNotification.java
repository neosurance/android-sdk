package eu.neosurance.app;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;

import cz.msebera.android.httpclient.Header;

public class NSRNotification {

    public static  void sendNotification(final Context ctx, final String title, final String message, String imageUrl){
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(imageUrl, new FileAsyncHttpResponseHandler(ctx) {
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                Log.d("nsr", throwable.toString() );
            }
            public void onSuccess(int statusCode, Header[] headers, File response) {
                if(response != null || response.exists()){
                    NSRNotification.sendNotification(ctx, title, message, BitmapFactory.decodeFile(file.getAbsolutePath()));
                }
            }
        });

    }

    public static void sendNotification(final Context ctx, final String title, final String message, final String imageUrl, final PendingIntent pendingIntent){
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(imageUrl, new FileAsyncHttpResponseHandler(ctx) {
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                Log.d("nsr", throwable.toString() );
            }
            public void onSuccess(int statusCode, Header[] headers, File response) {
                if(response != null || response.exists()){
                    NSRNotification.sendNotification(ctx, title, message, BitmapFactory.decodeFile(file.getAbsolutePath()), pendingIntent);
                }
            }
        });
    }

    public static void sendNotification(Context ctx, String title, String message, Bitmap image){
        Notification.Builder notification = NSRNotification.buildNotification(ctx, R.drawable.nsr_logo, title, message);
        notification.setLargeIcon(image);
        //notification.setStyle(new Notification.BigPictureStyle().bigPicture(image));
        showNotification(ctx, notification);
    }

    public static void sendNotification(Context ctx, String title, String message, Bitmap image, PendingIntent pendingIntent){
        Notification.Builder notification = NSRNotification.buildNotification(ctx, R.drawable.nsr_logo, title, message);
        notification.setLargeIcon(image);
        //notification.setStyle(new Notification.BigPictureStyle().bigPicture(image));//.setSummaryText(message));
        notification.setContentIntent(pendingIntent);
        showNotification(ctx, notification);
    }

    public static void sendNotification(Context ctx, String title, String message, PendingIntent pendingIntent){
        Notification.Builder notification = NSRNotification.buildNotification(ctx, R.drawable.nsr_logo, title, message);
        notification.setContentIntent(pendingIntent);
        showNotification(ctx, notification);
    }

    public static void sendNotification(Context ctx, String title, String message){
        showNotification(ctx, NSRNotification.buildNotification(ctx, R.drawable.nsr_logo, title, message));
    }

    private static Notification.Builder buildNotification(Context ctx, int icon, String title, String message){
        Notification.Builder notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            notification = new Notification.Builder(ctx)
                    .setSmallIcon(icon)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new Notification.BigTextStyle().bigText(message))
                    .setLights(Color.BLUE, 500, 500)
                    .setVibrate(new long[] {500,500,500})
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setPriority(Notification.PRIORITY_HIGH)

                    //.setSound(Uri.parse("android.resource://" + activity.getPackageName() + "/" + R.raw.push))
                    .setAutoCancel(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification.setChannelId("nsr");
        }
        return notification;
    }

    private static void showNotification(Context ctx, Notification.Builder notification){
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel("nsr", "nsrchannel", NotificationManager.IMPORTANCE_HIGH));
        }
        notificationManager.notify( (int) System.currentTimeMillis(), notification.build());
    }
}

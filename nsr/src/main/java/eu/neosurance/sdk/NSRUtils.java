package eu.neosurance.sdk;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NSRUtils {

    public static JSONObject makeEvent(String name, JSONObject payload) throws Exception {
        JSONObject event = new JSONObject();
        event.put("event", name);
        event.put("timezone", TimeZone.getDefault().getID());
        event.put("event_time", System.currentTimeMillis());
        event.put("payload", payload);
        return event;
    }

    public static int tokenRemainingSeconds(JSONObject authSettings) throws Exception {
        if(!authSettings.has("auth")) {
            return -1;
        }
        long expire = authSettings.getJSONObject("auth").getLong("expire")/1000;
        long now = System.currentTimeMillis()/1000;
        return (int)(expire-now);
    }

    public static Address getAddress(Context ctx, double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(ctx, Locale.getDefault());
            return geocoder.getFromLocation(lat, lng, 1).get(0);
        } catch (Exception error) {
        }
        return null;
    }

    public static boolean addPermission(Context ctx, List<String> permissionsList, String permission) {
        if (ActivityCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            return true;
        }
        return false;
    }

    public interface NSRPictureProcessed {
        public void onStart();
        public void onSuccess(String base64);
    }
}

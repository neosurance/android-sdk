package eu.neosurance.app;

import org.json.JSONObject;

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
}

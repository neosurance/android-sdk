package eu.neosurance.sdk;

import org.json.JSONObject;

public interface NSRSecurityResponse {
    public void completionHandler(JSONObject response, String error) throws Exception;
}
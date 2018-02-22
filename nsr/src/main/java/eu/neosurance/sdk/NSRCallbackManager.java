package eu.neosurance.sdk;

import android.content.Intent;

public interface NSRCallbackManager {
    public boolean onActivityResult(int requestCode, int resultCode, Intent data);
    public static class Factory {
        public static NSRCallbackManager create() {
            return new NSRCallbackManagerImpl();
        }
    }
}

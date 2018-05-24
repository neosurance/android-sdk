# NeosuranceSDK

[![](https://jitpack.io/v/neosurance/android-sdk.svg)](https://jitpack.io/#neosurance/android-sdk)

- Collects info from device sensors and from the hosting app
- Exchanges info with the AI engines
- Sends the push notification
- Displays a landing page
- Displays the list of the purchased policies

## Example

To run the example project, clone the repo, and build it.

## Requirements


```xml
	<uses-permission android:name="android.permission.WAKE_LOCK" />
	<uses-permission android:name="android.permission.INTERNET" />
	<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
	<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
	<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
	<uses-permission android:name="android.permission.WRITE_SETTINGS" />
	<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
	<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
	<uses-permission android:name="android.permission.CAMERA" />
	<uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION"/>


	...


	<activity
		android:name="eu.neosurance.sdk.NSRActivityWebView"
		android:theme="@style/AppTheme.NoActionBar"
		android:configChanges="orientation|screenSize|keyboardHidden"
		android:screenOrientation="portrait">
	</activity>
	<service
            android:name="eu.neosurance.sdk.NSRActivityRecognitionService"
            android:exported="false"/>
        <service
            android:name="eu.neosurance.sdk.NSRService"
            android:exported="false"/>
        <service
            android:name="eu.neosurance.sdk.NSRJobService"
            android:exported="false"
            android:permission="android.permission.BIND_JOB_SERVICE"/>
        <receiver
            android:name="eu.neosurance.sdk.NSRSync"
            android:process=":remote" />
	<provider
            android:name="android.support.v4.content.FileProvider"
            android:authorities="[your.package].provider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths"/>
        </provider>
```


## Installation

NeosuranceSDK is available through [jitpack](https://jitpack.io/). To install
it, simply add the following line to your project:


Add it in your root build.gradle at the end of repositories:

```gradle
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```  

Step 2. Add the dependency

```gradle
	dependencies {
		compile 'com.github.neosurance:android-sdk:1.3.16'
	}
```


1. Init

```java
	JSONObject configuration = new JSONObject();
	configuration.put("base_url", "https://sandbox.neosurancecloud.net/sdk/api/v1.0/");
	configuration.put("code", "xxxx");
	configuration.put("secret_key", "xxxx");
	NSR.getInstance(this).setup(configuration);
	NSRCallbackManager callbackManager = NSRCallbackManager.Factory.create();
```


2. Request Permissions

```java	
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		callbackManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
```

3. setUser

```java
	NSRUser user = new NSRUser();
	user.setEmail("jhon.doe@acme.com");
	user.setCode("jhon.doe@acme.com");
	user.setFirstName("Jhon");
	user.setLastName("Doe");
	NSR.getInstance(this).registerUser(user);
```

4. showApp

```java
	NSR.getInstance(this).showApp();
```

5. customEvent

```java          
	NSR.getInstance(this).sendCustomEvent(String name, JSONObject payload);
	//position example
	JSONObject payload = new JSONObject();
	payload.put("latitude", latitude);
	payload.put("longitude", longitude);
	NSR.getInstance(this).sendCustomEvent("position", payload);
```

6. Base64Image

```java     
	NSR.getInstance(this).takePicture();
	
	NSR.getInstance(this).registerCallback(callbackManager, new NSRBase64Image.Callback() {
		public void onSuccess(String base64Image) {
		}
		public void onCancel() {
		}
		public void onError() {
		}
	});
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        	callbackManager.onActivityResult(requestCode, resultCode, data);
    	}
```
7. SecurityDelegate

```java     
	NSR.getInstance(this).setSecurityDelegate(... your security delegate);
```


## Author

info@neosurance.eu

## License

NeosuranceSDK is available under the MIT license. See the LICENSE file for more info.

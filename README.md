# NeosuranceSDK

[![](https://jitpack.io/v/clickntap/android-neosurance-sdk.svg)](https://jitpack.io/#clickntap/android-neosurance-sdk)

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
	<uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION"/>
	
	...
	
	<activity
            android:name="eu.neosurance.app.NSRActivityWebView"
            android:theme="@style/AppTheme.NoActionBar"
            android:configChanges="orientation|screenSize|keyboardHidden"
            android:screenOrientation="portrait">
        </activity>
        <service
            android:name="eu.neosurance.app.NSRActivityRecognitionService"
            android:exported="false">
	</service>
        <service
            android:name="eu.neosurance.app.NSRJobService"
            android:permission="android.permission.BIND_JOB_SERVICE"
            android:exported="false">
	</service>
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
	        compile 'com.github.clickntap:android-neosurance-sdk:1.0.0'
	}
```


1. Init

```java
    NSMutableDictionary* settings = [[NSMutableDictionary alloc] init];
    [settings setObject:@"https://sandbox.neosurancecloud.net/sdk/api/v1.0/" forKey:@"base_url"];
    [settings setObject:@"xxxx" forKey:@"code"];
    [settings setObject:@"xxxx" forKey:@"secret_key"];
    [[NeosuranceSDK sharedInstance] setupWithDictionary:settings navigationController:navigationController];
    [[NeosuranceSDK sharedInstance] stayInBackground];
```
2. setUser

```java
    NSRUser* user = [[NSRUser alloc] init];
    user.email = @"jhon.doe@acme.com";
    user.code = @"jhon.doe@acme.com";
    user.firstname = @"Jhon";
    user.lastname = @"Doe";
    [[NeosuranceSDK sharedInstance] registerUser:user];
```

## Author

Giovanni Tigli, giovanni.tigli@neosurance.eu
Tonino Mendicino, tonino.mendicino@clickntap.com

## License

NeosuranceSDK is available under the MIT license. See the LICENSE file for more info.


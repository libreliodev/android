package com.librelio.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.librelio.LibrelioApplication;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.niveales.wind.R;

import java.io.IOException;

public class GooglePlayServicesUtils {

	private static final String TAG = "GooglePlayServicesUtils";
	private static final String PROPERTY_APP_VERSION = "gcm_app_version";
	private static final String PROPERTY_REG_ID = "gcm_reg_id";

	public static boolean checkPlayServices(Context context) {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(context);
		if (resultCode != ConnectionResult.SUCCESS) {
			// Uncomment this to show a dialog to install/update Google Play Services
			
			// if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
			// GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
			// PLAY_SERVICES_RESOLUTION_REQUEST).show();
			// } else {
			// Log.i(TAG, "This device is not supported.");
			// activity.finish();
			// }
			return false;
		}
		return true;
	}
	
	/**
	 * Gets the current registration ID for application on GCM service.
	 * <p>
	 * If result is empty, the app needs to register.
	 *
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	public static String getRegistrationId(Context context) {
	    final SharedPreferences prefs = GooglePlayServicesUtils.getGCMPreferences(context);
	    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	    if (registrationId.isEmpty()) {
	        Log.i(TAG, "Registration not found.");
	        return "";
	    }
	    // Check if app was updated; if so, it must clear the registration ID
	    // since the existing regID is not guaranteed to work with the new
	    // app version.
	    int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	    int currentVersion = getAppVersion(context);
	    if (registeredVersion != currentVersion) {
	        Log.i(TAG, "App version changed.");
	        return "";
	    }
	    return registrationId;
	}
	
	private static SharedPreferences getGCMPreferences(Context context) {
	    return PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	private static int getAppVersion(Context context) {
	    try {
	        PackageInfo packageInfo = context.getPackageManager()
	                .getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        // should never happen
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}
	
	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	public static void registerInBackground(final Context context) {
	    new AsyncTask<Void, Void, String>() {
	        private GoogleCloudMessaging gcm;

			@Override
	        protected String doInBackground(Void... params) {
	            String msg = "";
	            try {
	                if (gcm == null) {
	                    gcm = GoogleCloudMessaging.getInstance(context);
	                }
	                String regid = gcm.register(context.getResources().getString(R.string.gcm_project_number));
	                msg = "Device registered, registration ID=" + regid;

	                // You should send the registration ID to your server over HTTP,
	                // so it can use GCM/HTTP or CCS to send messages to your app.
	                // The request to your server should be authenticated if your app
	                // is using accounts.
	                
	                sendRegistrationIdToBackend(context, regid);
	            } catch (IOException ex) {
	                msg = "Error :" + ex.getMessage();
	                // If there is an error, don't just keep trying to register.
	                // Require the user to click a button again, or perform
	                // exponential back-off.
	            }
	            Log.d(TAG, msg);
	            return msg;
	        }

	    }.execute(null, null, null);
	}
	
	private static void storeRegistrationId(Context context, String regId) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    int appVersion = getAppVersion(context);
	    Log.i(TAG, "Saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PROPERTY_REG_ID, regId);
	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
	    editor.apply();
	}
	
	private static void sendRegistrationIdToBackend(final Context context, final String regid) {
		final AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		params.put("platform", "android");
		params.put("client", LibrelioApplication.getClientName(context));
		params.put("app", LibrelioApplication.getMagazineName(context));
		params.put("deviceid", LibrelioApplication.getAndroidId(context));
		params.put("registrationid", regid);
		client.post("http://apns.librelio.com/apns/gcm.php", params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, String content) {
				super.onSuccess(statusCode, content);
                // Only persist the regid if sent to server successfully
				GooglePlayServicesUtils.storeRegistrationId(context, regid);
			}
			
			@Override
			public void onFailure(Throwable error, String content) {
				super.onFailure(error, content);
				Log.d(TAG, "Registering GCM regid with server failed");
			}
		});
	}

}

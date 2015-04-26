package com.librelio;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.librelio.activity.MuPDFActivity;
import com.librelio.storage.DataBaseHelper;
import com.librelio.utils.GooglePlayServicesUtils;
import com.librelio.utils.SystemHelper;
import com.niveales.wind.BuildConfig;
import com.niveales.wind.R;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import net.danlew.android.joda.JodaTimeAndroid;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import de.greenrobot.event.EventBus;
import io.fabric.sdk.android.Fabric;

public class LibrelioApplication extends Application {

	private static final String TAG = "LibrelioApplication";
	private static final String PATH_SEPARATOR = "/";
	
	private static String baseUrl;
	private static OkHttpClient client;
    private Tracker tracker;

	private BillingProcessor bp;

	@NonNull
	public BillingProcessor getBillingProcessor() {
		return bp;
	}

	@NonNull
	private static LibrelioApplication instance;

	public LibrelioApplication() {
		instance = this;
	}

	public static LibrelioApplication get() {
		return instance;
	}

    @Override
	public void onCreate() {
        super.onCreate();

		JodaTimeAndroid.init(this);

        if (!BuildConfig.DEBUG && BuildConfig.CRASHLYTICS_ENABLED) {
            Fabric.with(this, new Crashlytics());
        }

        baseUrl = "http://librelio-europe.s3.amazonaws.com/" + getClientName(this) + PATH_SEPARATOR + getMagazineName(this) + PATH_SEPARATOR;

//		baseUrl = "http://librelio-test.s3.amazonaws.com/" + getMagazineName(this) +
//                PATH_SEPARATOR;

        EventBus.builder().sendNoSubscriberEvent(false).installDefaultEventBus();

        registerForGCM();

		bp = new BillingProcessor(this, null,
				new BillingProcessor.IBillingHandler() {
					@Override
					public void onProductPurchased(String s, TransactionDetails transactionDetails) {

					}

					@Override
					public void onPurchaseHistoryRestored() {

					}

					@Override
					public void onBillingError(int i, Throwable throwable) {

					}

					@Override
					public void onBillingInitialized() {

					}
				});

		new Thread(new Runnable() {
			@Override
			public void run() {

				// Precache the subscription details
				bp.getSubscriptionListingDetails(getString(R.string.yearly_subs_code));
				bp.getSubscriptionListingDetails(getString(R.string.monthly_subs_code));
				//FIXME precache all the magazine prices
			}
		}).start();
    }

    public synchronized Tracker getTracker() {
        if (tracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            tracker = analytics.newTracker(getString(R.string.google_analytics));
            tracker.enableExceptionReporting(true);
            tracker.enableAutoActivityTracking(true);
            // Set the log level to verbose.
//            GoogleAnalytics.getInstance(this).getLogger()
//                    .setLogLevel(Logger.LogLevel.VERBOSE);
        }
        return tracker;
    }

	public static OkHttpClient getOkHttpClient() {
		if (client == null) {
			client = new OkHttpClient();
			// Fix for https://github.com/square/okhttp/issues/184
			// Although it should be fixed in OkHttp 2
			SSLContext sslContext;
			try {
				sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, null, null);
			} catch (GeneralSecurityException e) {
				throw new AssertionError(); // The system has no TLS. Just give
											// up.
			}
			client.setSslSocketFactory(sslContext.getSocketFactory());
			client.setFollowRedirects(false);
			client.setFollowSslRedirects(false);
//			client.interceptors().add(new LoggingInterceptor());
			client.networkInterceptors().add(new UserAgentInterceptor("Android"));
		}
		return client;
	}

	static class LoggingInterceptor implements Interceptor {
		@Override public Response intercept(Chain chain) throws IOException {
			Request request = chain.request();

			long t1 = System.nanoTime();
			Log.d("logging", String.format("Sending request %s on %s%n%s",
					request.url(), chain.connection(), request.headers()));

			Response response = chain.proceed(request);

			long t2 = System.nanoTime();
			Log.d("logging", String.format("Received response for %s in %.1fms%n%s",
					response.request().url(), (t2 - t1) / 1e6d, response.headers()));

			return response;
		}
	}

	static class UserAgentInterceptor implements Interceptor {

		private final String userAgent;

		public UserAgentInterceptor(String userAgent) {
			this.userAgent = userAgent;
		}

		@Override
		public Response intercept(Chain chain) throws IOException {
			Request originalRequest = chain.request();
			Request requestWithUserAgent = originalRequest.newBuilder()
					.removeHeader("User-Agent")
					.addHeader("User-Agent", userAgent)
					.build();
			return chain.proceed(requestWithUserAgent);
		}
	}

	private void registerForGCM() {
        // Check device for Play Services APK. If check succeeds, proceed with
        //  GCM registration.
        if (GooglePlayServicesUtils.checkPlayServices(getApplicationContext())) {
            String regid = GooglePlayServicesUtils.getRegistrationId(getApplicationContext());
            
            if (BuildConfig.DEBUG) {
            	Log.i(TAG, "current GCM RegistrationID = " + regid);
            }

            // if regid not stored in SharedPreferences then register for GCM
            if (regid.isEmpty()) {
                GooglePlayServicesUtils.registerInBackground(getApplicationContext());
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
	}


	public static void startPDFActivity(Context context, String filePath, String title, boolean showThumbnails){
		try{
			Uri uri = Uri.parse(filePath);
			Intent intent = new Intent(context,MuPDFActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			intent.setData(uri);
			intent.putExtra(DataBaseHelper.FIELD_TITLE, title);
			intent.putExtra(MuPDFActivity.SHOW_THUMBNAILS_EXTRA, showThumbnails);
			context.startActivity(intent);
		} catch (Exception e) {
			Log.e(TAG,"Problem with starting PDF-activity, path: "+filePath,e);
		}

	}

	public static boolean thereIsConnection(Context context) {
		
		if (SystemHelper.isEmulator(context)) {
			return true;
		}
		
		ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo i = conMgr.getActiveNetworkInfo();
		if (i == null) {
			return false;
		}
		if (!i.isConnected()) {
			return false;
		}
		if (!i.isAvailable()) {
			return false;
		}
		return true;
	}

	public static String getClientName(Context context){
		String packageName = context.getPackageName();
		int lastIndexOfDot = packageName.lastIndexOf(".");
		return packageName.substring(packageName.lastIndexOf(".", lastIndexOfDot - 1) + 1, lastIndexOfDot);
	}
	
	public static String getMagazineName(Context context){
		String packageName = context.getPackageName();
		return packageName.substring(packageName.lastIndexOf(".") + 1);
	}

    public static String getServiceName(Context context){
        return context.getResources().getString(R.string.user_service);
    }
	
	public static String getUrlString(String fileName){
		return PATH_SEPARATOR + fileName;
	}
	
	public static String getUrlString(Context context, String fileName){
		return LibrelioApplication.getClientName(context) + PATH_SEPARATOR 
		+ LibrelioApplication.getMagazineName(context) + PATH_SEPARATOR + fileName;
	}

	public static String getYearlySubsCode(Context context){
		return context.getResources().getString(R.string.yearly_subs_code);
	}

	public static String getMonthlySubsCode(Context context){
		return context.getResources().getString(R.string.monthly_subs_code);
	}
	
	public static boolean isEnableCodeSubs(Context context){
		return !context.getResources().getString(R.string.code_service).isEmpty();
	}

    public static boolean isEnableUsernamePasswordLogin(Context context){
        return !context.getResources().getString(R.string.user_service).isEmpty();
    }

	public static String getServerUrl(Context context){
		return context.getString(R.string.server_url);
	}

	public static String getAmazonServerUrl(){
		return baseUrl;
	}

    public static String getAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}

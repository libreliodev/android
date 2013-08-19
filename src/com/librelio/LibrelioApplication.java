package com.librelio;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import android.widget.Toast;
import com.librelio.activity.MuPDFActivity;
import com.librelio.base.IBaseContext;
import com.librelio.model.Magazine;
import com.librelio.utils.SystemHelper;
import com.niveales.wind.R;
import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formKey = "",
        mailTo = "android@librelio.com",
        customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT },
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text)
public class LibrelioApplication extends Application {
	public static final String SUBSCRIPTION_YEAR_KEY = "yearlysubscription";
	public static final String SUBSCRIPTION_MONTHLY_KEY = "monthlysubscription";

	private static final String TAG = "LibrelioApplication";
	private static final String PATH_SEPARATOR = "/";
	
//	private static final String SERVER_URL = "http://php.netcook.org/librelio-server/downloads/android_verify.php";
	
	private static String baseUrl;

	@Override
	public void onCreate() {
        super.onCreate();
        ACRA.init(this);

        baseUrl = "http://librelio-europe.s3.amazonaws.com/" + getClientName(this) + PATH_SEPARATOR + getMagazineName(this) + PATH_SEPARATOR;


//		baseUrl = "http://librelio-test.s3.amazonaws.com/" + getMagazineName(this) +
//                PATH_SEPARATOR;

    }


	public static void startPDFActivity(Context context, String filePath, String title){
		try{
			Uri uri = Uri.parse(filePath);
			Intent intent = new Intent(context,MuPDFActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			intent.setData(uri);
			intent.putExtra(Magazine.FIELD_TITLE, title);
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
		return context.getResources().getString(R.string.client_name);
	}
	
	public static String getMagazineName(Context context){
		return context.getResources().getString(R.string.magazine_name);
	}

    public static String getServiceName(Context context){
        return context.getResources().getString(R.string.service_name);
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
		return context.getResources().getBoolean(R.bool.enable_code_subs);
	}

    public static boolean isEnableUsernamePasswordLogin(Context context){
        return context.getResources().getBoolean(R.bool.enable_username_password_login);
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

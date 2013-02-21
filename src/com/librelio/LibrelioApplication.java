package com.librelio;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import com.librelio.activity.MuPDFActivity;
import com.librelio.utils.SystemHelper;
import com.niveales.wind.R;

public class LibrelioApplication extends Application {
	public static final String SUBSCRIPTION_YEAR_KEY = "yearlysubscription";
	public static final String SUBSCRIPTION_MONTHLY_KEY = "monthlysubscription";

	private static final String TAG = "LibrelioApplication";
	private static final String PATH_SEPARATOR = "/";
	
//	private static final String SERVER_URL = "http://php.netcook.org/librelio-server/downloads/android_verify.php";
	
	private static String baseUrl;

	@Override
	public void onCreate() {
		String clientName = getClientName(this);
		String magazineName = getMagazineName(this);
		baseUrl = "http://librelio-europe.s3.amazonaws.com/" + clientName + PATH_SEPARATOR + magazineName + PATH_SEPARATOR;
		super.onCreate();
	}

	public static void startPDFActivity(Context context, String filePath){
		try{
			Uri uri = Uri.parse(filePath);
			Intent intent = new Intent(context,MuPDFActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			intent.setData(uri);
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
	
	public static String getUrlString(String fileName){
		return PATH_SEPARATOR + fileName;
	}
	
	public static String getUrlString(Context context, String fileName){
		return LibrelioApplication.getClientName(context) + PATH_SEPARATOR 
		+ LibrelioApplication.getMagazineName(context) + PATH_SEPARATOR + fileName;
	}

	public static boolean isEnableYearlySubs(Context context){
		String data = context.getResources().getString(R.string.enable_yearly_subs);
		if(data.equalsIgnoreCase("true")){
			return true;
		} else {
			return false;
		}
	}

	public static boolean isEnableMonthlySubs(Context context){
		String data = context.getResources().getString(R.string.enable_monthly_subs);
		if(data.equalsIgnoreCase("true")){
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean isEnableCodeSubs(Context context){
		return context.getResources().getBoolean(R.bool.enable_code_subs);
	}

	public static String getServerUrl(Context context){
		return context.getString(R.string.server_url);
	}

	public static String getAmazonServerUrl(){
		return baseUrl;
	}

}

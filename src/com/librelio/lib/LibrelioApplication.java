package com.librelio.lib;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import com.artifex.mupdf.MuPDFActivity;
import com.niveales.wind.R;

public class LibrelioApplication extends Application {
	private static final String TAG = "LibrelioApplication";
	private static final String META_DATA_CLIENT_NAME_KEY = "ClientName";
	private static final String META_DATA_MAGAZINE_NAME_KEY = "MagazineName";
	private static final String META_DATA_EARLY_SUBS_KEY = "EnableYearlySubs";
	private static final String META_DATA_MONTHLY_SUBS_KEY = "EnableMonthlySubs";
	public static final String SUBSCRIPTION_YEAR_KEY = "yearlysubscription";
	public static final String SUBSCRIPTION_MONTHLY_KEY = "monthlysubscription";
	public static String BASE_URL;
	
	@Override
	public void onCreate() {
		String clientName = getClientName(this);
		String magazineName = getMagazineName(this);
		BASE_URL = "http://librelio-europe.s3.amazonaws.com/" + 
				clientName + "/" + magazineName + "/";
		super.onCreate();
	}
	
	public static void startPDFActivity(Context context,String filePath){
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
		ApplicationInfo ai = null;
		try {
			ai = context.getPackageManager().getApplicationInfo(context.getPackageName(),PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			Log.e(TAG,"Get mata-data error(getClientName)!!!",e);
		}
	    return (String)ai.metaData.get(META_DATA_CLIENT_NAME_KEY);
	}
	
	public static String getMagazineName(Context context){
		ApplicationInfo ai = null;
		try {
			ai = context.getPackageManager().getApplicationInfo(context.getPackageName(),PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			Log.e(TAG,"Get mata-data error(getMagazineName)!!!",e);
		}
	    return (String)ai.metaData.get(META_DATA_MAGAZINE_NAME_KEY);
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
}

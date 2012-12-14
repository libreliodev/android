package com.librelio.lib;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.artifex.mupdf.MuPDFActivity;
import com.niveales.wind.R;

public class LibrelioApplication extends Application {
	private static final String TAG = "LibrelioApplication";
	public static String BASE_URL;
	public static String APP_DIRECTORY = "/data/data/com.niveales.wind/librelio/";
	
	@Override
	public void onCreate() {
		String clientName = getResources().getString(R.string.client_name);
		String magazineName = getResources().getString(R.string.magazine_name);
		BASE_URL = "http://librelio-europe.s3.amazonaws.com/" + 
				clientName + "/" + magazineName + "/";
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
			Log.e(TAG,"Problem with starting PDG-activity, path: "+filePath,e);
		}

	}
}

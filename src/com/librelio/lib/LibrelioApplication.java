package com.librelio.lib;

import com.niveales.wind.R;

import android.app.Application;
import android.os.Environment;

public class LibrelioApplication extends Application {
	public static String BASE_URL;
	public static String appDirectory = Environment.getExternalStorageDirectory()+"/librelio/";
	
	@Override
	public void onCreate() {
		String clientName = getResources().getString(R.string.client_name);
		String magazineName = getResources().getString(R.string.magazine_name);
		BASE_URL = "http://librelio-europe.s3.amazonaws.com/" + 
				clientName + "/" + magazineName + "/";
		super.onCreate();
	}
}

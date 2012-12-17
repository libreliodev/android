package com.librelio.base;

import com.librelio.lib.LibrelioApplication;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;

public class BaseService extends Service implements iBaseContext{

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public String getInternalPath() {
		return LibrelioApplication.APP_DIRECTORY;
	}

	@Override
	public String getExternalPath() {
		return Environment.getExternalStorageDirectory() + "/librelio/";
	}

	@Override
	public String getStoragePath() {
		return getInternalPath();
	}

}

package com.librelio.base;

import android.app.Service;
import android.os.Environment;

import com.librelio.lib.LibrelioApplication;

abstract public class BaseService extends Service implements IBaseContext {

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

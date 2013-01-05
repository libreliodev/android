package com.librelio.base;

import com.librelio.LibrelioApplication;

import android.app.Service;
import android.os.Environment;

abstract public class BaseService extends Service implements IBaseContext {

	@Override
	public String getInternalPath() {
		return getDir("librelio", MODE_PRIVATE).getAbsolutePath();
	}

	@Override
	public String getExternalPath() {
		return Environment.getExternalStorageDirectory() + "/librelio/";
	}

	@Override
	public String getStoragePath() {
		return getInternalPath();
	}

	@Override
	public boolean isOnline() {
		return LibrelioApplication.thereIsConnection(getBaseContext());
	}
}

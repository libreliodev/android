package com.librelio.base;

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

}

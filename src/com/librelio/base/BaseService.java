package com.librelio.base;

import com.librelio.LibrelioApplication;
import com.niveales.wind.R;

import android.app.Service;
import android.content.SharedPreferences;
import android.os.Environment;

abstract public class BaseService extends Service implements IBaseContext {

	private SharedPreferences sharedPreferences;

	@Override
	public String getInternalPath() {
		return getDir("librelio", MODE_PRIVATE).getAbsolutePath() + "/";
	}

	@Override
	public String getExternalPath() {
		return Environment.getExternalStorageDirectory() + "/librelio/";
	}

	@Override
	public String getStoragePath() {
		if (USE_INTERNAL_STORAGE) {
			return getInternalPath();
		} else {
			return getExternalPath();
		}
	}

	@Override
	public boolean isOnline() {
		return LibrelioApplication.thereIsConnection(getBaseContext());
	}

	@Override
	public SharedPreferences getPreferences() {
		if (null == sharedPreferences) {
			sharedPreferences = getSharedPreferences(LIBRELIO_SHARED_PREFERENCES, MODE_PRIVATE); 
		}
		return sharedPreferences;
	}

	@Override
	public String getVideoTempPath() {
		return getExternalPath() + ".tmp.mp4";
	}

}

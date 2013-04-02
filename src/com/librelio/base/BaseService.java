package com.librelio.base;

import java.util.Random;

import android.app.Service;
import android.content.SharedPreferences;
import android.os.Environment;

import com.librelio.LibrelioApplication;
import com.niveales.wind.R;

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
		if (getResources().getBoolean(R.bool.use_internal_storage)) {
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
		return getExternalPath() + ".f" + new Random().nextLong() + "-video.mp4";
	}

}

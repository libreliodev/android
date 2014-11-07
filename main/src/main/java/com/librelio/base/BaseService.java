package com.librelio.base;

import android.app.Service;
import android.content.SharedPreferences;
import com.librelio.LibrelioApplication;

abstract public class BaseService extends Service implements IBaseContext {

	private SharedPreferences sharedPreferences;

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

}

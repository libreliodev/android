package com.librelio.base;

import android.content.SharedPreferences;

public interface IBaseContext {

	String LIBRELIO_SHARED_PREFERENCES = "LIBRELIO_SHARED_PREFERENCES";
	String LAST_UPDATE_PREFERENCES_KEY = "LAST_UPDATE_PREFERENCES_KEY";

	boolean isOnline();

	SharedPreferences getPreferences();
}

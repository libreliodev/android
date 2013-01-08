package com.librelio.base;

import android.content.SharedPreferences;

public interface IBaseContext {

	String LIBRELIO_SHARED_PREFERENCES = "LIBRELIO_SHARED_PREFERENCES";

	String getInternalPath();

	String getExternalPath();

	String getStoragePath();

	boolean isOnline();

	SharedPreferences getPreferences();

	String getVideoTempPath();
}

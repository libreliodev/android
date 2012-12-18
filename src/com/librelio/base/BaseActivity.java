package com.librelio.base;

import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

abstract public class BaseActivity extends Activity implements IBaseContext{
	public static final String BROADCAST_ACTION = "com.librelio.lib.service.broadcast";

	/**
	 * The receiver for stop progress in action bar
	 */
	protected BroadcastReceiver updateProgressStop = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			setProgressBarIndeterminateVisibility(false);
		}
	};

	@Override
	public String getInternalPath() {
		return getDir("librelio", MODE_PRIVATE).getAbsolutePath();
	}

	@Override
	public String getExternalPath() {
		return Environment.getExternalStorageDirectory() + "/librelio/";
	}
	@Override
	public String getStoragePath(){
		return getInternalPath();
	}

	/**
	 * Replaces the language and/or country of the device into the given string.
	 * The pattern "%lang%" will be replaced by the device's language code and
	 * the pattern "%region%" will be replaced with the device's country code.
	 * 
	 * @param str
	 *            the string to replace the language/country within
	 * @return a string containing the local language and region codes
	 */
	protected String replaceLanguageAndRegion(String str) {
		// Substitute language and or region if present in string
		if (str.contains("%lang%") || str.contains("%region%")) {
			Locale locale = Locale.getDefault();
			str = str.replace("%lang%", locale.getLanguage().toLowerCase());
			str = str.replace("%region%", locale.getCountry().toLowerCase());
		}
		return str;
	}
	
	protected int getUpdatePeriod() {
		return 1800000;
	}
}

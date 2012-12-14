package com.librelio.activity;

import com.librelio.lib.LibrelioApplication;

import android.app.Activity;
import android.os.Environment;

abstract public class BaseActivity extends Activity {

	protected String getInternalPath() {
		return LibrelioApplication.APP_DIRECTORY;
	}

	protected String getExternalPath() {
		return Environment.getExternalStorageDirectory() + "/librelio/";
	}
}

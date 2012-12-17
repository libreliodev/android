package com.librelio.base;

import com.librelio.lib.LibrelioApplication;

import android.app.Activity;
import android.os.Environment;

abstract public class BaseActivity extends Activity implements iBaseContext{

	@Override
	public String getInternalPath() {
		return LibrelioApplication.APP_DIRECTORY;
	}
	@Override
	public String getExternalPath() {
		return Environment.getExternalStorageDirectory() + "/librelio/";
	}
	@Override
	public String getStoragePath(){
		return getInternalPath();
	}
}

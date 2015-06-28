package com.librelio.activity;

import android.os.Bundle;

import com.librelio.base.BaseActivity;
import com.librelio.utils.SystemHelper;

public abstract class AbstractLockRotationActivity extends BaseActivity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//noinspection ResourceType
		setRequestedOrientation(
				SystemHelper.getScreenOrientation(this));
	}	
	
}

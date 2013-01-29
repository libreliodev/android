package com.librelio.activity;

import com.librelio.base.BaseActivity;

public abstract class AbstractLockRotationActivity extends BaseActivity{
	private boolean rotationWasDisabled = false;

	@Override
	protected void onResume() {
		int rotationEnable = android.provider.Settings.System.getInt(
				getContentResolver(), android.provider.Settings.System.ACCELEROMETER_ROTATION, 1);
		if(rotationEnable == 0){
			rotationWasDisabled = true;
		} else {
			enableRotation(false);
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		if(!rotationWasDisabled){
			enableRotation(true);
		}
		super.onPause();
	}
}

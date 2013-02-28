package com.librelio.animation;

import android.content.Intent;

import com.librelio.base.BaseActivity;
import com.niveales.wind.R;

public class Rotation3dActivity extends BaseActivity {

	@Override
	protected void onResume() {
			// animateIn this activity
			ActivitySwitcher.animationIn(findViewById(R.id.container), getWindowManager());
			super.onResume();
	}
	
	@Override
	public void finish() {
		// we need to override this to performe the animtationOut on each finish.
		ActivitySwitcher.animationOut(findViewById(R.id.container), getWindowManager(), new ActivitySwitcher.AnimationFinishedListener() {
			@Override
			public void onAnimationFinished() {
				Rotation3dActivity.super.finish();
				// disable default animation
				overridePendingTransition(0, 0);
			}
		});
	}
	
	protected void animatedStartActivity(final Intent intent) {
		// we only animateOut this activity here.
		// The new activity will animateIn from its onResume() - be sure to implement it.
		// disable default animation for new intent
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		ActivitySwitcher.animationOut(findViewById(R.id.container), 
				getWindowManager(), new ActivitySwitcher.AnimationFinishedListener() {
			@Override
			public void onAnimationFinished() {
				startActivity(intent);
			}
		});
	}
	
}

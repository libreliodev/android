package com.librelio.activity;

import android.os.Bundle;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd;
import com.niveales.wind.R;

import java.util.Timer;
import java.util.TimerTask;

public class StartupActivity extends AbstractLockRotationActivity {

	private static int DEFAULT_DELAY = 1000;

	private Timer mShowMainActivityTimer;
	private PublisherInterstitialAd interstitial;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.startup);

		// Create the interstitial.
		interstitial = new PublisherInterstitialAd(this);
		interstitial.setAdUnitId(getString(R.string.dfp_prefix) + "startup");

		interstitial.setAdListener(new AdListener() {
			@Override
			public void onAdLoaded() {
				super.onAdLoaded();
				// Should display ad after a slight delay for splash screen
				displayInterstitial();
			}

			@Override
			public void onAdClosed() {
				super.onAdClosed();
				startMainMagazineActivity();
			}

			@Override
			public void onAdFailedToLoad(int errorCode) {
				super.onAdFailedToLoad(errorCode);
				showMagazineAfterDelay(DEFAULT_DELAY);
			}

			@Override
			public void onAdLeftApplication() {
				super.onAdLeftApplication();
			}

			@Override
			public void onAdOpened() {
				super.onAdOpened();
			}
		});

		// Create ad request.
		PublisherAdRequest adRequest = new PublisherAdRequest.Builder().build();

		// Begin loading your interstitial.
		interstitial.loadAd(adRequest);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	protected void showMagazineAfterDelay(int delay) {
		if (mShowMainActivityTimer != null) {
			mShowMainActivityTimer.cancel();
		}
		mShowMainActivityTimer = new Timer();
		mShowMainActivityTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				startMainMagazineActivity();
			}
		}, delay);
	}

	void startMainMagazineActivity() {
		overridePendingTransition(android.R.anim.accelerate_interpolator, android.R.anim.slide_out_right);
		startActivity(MainTabsActivity.getIntent(this));
		finish();
	}

	// Invoke displayInterstitial() when you are ready to display an interstitial.
	public void displayInterstitial() {
		if (interstitial.isLoaded()) {
			interstitial.show();
		}
	}
}
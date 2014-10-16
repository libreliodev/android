/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.librelio.activity;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;

import com.google.analytics.tracking.android.EasyTracker;
import com.librelio.LibrelioApplication;
import com.librelio.animation.DisplayNextView;
import com.librelio.animation.Rotate3dAnimation;
import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;
import com.niveales.wind.BuildConfig;
import com.niveales.wind.R;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/**
 * The start point for Librelio application (Splash-screen)
 *
 * @author Nikolay Moskvin <moskvin@netcook.org>
 *
 */
public class StartupActivity extends AbstractLockRotationActivity {
	private static final String TAG = "StartupActivity";
	private static final String PARAM_CLIENT = "@client";
	private static final String PARAM_APP = "@app";
	private static final String PLIST_DELAY = "Delay";
	private static final String PLIST_LINK = "Link";
    private static int DEFAULT_ADV_DELAY = 1000;
	private ImageView startupImage;
	private ImageView advertisingImage;
	private boolean advertisingClickPerformed = false;
	private boolean isFirstImage = true;
	private Timer mStartupAdsTimer;
    private Bitmap adImage;

	OkHttpClient client = new OkHttpClient();

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.startup);

		startupImage = (ImageView) findViewById(R.id.startup_image);
		advertisingImage = (ImageView) findViewById(R.id.advertising_image);

	    loadAdvertisingImage();
    }

	private void loadAdvertisingImage() {

		client.setReadTimeout(2, TimeUnit.SECONDS);
		Request request = new Request.Builder().url(getAdvertisingImageURL())
				.build();

		client.newCall(request).enqueue(new Callback() {

			@Override
			public void onResponse(Response response) throws IOException {
				if (response.code() == 200) {
					EasyTracker.getTracker().sendView(
							"Interstitial/"
									+ FilenameUtils
											.getName(getAdvertisingImageURL()));
					byte[] bytes = response.body().bytes();
					if (bytes != null) {
						adImage = BitmapFactory.decodeByteArray(bytes, 0,
								bytes.length);
					}
					loadAdvertisingLinkAndDisplayAdvertising();
				} else {
					showMagazineAfterDelay(DEFAULT_ADV_DELAY);
				}
				response.body().close();
			}

			@Override
			public void onFailure(Request request, Throwable throwable) {
				showMagazineAfterDelay(DEFAULT_ADV_DELAY);
			}
		});
	}

    private void loadAdvertisingLinkAndDisplayAdvertising() {
    	
    	Request request = new Request.Builder().url(getAdvertisingLinkURL()).build();
    	
    	client.newCall(request).enqueue(new Callback() {
    		
			@Override
			public void onResponse(Response response) throws IOException {
				if (response.code() == 200) {
				 PListXMLHandler handler = new PListXMLHandler();
	                PListXMLParser parser = new PListXMLParser();
	                parser.setHandler(handler);
	                parser.parse(response.body().string());
	                PList list = ((PListXMLHandler)parser.getHandler()).getPlist();
	                if (list != null){
	                    Dict dict = (Dict) list.getRootElement();
	                    String delay = dict.getString(PLIST_DELAY).getValue().toString();
	                    String link = dict.getString(PLIST_LINK).getValue().toString();
	                    if (adImage != null) {
	                    	runOnUiThread(new Runnable() {
								@Override
								public void run() {
			                        advertisingImage.setImageBitmap(adImage);
			                        if (isFirstImage) {
			                            applyRotation(0, 90);
			                            isFirstImage = !isFirstImage;
			                        } else {
			                            applyRotation(0, -90);
			                            isFirstImage = !isFirstImage;
			                        }
									
								}
							});
	                    }
	                    setOnAdvertisingImageClickListener(link);
	                    showMagazineAfterDelay(Integer.valueOf(delay));
	                } else {
						showMagazineAfterDelay(DEFAULT_ADV_DELAY);	
					}
				} else {
					showMagazineAfterDelay(DEFAULT_ADV_DELAY);	
				}
				response.body().close();
			}
			
			@Override
			public void onFailure(Request request, Throwable throwable) {
				showMagazineAfterDelay(DEFAULT_ADV_DELAY);
			}
		});
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mStartupAdsTimer != null) {
            mStartupAdsTimer.cancel();
        }
    }

	private void applyRotation(float start, float end) {
		// Find the center of image
		final float centerX = startupImage.getWidth() / 2.0f;
		final float centerY = startupImage.getHeight() / 2.0f;

		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		final Rotate3dAnimation rotation =
				new Rotate3dAnimation(start, end, centerX, centerY, 0, false);
		rotation.setDuration(500);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new DisplayNextView(isFirstImage, startupImage, advertisingImage));

		if (isFirstImage){
			startupImage.startAnimation(rotation);
		} else {
			advertisingImage.startAnimation(rotation);
		}
	}

	protected void showMagazineAfterDelay(int delay) {
		mStartupAdsTimer = new Timer();
		mStartupAdsTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (!advertisingClickPerformed){
					startMainMagazineActivity();
				}
			}
		}, delay);
	}

	private void setOnAdvertisingImageClickListener(final String link){
		if (advertisingImage != null){
			advertisingImage.setOnTouchListener(new OnTouchListener() {
//				@Override
//				public void onClick(View v) {
//					advertisingClickPerformed = true;
//
//					startAdsActivity(link);
//				}

				@Override
				public boolean onTouch(View pView, MotionEvent pEvent) {
					if(pEvent.getAction() == MotionEvent.ACTION_DOWN) {
						// check the Y coordinates of the touch event
						float pY = pEvent.getY();
						int   pHeight = pView.getHeight();
						if(Math.round(pY/0.2) <= pHeight) {
							// Skip ads
							if(mStartupAdsTimer != null)
								mStartupAdsTimer.cancel();
							startMainMagazineActivity();
						} else {
							// Show ads
							if(mStartupAdsTimer != null)
								mStartupAdsTimer.cancel();
							startAdsActivity(link);
						}
					}
					return false;
				}
			});
		}
	}

	private String getAdvertisingImageURL() {
		String advertisingUrl = new StringBuilder(getString(R.string.get_advertising_image_url))
							.append(getString(R.string.get_advertising_image_end))
							.toString()
							.replace(PARAM_CLIENT, Uri.encode(LibrelioApplication.getClientName(self())))
							.replace(PARAM_APP, Uri.encode(LibrelioApplication.getMagazineName(self())));

		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Advertising imageurl: "+ advertisingUrl);
		}
		return advertisingUrl;
	}

	private String getAdvertisingLinkURL() {
		String advertisingLinkUrl = new StringBuilder(getString(R.string.get_advertising_link_url))
							.toString()
							.replace(PARAM_CLIENT, Uri.encode(LibrelioApplication.getClientName(self())))
							.replace(PARAM_APP, Uri.encode(LibrelioApplication.getMagazineName(self())));
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Advertising link url: "+ advertisingLinkUrl);
		}
		return advertisingLinkUrl;
	}

	private StartupActivity self(){
		return this;
	}

	void startMainMagazineActivity() {
		startActivity(getMainTabsActivityIntent());
		finish();
	}

    private Intent getMainTabsActivityIntent() {
        return MainTabsActivity.getIntent(this);
    }

	/**
	 * @param pLink - URL to display
	 */
	void startAdsActivity(final String pLink) {
		Intent mainMagazineActivityIntent = getMainTabsActivityIntent();
		Intent webAdvertisingActivityIntent = new Intent(self(),
				WebViewActivity.class);
		webAdvertisingActivityIntent.putExtra(
				WebViewActivity.PARAM_LINK, pLink);
		startActivities(new Intent[] { mainMagazineActivityIntent,
				webAdvertisingActivityIntent });
		finish();
	}
}

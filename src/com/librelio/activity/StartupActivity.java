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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
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
import com.niveales.wind.R;

/**
 * The start point for Librelio application (Splash-screen)
 * 
 * @author Nikolay Moskvin <moskvin@netcook.org>
 * 
 */
public class StartupActivity extends AbstractLockRotationActivity {
	private static final String TAG = "StartupActivity";
	private static final String PARAM_TEST = "test";
		
	private static final String PARAM_CLIENT = "@client";
	private static final String PARAM_APP = "@app";
	
	private static final String PLIST_DELAY = "Delay";
	private static final String PLIST_LINK = "Link";
	
	private static int DEFAULT_ADV_DELAY = 1000;

	private ImageView startupImage;
	private ImageView advertisingImage;
	
	private boolean advertisingClickPerfomed = false;
	
	private boolean isFirstImage = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.startup);
		
		startupImage = (ImageView) findViewById(R.id.sturtup_image);
		advertisingImage = (ImageView) findViewById(R.id.advertising_image);
		
		if (hasTestMagazine()) {
			initStorage(PARAM_TEST);
			new InitTestMagazines().execute(PARAM_TEST);
		} else {
			initStorage(PARAM_TEST);
			new InitPredefinedMagazinesTask().execute();
		}
		
		new LoadAdvertisingImageTask().execute();
	}
	
	@Override
	public void onStart(){
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
		
	}
	
	@Override
	public void onStop(){
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}

	
	
	private class InitTestMagazines extends AsyncTask<String, Void, Integer> {

		@Override
		protected Integer doInBackground(String... params) {
			try {
				final String name = params[0];
				final String testDir = getStoragePath() + name + "/";
				final String testImage = name + ".png";
				final String testImagePath = getStoragePath() + testImage;
				String[] assetsList = getResources().getAssets().list(name);
				File file = new File(testImagePath);
				if (!file.exists()) {
					copyFromAssets(testImage, testImagePath);
				}
				for(String asset : assetsList){
					file = new File(testDir + asset);
					if (!file.exists()) {
						copyFromAssets(name + "/" + asset, testDir + asset);
					}
				}
				return 0;
			} catch (IOException e) {
				Log.e(TAG,"Test directory in assets is unavailable", e);
			}
			return -1;
		}
		


		@Override
		protected void onPostExecute(Integer result) {
			if (isCancelled()) {
				return;
			}
			getPreferences().edit().putBoolean(TEST_INIT_COMPLETE, result == 0).commit();
			new InitPredefinedMagazinesTask().execute();
		}
	};

	private class InitPredefinedMagazinesTask extends AsyncTask<Void, Void, Integer>{
		@Override
		protected Integer doInBackground(Void... params) {
			String[] assetsList = null;
			try {
				assetsList = getResources().getAssets().list("");
				for (String file : assetsList) {
					if (file.contains(".plist") || file.contains(".png")) {
						copyFromAssets(file, getStoragePath() + file);
					}
				}
				return 0;
			} catch (IOException e) {
				Log.e(TAG, "copy fake-magazines failed", e);
			}

			return -1;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (isCancelled()) {
				return;
			}
		}

	}
	
	private class LoadAdvertisingImageTask extends AsyncTask<Void, Void, Bitmap>{
		@Override
		protected Bitmap doInBackground(Void... params) {
			
			String imageUrl = getAdvertisingImageURL();
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(imageUrl);
			try {
				HttpResponse response = httpclient.execute(httpget);
				if (response != null){
					HttpEntity entity = response.getEntity();
					if (entity != null){
						Bitmap adImage = BitmapFactory.decodeStream(entity.getContent());
						return adImage;
					}
				}
			} catch (ClientProtocolException e) {
				Log.e(TAG, "Loading advertising image failed.", e);
			} catch (IOException e) {
				Log.e(TAG, "Loading advertising image failed.", e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap adImage) {
			 if (adImage != null){
				 advertisingImage.setImageBitmap(adImage);
				 if (isFirstImage) {      
					 applyRotation(0, 90);
					 isFirstImage = !isFirstImage;
				 } else {   
					 applyRotation(0, -90);
					 isFirstImage = !isFirstImage;
				 }
				 
				 new LoadAdvertisingLinkTask().execute();
			 }else{
				onStartMagazine(DEFAULT_ADV_DELAY);
 			 }
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
	
	private class LoadAdvertisingLinkTask extends AsyncTask<Void, Void, Integer>{
		@Override
		protected Integer doInBackground(Void... params) {
			String linkUrl = getAdvertisingLinkURL();
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(linkUrl);
			try {
				HttpResponse response = httpclient.execute(httpget);
				if (response != null){
					HttpEntity entity = response.getEntity();
					if (entity != null){
						
						InputStream is = entity.getContent();
						InputStreamReader isReader = new InputStreamReader(is);
						StringBuilder sb = new StringBuilder();
						BufferedReader br = new BufferedReader(isReader);
						String read = br.readLine();
						while(read != null) {
							sb.append(read);
							read = br.readLine();
						}
						isReader.close();
						
						PListXMLHandler handler = new PListXMLHandler();
						PListXMLParser parser = new PListXMLParser();
						parser.setHandler(handler);
						parser.parse(sb.toString());
						PList list = ((PListXMLHandler)parser.getHandler()).getPlist();
						if (list != null){
							Dict dict = (Dict) list.getRootElement();
							String delay = dict.getConfiguration(PLIST_DELAY).getValue().toString();
							String link = dict.getConfiguration(PLIST_LINK).getValue().toString();
							setOnAdvertisingImageClickListener(link);
							return Integer.valueOf(delay);
						}
					}
				}
			} catch (ClientProtocolException e) {
				Log.e(TAG, "Loading advertising link failed.", e);
			} catch (IOException e) {
				Log.e(TAG, "Loading advertising link failed.", e);
			}
			return DEFAULT_ADV_DELAY;
		}

		@Override
		protected void onPostExecute(Integer delay) {
			onStartMagazine(delay);
		}
	}

	protected void onStartMagazine(int delay) {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if (!advertisingClickPerfomed){
					Intent intent = new Intent(self(), MainMagazineActivity.class);
					startActivity(intent);
					finish();
				}
			}
		}, delay);
	}
	
	private void setOnAdvertisingImageClickListener(final String link){
		if (advertisingImage != null){
			advertisingImage.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					advertisingClickPerfomed = true;
					
					Intent intent = new Intent(self(), WebAdvertisingActivity.class);
					intent.putExtra(WebAdvertisingActivity.PARAM_LINK, link);
					startActivity(intent);
					finish();
				}
			});
		} 
	}
	
	private String getAdvertisingImageURL() {
		
		return new StringBuilder(getString(R.string.get_advertising_image_url))
							.append(getString(R.string.get_advertising_image_end))
							.toString()
							.replace(PARAM_CLIENT, Uri.encode(LibrelioApplication.getClientName(self())))
							.replace(PARAM_APP, Uri.encode(LibrelioApplication.getMagazineName(self())));
	}
	
	private String getAdvertisingLinkURL() {
		
		return new StringBuilder(getString(R.string.get_advertising_link_url))
							.toString()
							.replace(PARAM_CLIENT, Uri.encode(LibrelioApplication.getClientName(self())))
							.replace(PARAM_APP, Uri.encode(LibrelioApplication.getMagazineName(self())));
	}
	
	private StartupActivity self(){
		return this;
	}

}
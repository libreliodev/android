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

import java.io.File;
import java.io.IOException;
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
import android.widget.ImageView;

import com.librelio.LibrelioApplication;
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
	
	private ImageView startupImage;
	
	private boolean advertisingClickPerfomed = false; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.startup);
		
		startupImage = (ImageView) findViewById(R.id.sturtup_image);
		
		if (hasTestMagazine()) {
			initStorage(PARAM_TEST);
			new InitTestMagazines().execute(PARAM_TEST);
		} else {
			initStorage(PARAM_TEST);
			new InitPredefinedMagazinesTask().execute();
		}
		
		new LoadAdvertisingImageTask().execute();
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
				 startupImage.setImageBitmap(adImage);
			 }
			new LoadAdvertisingLinkTask().execute();
		}
	}
	
	private class LoadAdvertisingLinkTask extends AsyncTask<Void, Void, Void>{
		@Override
		protected Void doInBackground(Void... params) {
			String linkUrl = getAdvertisingLinkURL();
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			onStartMagazine();
		}
	}

	protected void onStartMagazine() {
		if (advertisingClickPerfomed){
			return;
		}
		
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				Intent intent = new Intent(getApplicationContext(), MainMagazineActivity.class);
				startActivity(intent);
				finish();
			}
		}, 1000);
	}
	
	protected void onChangeRotation() {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				setContentView(R.layout.startup);
			}
		}, 1000);
	}
	
	private String getAdvertisingImageURL() {
		
		return new StringBuilder(getString(R.string.get_advertising_image_url))
							.append(getString(R.string.get_advertising_image_end))
							.toString()
							.replace(PARAM_CLIENT, Uri.encode(LibrelioApplication.getClientName(this)))
							.replace(PARAM_APP, Uri.encode(LibrelioApplication.getMagazineName(this)));
	}
	
	private String getAdvertisingLinkURL() {
		
		return new StringBuilder(getString(R.string.get_advertising_link_url))
							.toString()
							.replace(PARAM_CLIENT, Uri.encode(LibrelioApplication.getClientName(this)))
							.replace(PARAM_APP, Uri.encode(LibrelioApplication.getMagazineName(this)));
	}

}
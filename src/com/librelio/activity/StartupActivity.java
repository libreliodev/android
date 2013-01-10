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

import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.librelio.base.BaseActivity;
import com.niveales.wind.R;

/**
 * The start point for Librelio application (Splash-screen)
 * 
 * @author Nikolay Moskvin <moskvin@netcook.org>
 * 
 */
public class StartupActivity extends BaseActivity {
	private static final String TAG = "StartupActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.startup);


		if (hasTestMagazine() && !getPreferences().getBoolean(TEST_INIT_COMPLETE, false)) {
			initStorage("test");
			new InitTestMagazines().execute("test");
		} else {
			initStorage("test");
			new InitPredefinedMagazinesTask().execute();
		}

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
				for(String file : assetsList){
					copyFromAssets(name + "/" + file, testDir + file);
				}
				copyFromAssets(testImage, testImagePath);
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
			getPreferences().edit().putBoolean(TEST_INIT_COMPLETE, true).commit();
			onStartMagazine();
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		setContentView(R.layout.startup);
		super.onConfigurationChanged(newConfig);
	}

	protected void onStartMagazine() {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				Intent intent = new Intent(getApplicationContext(), MainMagazineActivity.class);
				startActivity(intent);
				finish();
			}
		}, 1000);
	}
}
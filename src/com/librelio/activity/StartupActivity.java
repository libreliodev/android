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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.librelio.LibrelioApplication;
import com.librelio.base.BaseActivity;
import com.librelio.service.DownloadMagazineListService;
import com.librelio.storage.DataBaseHelper;
import com.librelio.storage.Magazines;
import com.niveales.wind.R;

/**
 * The start point for Librelio application (Splash-screen)
 * 
 * @author Nikolay Moskvin <moskvin@netcook.org>
 * 
 */
public class StartupActivity extends BaseActivity {
	private static final String TAG = "StartupActivity";
	public static final String TEST_FILE_NAME = "test/test.pdf";

	private BroadcastReceiver br;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.startup);
		File f = new File(getStoragePath());
		if(!f.exists()){
			Log.d(TAG,"onCreate directory was create");
			f.mkdirs();
		}
		/**
		 * TODO delete after testing
		 * @Mike Did you test?
		 */
		new AsyncTask<Void, Void, Void>(){
			String[] assetsList1 = null;
			String testDir = null;
			protected void onPreExecute() {
				testDir = getStoragePath()+"test/";
				Log.d(TAG,"testDir: "+testDir);
				File dir = new File(testDir);
				if(!dir.exists()){
					dir.mkdir();
				}
				try {
					assetsList1 = getResources().getAssets().list("test");
				} catch (IOException e1) {
					Log.e(TAG,"Test directory in assets is unavailable",e1);
				}				
			};
			@Override
			protected Void doInBackground(Void... params) {
				//FIXME: @Mike Please add check if copy-process was failed!
				for(String file : assetsList1){
					Log.d(TAG,file);
					copyFromAssets("test/"+file, testDir+file);
				}
				copyFromAssets("test.png", getStoragePath() + "test.png");
				return null;
			}
			@Override
			protected void onPostExecute(Void result) {
				new StartUpTask().execute();
				super.onPostExecute(result);
			}
			
		}.execute();

	}
	
	public class StartUpTask extends AsyncTask<Void, Void, Void>{
		DataBaseHelper dbhelp;
		SQLiteDatabase db;
		Cursor c;
		@Override
		protected Void doInBackground(Void... params) {
			dbhelp = new DataBaseHelper(getContext());
			db = dbhelp.getReadableDatabase();
			c = db.rawQuery("select * from "+Magazines.TABLE_NAME, null);
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			if(c.getCount()>0){
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						startMagazinesView();
					}
				}, 1000);
			} else {
				if(!LibrelioApplication.thereIsConnection(getContext())){
					String[] assetsList = null;
					try {
						assetsList = getResources().getAssets().list("");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					for(String file : assetsList){
						if(file.contains(".plist")||file.contains(".png")){
							copyFromAssets(file, getStoragePath()+file);
						}
					}
				}
				Intent intent = new Intent(getContext(), DownloadMagazineListService.class);
				startService(intent);
				br = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						startMagazinesView();
					}
				};
				IntentFilter filter = new IntentFilter(BROADCAST_ACTION);
				registerReceiver(br, filter);
			}
			c.close();
			db.close();
			super.onPostExecute(result);
		}
		
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		setContentView(R.layout.startup);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onDestroy() {
		if(br!=null){
			unregisterReceiver(br);
		}
		super.onDestroy();
	}
	
	private void startMagazinesView(){
		Intent intent = new Intent(getApplicationContext(),
				MainMagazineActivity.class);
		startActivity(intent);
		finish();
	}
	
	private void copyFromAssets(String src, String dst){
		try {
			int count;
			InputStream input = getAssets().open(src);
			OutputStream output = new FileOutputStream(dst);
			byte data[] = new byte[1024];

			while ((count = input.read(data)) != -1) {
				output.write(data, 0, count);
			}
			output.flush();
			output.close();
			input.close();
		} catch (IOException e) {
			Log.e(TAG, "copyFromAssets failed", e);
		}
	}
	
	private Context getContext(){
		return this;
	}
}
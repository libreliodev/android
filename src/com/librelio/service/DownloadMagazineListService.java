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

package com.librelio.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.librelio.LibrelioApplication;
import com.librelio.activity.MainMagazineActivity;
import com.librelio.base.BaseActivity;
import com.librelio.base.BaseService;
import com.librelio.model.Magazine;
import com.librelio.storage.MagazineManager;
import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Array;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;

/**
 * The service for download magazines
 * 
 * @author Nikolay Moskvin <moskvin@netcook.org>
 * 
 */
public class DownloadMagazineListService extends BaseService {
	private static final String TAG = "DownloadMagazinesService";

	public static final String USE_STATIC_MAGAZINES = "USE_STATIC_MAGAZINES";
	public static final String ALREADY_RUNNING = "DownloadMagazineListService_ALREADY_RUNNING";

	private static final String PLIST_FILE_NAME = "magazines.plist";
	private static final String FILE_NAME_KEY = "FileName";
	private static final String TITLE_KEY = "Title";
	private static final String SUBTITLE_KEY = "Subtitle";

	private String plistUrl;
	private String pList;
	private Calendar calendar;
	private SimpleDateFormat dateFormat;
	private MagazineManager magazineManager;
	private boolean useStaticMagazines;

	@Override
	public void onCreate() {
		if (getPreferences().getBoolean(ALREADY_RUNNING, false)) {
			return;
		}
		getPreferences().edit().putBoolean(ALREADY_RUNNING, true).commit();
		calendar = Calendar.getInstance();
		dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
		magazineManager = new MagazineManager(this);
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				magazineManager.cleanMagazines(Magazine.TABLE_MAGAZINES);
				//
				plistUrl = LibrelioApplication.getAmazonServerUrl() + "Magazines.plist";
				Log.d(TAG, "Downloading start path:" + getStoragePath() + ", mode = " + useStaticMagazines);

				// Plist downloading
				if (isOnline() && !useStaticMagazines) {
					downloadFromUrl(plistUrl, getStoragePath() + PLIST_FILE_NAME);
				}
				//Convert plist to String for parsing
				pList = getStringFromFile(getStoragePath() + PLIST_FILE_NAME);
				//Parsing
				PListXMLHandler handler = new PListXMLHandler();
				PListXMLParser parser = new PListXMLParser();
				parser.setHandler(handler);
				parser.parse(pList);
				PList list = ((PListXMLHandler)parser.getHandler()).getPlist();
				Array arr = (Array) list.getRootElement();
				for (int i = 0; i < arr.size(); i++) {
					Dict dict = (Dict) arr.get(i);
					String fileName = dict.getConfiguration(FILE_NAME_KEY).getValue().toString();
					String title = dict.getConfiguration(TITLE_KEY).getValue().toString();
					String subtitle = dict.getConfiguration(SUBTITLE_KEY).getValue().toString();
					String downloadDate = getCurrentDate();
					
					Magazine magazine = new Magazine(fileName, title, subtitle, downloadDate, getContext());
					//saving png
					File png = new File(magazine.getPngPath());
					if (!png.exists()) {
						if (isOnline() && !useStaticMagazines) {
							downloadFromUrl(magazine.getPngUrl(), magazine.getPngPath());
						}
						Log.d(TAG, "Image download: " + magazine.getPngPath());
					} else {
						Log.d(TAG, magazine.getPngPath() + " already exist");
					}
					magazineManager.addMagazine(magazine, Magazine.TABLE_MAGAZINES, false);
				}

				Log.d(TAG, "Downloading was finished");
				//
				try {
					Intent intent = new Intent(BaseActivity.BROADCAST_ACTION);
					sendBroadcast(intent);
					Intent intentInvalidate = new Intent(MainMagazineActivity.BROADCAST_ACTION_IVALIDATE);
					sendBroadcast(intentInvalidate);
					Intent updateProgressStop = new Intent(MainMagazineActivity.UPDATE_PROGRESS_STOP);
					sendBroadcast(updateProgressStop);
				//
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "sendBroadcast failed", e);
				}
				stopSelf();
				getPreferences().edit().putBoolean(ALREADY_RUNNING, false).commit();
				if (useStaticMagazines) {
					startSelf();
				}
				return null;
			}
		}.execute();
		super.onCreate();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (null != intent) {
			useStaticMagazines = intent.getBooleanExtra(USE_STATIC_MAGAZINES, false);
		}
		return super.onStartCommand(intent, flags, startId);
	}

	public static int downloadFromUrl(String sUrl, String filePath){
		int count = -1;
		try{
			URL url = new URL(sUrl);
			URLConnection conexion = url.openConnection();
			conexion.connect();
	
			int lenghtOfFile = conexion.getContentLength();
			Log.d(TAG, "downloadFromUrl Lenght of file: " + lenghtOfFile);
	
			InputStream input = new BufferedInputStream(url.openStream());
			OutputStream output = new FileOutputStream(filePath);
	
			byte data[] = new byte[1024];
			while ((count = input.read(data)) != -1) {
				output.write(data, 0, count);
			}
			output.flush();
			output.close();
			input.close();
		} catch (Exception e) {
			Log.e(TAG, "Problem with download: " + filePath, e);
		}
		return count;
	}

	private static String getStringFromFile(String path){
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(path));

		} catch (FileNotFoundException e) {
			Log.e(TAG, "Problem with open file", e);
			return null;
		}
		char[] buf = new char[1024];
		int numRead = 0;
		try {
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
			reader.close();
		} catch (IOException e) {
			Log.e(TAG,"Problem with reading file", e);
			return null;
		}
		return fileData.toString();
	}
	
	private String getCurrentDate(){
		return dateFormat.format(calendar.getTime());
	}

	private void startSelf() {
		startService(new Intent(getApplicationContext(), DownloadMagazineListService.class));
	}
	
	private Context getContext() {
		return this;
	}
}

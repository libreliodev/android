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
import java.util.Date;

import com.librelio.utils.StorageUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

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
	private static final String IF_MODIFIED_SINCE_HEADER = "If-Modified-Since";

	private String plistUrl;
	private String pList;
	private Calendar calendar;
	private SimpleDateFormat dateFormat;
	private SimpleDateFormat updateDateFormat;
	private MagazineManager magazineManager;
	private boolean useStaticMagazines;
	private Intent intentInvalidateUI;

	@Override
	public void onCreate() {
		if (getPreferences().getBoolean(ALREADY_RUNNING, false)) {
			return;
		}
		getPreferences().edit().putBoolean(ALREADY_RUNNING, true).commit();
		calendar = Calendar.getInstance();
		dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
		updateDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

		intentInvalidateUI = new Intent(MainMagazineActivity.BROADCAST_ACTION_IVALIDATE);
		magazineManager = new MagazineManager(this);
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				plistUrl = LibrelioApplication.getAmazonServerUrl() + "Magazines.plist";
				Log.d(TAG, "Downloading start path:" + StorageUtils.getStoragePath(getContext()) + ", mode = " + useStaticMagazines);
				
				//Checking for updates
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httpget = new HttpGet(plistUrl);
				String lastUdate = getLastUdateDate();
				httpget.addHeader(IF_MODIFIED_SINCE_HEADER,lastUdate);
				
				int responseCode = 0;
				try {
					HttpResponse response = httpclient.execute(httpget);
					responseCode = response.getStatusLine().getStatusCode();
				} catch (ClientProtocolException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				if(responseCode == 304){
					Log.d(TAG,"There is NO updates, code "+responseCode+" (current update date : "+lastUdate+")");
					finishDownloading();
					return null;
				}
				
				sendProgressUpdateIntent(true);
				// Plist downloading
				if (isOnline() && !useStaticMagazines) {
					downloadFromUrl(plistUrl, StorageUtils.getStoragePath(getContext()) + PLIST_FILE_NAME);
					saveUpadteDate();
					Log.d(TAG,"There is updates (current update date : "+lastUdate+")");
				}
				//Convert plist to String for parsing
				pList = getStringFromFile(StorageUtils.getStoragePath(getContext()) + PLIST_FILE_NAME);
				
				//Parsing
				PListXMLHandler handler = new PListXMLHandler();
				PListXMLParser parser = new PListXMLParser();
				parser.setHandler(handler);
				parser.parse(pList);
				PList list = ((PListXMLHandler)parser.getHandler()).getPlist();
				Array arr = (Array) list.getRootElement();
				if(arr.size() > 0){
					magazineManager.cleanMagazines(Magazine.TABLE_MAGAZINES);
				}
				for (int i = 0; i < arr.size(); i++) {
					Dict dict = (Dict) arr.get(i);
					String fileName = dict.getConfiguration(FILE_NAME_KEY).getValue().toString();
					String title = dict.getConfiguration(TITLE_KEY).getValue().toString();
					String subtitle = dict.getConfiguration(SUBTITLE_KEY).getValue().toString();
					String downloadDate = getCurrentDate();
					
					Magazine magazine = new Magazine(fileName, title, subtitle, downloadDate, getContext());
					magazineManager.addMagazine(magazine, Magazine.TABLE_MAGAZINES, false);
				}
				sendBroadcast(intentInvalidateUI);
				for (Magazine magazine : magazineManager.getMagazines(false, Magazine.TABLE_MAGAZINES)) {
					//saving png
					File png = new File(magazine.getPngPath());
					if (!png.exists()) {
						if (isOnline() && !useStaticMagazines) {
							downloadFromUrl(magazine.getPngUrl(), magazine.getPngPath());
						}
						Log.d(TAG, "Image download: " + magazine.getPngPath());
						sendBroadcast(intentInvalidateUI);
					} else {
						Log.d(TAG, magazine.getPngPath() + " already exist");
					}
				}
				finishDownloading();
				return null;
			}
			
			
		}.execute();
		super.onCreate();
	}

	private void finishDownloading(){
		Log.d(TAG, "Downloading was finished");
		try {
			sendBroadcast(intentInvalidateUI);
			sendProgressUpdateIntent(false);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "sendBroadcast failed", e);
		}
		stopSelf();
		getPreferences().edit().putBoolean(ALREADY_RUNNING, false).commit();
		if (useStaticMagazines) {
			startSelf();
		}
	}

	private void sendProgressUpdateIntent(boolean show) {
		Intent updateProgress = new Intent(
				MainMagazineActivity.UPDATE_PROGRESS);
		updateProgress
				.putExtra(MainMagazineActivity.UPDATE_PROGRESS, show);
		sendBroadcast(updateProgress);
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
	
	private void saveUpadteDate(){
		Date date = (Date) calendar.getTime();
		Log.d(TAG, "saveUpadteDate, date : "+updateDateFormat.format(date));
		getPreferences().edit().putString(LAST_UPDATE_PREFERENCES_KEY, updateDateFormat.format(date)).commit(); 
	}
	
	private String getLastUdateDate(){
		String date = getPreferences().getString(LAST_UPDATE_PREFERENCES_KEY, "");
		Log.d(TAG, "getLastUdateDate, date : "+date);
		return date;
	}
}

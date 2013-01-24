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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.artifex.mupdf.LinkInfo;
import com.librelio.LibrelioApplication;
import com.librelio.base.BaseActivity;
import com.librelio.lib.utils.PDFParser;
import com.librelio.model.Magazine;
import com.librelio.service.DownloadMagazineListService;
import com.niveales.wind.R;

/**
 * The screen for download any resources from amazon servers
 * 
 * @author Nikolay Moskvin <moskvin@netcook.org>
 * 
 */
public class DownloadActivity extends BaseActivity {
	private static final String TAG = "DownloadActivity";
	private static final String STOP = "stop_modificator";

	private static final int INTERRUPT = -1;
	private static final int FINISH = 0;
	private static final int CONNECTION_ALERT = 1;

	public static final String FILE_NAME_KEY = "file_name_key";
	public static final String TITLE_KEY = "title_key";
	public static final String SUBTITLE_KEY = "subtitle_key";
	public static final String IS_SAMPLE_KEY = "issample";
	public static final String IS_TEMP_KEY = "istemp";
	public static final String TEMP_URL_KEY = "tempurl";

	private String fileName;
	private String fileUrl;
	private String filePath;
	private boolean isSample;
	private boolean isTemp;
	private ImageView preview;
	private TextView text;
	private ProgressBar progress;
	private DownloadTask download;
	private DownloadLinksTask downloadLinks;
	private Magazine magazine;
	private InputStream input;
	private OutputStream output;
	private boolean rotationWasDisabled = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String title = getIntent().getExtras().getString(TITLE_KEY);
		String subtitle = getIntent().getExtras().getString(SUBTITLE_KEY);
		fileName = getIntent().getExtras().getString(FILE_NAME_KEY);
		isSample= getIntent().getExtras().getBoolean(IS_SAMPLE_KEY);
		isTemp= getIntent().getExtras().getBoolean(IS_TEMP_KEY);
		
		magazine = new Magazine(fileName, title, subtitle, "", this);
		setContentView(R.layout.download);
		preview = (ImageView)findViewById(R.id.download_preview_image);
		text = (TextView)findViewById(R.id.download_progress_text);
		progress = (ProgressBar)findViewById(R.id.download_progress);
		progress.setProgress(0);
		
		fileUrl = magazine.getPdfUrl();
		filePath = magazine.getPdfPath();
		if(isSample){
			fileUrl = magazine.getSampleUrl();
			filePath = magazine.getSamplePath();
		} else if (isTemp){
			fileUrl = getIntent().getExtras().getString(TEMP_URL_KEY);
		}
		preview.setImageBitmap(BitmapFactory.decodeFile(magazine.getPngPath()));
		text.setText(getResources().getString(R.string.download));
		//
		if(!LibrelioApplication.thereIsConnection(this)){
			showDialog(CONNECTION_ALERT);
		} else {
			Log.d(TAG, "isSample: "+isSample+"\nfileUrl: "+fileUrl+"\nfilePath: "+filePath);
			download = new DownloadTask();
			try{
				download.execute();
			} catch (Exception e) {
				Log.e(TAG,"File download failed ("+fileUrl+")",e);
				download.cancel(true);
				finish();
			}
		}
	}

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

	private class DownloadTask extends AsyncTask<String, Double, String> {
		private NumberFormat formater = NumberFormat.getPercentInstance(Locale.getDefault());

		@Override
		protected void onPreExecute() {
			magazine.clearMagazineDir(); 
			magazine.makeMagazineDir();
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(String... params) {
			int count;
			try {
				URL url = new URL(fileUrl);
				URLConnection conexion = url.openConnection();
				conexion.connect();	
				int lengthOfFile = conexion.getContentLength();
				Log.d(TAG, "Length of file: " + lengthOfFile);
				input = new BufferedInputStream(url.openStream());
				output = new FileOutputStream(filePath);
				byte data[] = new byte[1024];
				long total = 0;
				while ((count = input.read(data)) != -1) {
					total += count;
					final double progress = total / (lengthOfFile * 1.0);
					publishProgress(progress);
					if(isCancelled()){
						output.flush();
						output.close();
						input.close();
						Log.d(TAG, "DownloadTask was stop");
						return STOP;
					}
					output.write(data, 0, count);
				}
				output.flush();
				output.close();
				input.close();
			} catch (Exception e) {
				// If download was interrupted then file delete
				File f = new File(filePath);
				f.delete();
				Log.e(TAG, "Problem with download!", e);
				return STOP;
			}
			return filePath;
		}

		@Override
		protected void onProgressUpdate(Double... p) {
			if (isCancelled()) {
				return;
			}
			final double curProgress = p[0];
			final String msg = String.format(Locale.getDefault(), "Downloading %s", formater.format(curProgress));
			text.setText(msg);
			progress.setProgress((int)(curProgress * 100));
		}

		@Override
		protected void onPostExecute(String result) {
			if(isCancelled() || result.equals(STOP)){
				closeDownloadScreen();
				return;
			}
			downloadLinks = new DownloadLinksTask();
			downloadLinks.execute();
			super.onPostExecute(result);
		}
	}

	private class DownloadLinksTask extends AsyncTask<String, String, Integer> {
		private ArrayList<String> links;
		private ArrayList<String> assetsNames;
		@Override
		protected void onPreExecute() {
			magazine.makeMagazineDir();
			text.setText("Getting assets...");
			Log.d(TAG,"Start DownloadLinksTask");
			links = new ArrayList<String>();
			assetsNames = new ArrayList<String>();
			//
			PDFParser linkGetter = new PDFParser(filePath);
			SparseArray<LinkInfo[]> linkBuf = linkGetter.getLinkInfo();
			if (linkBuf == null) {
				Log.d(TAG, "There is no links");
				return;
			}
			for (int i = 0; i < linkBuf.size(); i++) {
				int key = linkBuf.keyAt(i);
				Log.d(TAG,"--- i = "+i);
				if(linkBuf.get(key)!=null){
					for(int j=0;j<linkBuf.get(key).length;j++){
						String link = linkBuf.get(key)[j].uri;
						Log.d(TAG,"link[" + j + "] = "+link);
						String local = "http://localhost";
						if(link.startsWith(local)){
							int startIdx = local.length()+1;
							int finIdx = link.length();
							if(link.contains("?")){
								finIdx = link.indexOf("?");
							}
							String assetsFile = link.substring(startIdx, finIdx);
							links.add(Magazine.getAssetsBaseURL(fileName)+assetsFile);
							assetsNames.add(assetsFile);
							Log.d(TAG,"   link: "+Magazine.getAssetsBaseURL(fileName)+assetsFile);
							Log.d(TAG,"   file: "+assetsFile);
						}
					}
				}
			}
			text.setText("Download assets 0/"+links.size());
			progress.setProgress(0);
			progress.setMax(links.size());
			super.onPreExecute();
		}
		private int count = 0;
		@Override
		protected Integer doInBackground(String... params) {
			count = 0;
			for(int i=0;i<links.size();i++){
				if(isCancelled()){
					Log.d(TAG, "DownloadLinkTask was stop");
					magazine.clearMagazineDir();
					return INTERRUPT;
				}
				String assetUrl = links.get(i);
				String assetPath = magazine.getMagazineDir()+assetsNames.get(i);
				DownloadMagazineListService.downloadFromUrl(assetUrl, assetPath);
				publishProgress("");
			}
			return FINISH;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			count++;
			text.setText("Download assets "+count+"/"+links.size());
			progress.setProgress(count);
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Integer result) {
			if(result == INTERRUPT){
				return;
			}
			//
			magazine.makeCompleteFile(isSample);
			//
			Intent intentInvalidate = new Intent(MainMagazineActivity.BROADCAST_ACTION_IVALIDATE);
			sendBroadcast(intentInvalidate);
			LibrelioApplication.startPDFActivity(getContext(),filePath);
			closeDownloadScreen();
			super.onPostExecute(result);
		}
	}

	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBack");
		if(download!=null){
			download.cancel(true);
		}
		if(downloadLinks!=null){
			downloadLinks.cancel(true);
		}
		magazine.clearMagazineDir();
		finish();
		super.onBackPressed();
	}

	private void closeDownloadScreen(){
		finish();
	}

	private Context getContext(){
		return this;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONNECTION_ALERT:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String message = getResources().getString(R.string.connection_failed);
			builder.setMessage(message).setPositiveButton(R.string.ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			return builder.create();
		}
		return super.onCreateDialog(id);
	}
}

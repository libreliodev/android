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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.artifex.mupdf.LinkInfoExternal;
import com.google.analytics.tracking.android.EasyTracker;
import com.librelio.LibrelioApplication;
import com.librelio.lib.utils.PDFParser;
import com.librelio.model.Magazine;
import com.librelio.storage.MagazineManager;
import com.librelio.utils.FilenameUtils;
import com.niveales.wind.R;

/**
 * The screen for download any resources from amazon servers
 * 
 * @author Nikolay Moskvin <moskvin@netcook.org>
 * 
 */
public class DownloadActivity extends AbstractLockRotationActivity {
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
	private TextView progressText;
	private ProgressBar progress;
	private TextView assetProgressText;
	private ProgressBar assetProgress;
	private DownloadTask download;
	private DownloadLinksTask downloadLinks;
	private Magazine magazine;
	private InputStream input;
	private OutputStream output;
	private MagazineManager magazineManager;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String title = getIntent().getExtras().getString(TITLE_KEY);
		String subtitle = getIntent().getExtras().getString(SUBTITLE_KEY);
		fileName = getIntent().getExtras().getString(FILE_NAME_KEY);
		isSample= getIntent().getExtras().getBoolean(IS_SAMPLE_KEY);
		isTemp= getIntent().getExtras().getBoolean(IS_TEMP_KEY);
		
		magazineManager = new MagazineManager(this);
		
		magazine = new Magazine(fileName, title, subtitle, "", this);
		magazine.setSample(isSample);
		setContentView(R.layout.download);
		preview = (ImageView)findViewById(R.id.download_preview_image);
		progressText = (TextView)findViewById(R.id.download_progress_text);
		progress = (ProgressBar)findViewById(R.id.download_progress);
		assetProgressText = (TextView)findViewById(R.id.download_asset_progress_text);
		assetProgress = (ProgressBar)findViewById(R.id.download_asset_progress);
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
		progressText.setText(getResources().getString(R.string.download));
		//
		if(!LibrelioApplication.thereIsConnection(this)){
			showDialog(CONNECTION_ALERT);
		} else {
			Log.d(TAG, "isSample: "+isSample+"\nfileUrl: "+fileUrl+"\nfilePath: "+filePath);
			download = new DownloadTask();
			try{
				download.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} catch (Exception e) {
				Log.e(TAG,"File download failed ("+fileUrl+")",e);
				download.cancel(true);
				finish();
			}
		}
	}

	private class DownloadTask extends AsyncTask<String, Double, String> {
		
		private NumberFormat formater = NumberFormat.getPercentInstance(Locale.getDefault());
		private static final int BREAK_AFTER_FAILED_ATTEMPT = 5000;
		private static final int DOWNLOADING_ATTEMPTS = 4;

		@Override
		protected void onPreExecute() {
			magazine.clearMagazineDir(); 
			magazine.makeMagazineDir();
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(String... params) {
			int count;
			int lengthOfFile = 0;
			long total = 0;
			try {
				URL url = new URL(fileUrl);
				EasyTracker.getTracker().sendView(
						"Downloading/" + FilenameUtils.getBaseName(filePath));
				URLConnection connection = url.openConnection();
				connection.connect();
				lengthOfFile = connection.getContentLength();
				Log.d(TAG, "Length of file: " + lengthOfFile);
				input = new BufferedInputStream(url.openStream());
				output = new FileOutputStream(filePath);
				byte data[] = new byte[1024];
				while ((count = input.read(data)) != -1) {
					total += count;
					final double progress = total/(lengthOfFile * 1.0);
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
			} catch (Exception ex) {
				Log.e(TAG, "Problem with download!", ex);
				// If download was interrupted try downloading again
				return makeDownloadingAttempts(lengthOfFile, total);
			}finally{
				try {
					output.flush();
					output.close();
					input.close();
				} catch (Exception e) {}
			}

			return filePath;
		}

		@Override
		protected void onProgressUpdate(Double... p) {
			if (isCancelled()) {
				return;
			}
			final double curProgress = p[0];
			final String msg = (curProgress > 0) ?
					String.format(Locale.getDefault(), "Downloading %s", formater.format(curProgress)) 
					: getString(R.string.download);
			progressText.setText(msg);
			progress.setProgress((int)(curProgress * 100));
		}

		@Override
		protected void onPostExecute(String result) {
			if(isCancelled() || result.equals(STOP)){
				showAlertDialog(DOWNLOAD_ALERT);
				return;
			}
			downloadLinks = new DownloadLinksTask();
			downloadLinks.execute();
			super.onPostExecute(result);
		}
		
		private String makeDownloadingAttempts(int lengthOfFile, long total){
			String action = STOP;
			int attempts = 0;
			while (action == STOP){
				if (attempts >= DOWNLOADING_ATTEMPTS || isCancelled()){
					return STOP;
				}
				
				Object[] result = getFileAgain(lengthOfFile, total);
				action = (String) result[0]; //Downloading result.
				long bytes = (Long) result[1]; //Quantity of bytes were downloaded.
				boolean wereSomeBytes = bytes > total ? true : false; //Were downloaded some bytes.
				total = bytes;
				
				if (action == STOP){
					if (wereSomeBytes){
						attempts = 0;
					}
					attempts++;
					try {
						Thread.sleep(BREAK_AFTER_FAILED_ATTEMPT);
					} catch (InterruptedException e) {}
				}
			};
			return filePath;
		}
		
		private Object[] getFileAgain(int lengthOfFile, long total){
			Object[] result = new Object[2];
			//It tells, downloading failed.
			result[0] = STOP;
			
			int count;
			try {
				URL url = new URL(fileUrl);
				URLConnection connection = url.openConnection();
		        File file=new File(filePath);
		        if(file.exists()){
		             connection.setRequestProperty("Range", "bytes="+(file.length())+"-");
		        }
		        connection.connect();
				input = new BufferedInputStream(connection.getInputStream());
				output = new FileOutputStream(filePath, true);
				byte data[] = new byte[1024];
				while ((count = input.read(data)) != -1) {
					total += count;
					final double progress = total / (lengthOfFile * 1.0);
					publishProgress(progress);
					if(isCancelled()){
						Log.d(TAG, "DownloadTask was stop");
						output.flush();
						output.close();
						input.close();
						return result;
					}
					output.write(data, 0, count);
				}

		        //It tells, downloading success finished.
				result[0] = filePath;
			} catch (Exception e) {
				Log.e(TAG, "Problem with download!", e);
			}finally{
				try {
					output.flush();
					output.close();
					input.close();
				} catch (Exception e) {}
			}
			
	        //It tells quantity of bytes were downloaded.
			result[1] = total;
			return result;
		}
	}

	private class DownloadLinksTask extends AsyncTask<String, String, Integer> {
		private ArrayList<String> links;
		private ArrayList<String> assetsNames;
		
		private NumberFormat formater = NumberFormat.getPercentInstance(Locale.getDefault());
		private static final int BREAK_AFTER_FAILED_ATTEMPT = 5000;
		private static final int DOWNLOADING_ATTEMPTS = 4;
		
		@Override
		protected void onPreExecute() {
			magazine.makeMagazineDir();
			progressText.setText("Getting assets...");
			assetProgressText.setVisibility(View.VISIBLE);
			assetProgress.setVisibility(View.VISIBLE);
			Log.d(TAG,"Start DownloadLinksTask");
			links = new ArrayList<String>();
			assetsNames = new ArrayList<String>();
			//
			PDFParser linkGetter = new PDFParser(filePath);
			SparseArray<LinkInfoExternal[]> linkBuf = linkGetter.getLinkInfo();
			if (linkBuf == null) {
				Log.d(TAG, "There is no links");
				return;
			}
			for (int i = 0; i < linkBuf.size(); i++) {
				int key = linkBuf.keyAt(i);
				Log.d(TAG,"--- i = "+i);
				if(linkBuf.get(key)!=null){
					for(int j=0;j<linkBuf.get(key).length;j++){
						LinkInfoExternal extLink = linkBuf.get(key)[j];
						String link = linkBuf.get(key)[j].url;
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
			progressText.setText("Download assets 0/"+links.size());
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
				Object[] result = downloadFromUrl(assetUrl, assetPath, false);
				if ((Integer) result[0] == INTERRUPT){
					Log.d(TAG, "DownloadLinkTask failed");
					magazine.clearMagazineDir();
					return INTERRUPT;
				}
				publishProgress("");
			}
			return FINISH;
		}
		
		public Object[] downloadFromUrl(String sUrl, String filePath, boolean resume){
			Object[] result = new Object[2];
			int count = -1;
			try{
				URL url = new URL(sUrl);
				URLConnection connection = url.openConnection();
		        File file=new File(filePath);
		        if(file.exists()){
		             connection.setRequestProperty("Range", "bytes="+(file.length())+"-");
		        }
				connection.connect();
				int lengthOfFile = connection.getContentLength();
				Log.d(TAG, "downloadFromUrl Lenght of file: " + lengthOfFile);
		
				input = new BufferedInputStream(connection.getInputStream());
				output = new FileOutputStream(filePath, true);
				int total = 0;
				byte data[] = new byte[1024];
				while ((count = input.read(data)) != -1) {
					output.write(data, 0, count);
					total += count;
					final double progress = total / (lengthOfFile * 1.0);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							final String msg = (progress > 0) ? String.format(
									Locale.getDefault(), "Downloading %s",
									formater.format(progress))
									: getString(R.string.download);
							assetProgressText.setText(msg);
							assetProgress.setProgress((int) (progress * 100));
						}
		});
				}
				
		        //It tells, downloading success finished.
				result[0] = FINISH;
			} catch (Exception e) {
				Log.e(TAG, "Problem with download: " + filePath, e);
		        //It tells, downloading failed.
				result[0] = INTERRUPT;
				
				//Recursive calling. Only one level down always.
				if (!resume){
					result[0] = makeDownloadingAttempts(sUrl, filePath);
				}
			}finally{
				try {
					output.flush();
					output.close();
					input.close();
				} catch (Exception e) {}
			}
			
	        //It tells quantity of bytes were downloaded.
			result[1] = count;
			return result;
		}
		
		private int makeDownloadingAttempts(String sUrl, String filePath){
			int action = INTERRUPT;
			int attempts = 0;
			while(action == INTERRUPT){
				if (attempts >= DOWNLOADING_ATTEMPTS  || isCancelled()){
					break;
				}
				Object[] resumeResult = downloadFromUrl(sUrl, filePath, true);
				action = (Integer) resumeResult[0]; //Downloading result.
				boolean wereSomeBytes = (Integer) resumeResult[1] > -1 ? true : false; //Were downloaded some bytes.
				
				if (action == INTERRUPT){
					if (wereSomeBytes){
						attempts = 0;
					}
					attempts++;
					try {
						Thread.sleep(BREAK_AFTER_FAILED_ATTEMPT);
					} catch (InterruptedException ex) {}
				}
			}
			return action;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			count++;
			progressText.setText("Download assets "+count+"/"+links.size());
			progress.setProgress(count);
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Integer result) {
			if(result == INTERRUPT){
				showAlertDialog(DOWNLOAD_ALERT);
				return;
			}
			magazine.makeCompleteFile(isSample);
			
			Date date = Calendar.getInstance().getTime();
			String downloadDate = new SimpleDateFormat(" dd.MM.yyyy").format(date);
			magazine.setDownloadDate(downloadDate);
			magazineManager.removeMagazine(
					Magazine.TABLE_DOWNLOADED_MAGAZINES,
					Magazine.FIELD_FILE_NAME,
					"'" + magazine.getFileName() + "'");
			magazineManager.addMagazine(
					magazine, 
					Magazine.TABLE_DOWNLOADED_MAGAZINES, 
					true);
			
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

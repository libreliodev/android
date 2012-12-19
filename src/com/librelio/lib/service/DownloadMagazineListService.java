package com.librelio.lib.service;

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
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.librelio.base.BaseActivity;
import com.librelio.base.BaseService;
import com.librelio.base.IBaseContext;
import com.librelio.lib.LibrelioApplication;
import com.librelio.lib.model.MagazineModel;
import com.librelio.lib.storage.DataBaseHelper;
import com.librelio.lib.storage.Magazines;
import com.librelio.lib.ui.MainMagazineActivity;
import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Array;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;

public class DownloadMagazineListService extends BaseService{
	private static final String TAG = "DownloadPlistService";
	private static final String PLIST_FILE_NAME = "magazines.plist";
	private static final String FILE_NAME_KEY = "FileName";
	private static final String TITLE_KEY = "Title";
	private static final String SUBTITLE_KEY = "Subtitle";
	
	
	private String plistUrl;
	private String pList;
	private Calendar calendar;
	private SimpleDateFormat dateFormat;

	
	@Override
	public void onCreate() {
		calendar = Calendar.getInstance();
		dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
		new AsyncTask<Void, Void, Void>() {
			
			@Override
			protected Void doInBackground(Void... params) {
				//
				cleanMagazinesListInBase();
				//
				plistUrl = LibrelioApplication.BASE_URL+"Magazines.plist";
				Log.d(TAG,"onCreate path:"+((IBaseContext)getContext()).getStoragePath());
				
				
				// Plist downloading
				if(LibrelioApplication.thereIsConnection(getContext())){
					downloadFromUrl(plistUrl,((IBaseContext)getContext()).getStoragePath() +PLIST_FILE_NAME);
				}
				//Convert plist to String for parsing
				pList = getStringFromFile(((IBaseContext)getContext()).getStoragePath() +PLIST_FILE_NAME);
				//Parsing
				PListXMLHandler handler = new PListXMLHandler();
				PListXMLParser parser = new PListXMLParser();
				parser.setHandler(handler);
				parser.parse(pList);
				PList list = ((PListXMLHandler)parser.getHandler()).getPlist();
				Array arr = (Array)list.getRootElement();
				for(int i=0; i<arr.size();i++){
					Dict dict = (Dict)arr.get(i);
					String fileName = dict.getConfiguration(FILE_NAME_KEY).getValue().toString();
					String title = dict.getConfiguration(TITLE_KEY).getValue().toString();
					String subtitle = dict.getConfiguration(SUBTITLE_KEY).getValue().toString();
					String downloadDate = getCurrentDate();
					
					MagazineModel magazine = new MagazineModel(fileName, title, subtitle, downloadDate, getContext());
					//saving png
					File png = new File(magazine.getPngPath());
					if(!png.exists()){
						if(LibrelioApplication.thereIsConnection(getContext())){
							downloadFromUrl(magazine.getPngUrl(),magazine.getPngPath());
						}
						Log.d(TAG,"Image download: "+magazine.getPngPath());
					} else {
						Log.d(TAG,magazine.getPngPath()+" already exist");
					}
					magazine.saveInBase();
				}
				Log.d(TAG,"Downloading is finished");
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
				return null;
			}
		}.execute();
		super.onCreate();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	
	public static void downloadFromUrl(String sUrl, String filePath){
		int count;
		try{
			URL url = new URL(sUrl);
			URLConnection conexion = url.openConnection();
			conexion.connect();
	
			int lenghtOfFile = conexion.getContentLength();
			Log.d(TAG, "Lenght of file: " + lenghtOfFile);
	
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
	}
	
	public static String getStringFromFile(String path){
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
			Log.e(TAG,"Problem with reading file",e);
			return null;
		}
		return fileData.toString();
	}
	
	private String getCurrentDate(){
		return dateFormat.format(calendar.getTime());
	}
	
	private synchronized void cleanMagazinesListInBase(){
		SQLiteDatabase db;
		DataBaseHelper dbhelp = new DataBaseHelper(getApplicationContext());
		db = dbhelp.getWritableDatabase();
		db.execSQL("DELETE FROM "+Magazines.TABLE_NAME+" WHERE 1");
		db.close();
		Log.d(TAG, "at cleanMagazinesListInBase: "+Magazines.TABLE_NAME+" table was clean");
	}
	
	private Context getContext(){
		return this;
	}

}

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

import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.librelio.lib.LibrelioApplication;
import com.librelio.lib.model.MagazineModel;
import com.librelio.lib.storage.DataBaseHelper;
import com.librelio.lib.storage.Magazines;
import com.librelio.lib.ui.MainMagazineActivity;
import com.librelio.lib.ui.StartupActivity;
import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Array;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;

public class DownloadMagazineListService extends Service{
	private static final String TAG = "DownloadPlistService";
	private static final String PLIST_FILE_NAME = "magazines.plist";
	private static final String FILE_NAME_KEY = "FileName";
	private static final String TITLE_KEY = "Title";
	private static final String SUBTITLE_KEY = "Subtitle";
	
	
	private String plistUrl;
	private String pList;
	
	@Override
	public void onCreate() {
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			
			@Override
			protected Void doInBackground(Void... params) {
				//
				cleanMagazinesListInBase();
				//
				plistUrl = LibrelioApplication.BASE_URL+"Magazines.plist";
				Log.d(TAG,"onCreate path:"+LibrelioApplication.APP_DIRECTORY);
				File f = new File(LibrelioApplication.APP_DIRECTORY);
				if(!f.exists()){
					Log.d(TAG,"onCreate directory was create");
					f.mkdirs();
				}
				
				// Plist downloading
				downloadFromUrl(plistUrl,LibrelioApplication.APP_DIRECTORY+PLIST_FILE_NAME);
				//Convert plist to String for parsing
				pList = getStringFromFile(LibrelioApplication.APP_DIRECTORY+PLIST_FILE_NAME);
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
					
					MagazineModel magazine = new MagazineModel(fileName, title,
							subtitle, downloadDate, getApplicationContext());
					//saving png
					File png = new File(magazine.getPngPath());
					if(!png.exists()){
						downloadFromUrl(magazine.getPngUrl(),magazine.getPngPath());
						Log.d(TAG,"Image download: "+magazine.getPngPath());
					} else {
						Log.d(TAG,magazine.getPngPath()+" already exist");
					}
					magazine.saveInBase();
				}
				Log.d(TAG,"Downloading is finished");
				//
				Intent intent = new Intent(StartupActivity.BROADCAST_ACTION);
				sendBroadcast(intent);
				Intent intentInvalidate = new Intent(MainMagazineActivity.BROADCAST_ACTION_IVALIDATE);
				sendBroadcast(intentInvalidate);
				//
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
			Log.d("ANDRO_ASYNC", "Lenght of file: " + lenghtOfFile);
	
			InputStream input = new BufferedInputStream(url.openStream());
			OutputStream output = new FileOutputStream(filePath);
	
			byte data[] = new byte[1024];
	
			long total = 0;

			while ((count = input.read(data)) != -1) {
				total += count;
				output.write(data, 0, count);
			}
			output.flush();
			output.close();
			input.close();
		} catch (Exception e) {
			Log.e(TAG, "Problem with download: "+filePath,e);
		}
		/*try{
			URL url = new URL(sUrl);
			File file = new File(filePath);
			
			URLConnection ucon = url.openConnection();
			
			InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                    baf.append((byte) current);
            }

            /* Convert the Bytes read to a String. */
           /* FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            Log.d(TAG,"File was download: "+filePath);
            fos.close();
		}
        catch (FileNotFoundException e) {
        	Log.e(TAG,"File not found: "+filePath,e);
		} catch (IOException e) {
			Log.e(TAG,"IOException",e);
		} */
	}
	
	public static String getStringFromFile(String path){
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(path));

		} catch (FileNotFoundException e) {
			Log.e(TAG, "Problem with open file", e);
			return "";
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
			return "";
		}
		return fileData.toString();
	}
	
	private String getCurrentDate(){
		Calendar c = Calendar.getInstance();
		SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
		return df.format(c.getTime());
	}
	
	private synchronized void cleanMagazinesListInBase(){
		SQLiteDatabase db;
		DataBaseHelper dbhelp = new DataBaseHelper(getApplicationContext());
		db = dbhelp.getWritableDatabase();
		db.execSQL("DELETE FROM "+Magazines.TABLE_NAME+" WHERE 1");
		db.close();
		Log.d(TAG, "at cleanMagazinesListInBase: "+Magazines.TABLE_NAME+" table was clean");
	}
}

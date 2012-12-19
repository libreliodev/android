package com.librelio.lib.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
import android.os.Bundle;
import android.util.Log;

import com.librelio.base.BaseActivity;
import com.librelio.lib.LibrelioApplication;
import com.librelio.lib.service.DownloadMagazineListService;
import com.librelio.lib.storage.DataBaseHelper;
import com.librelio.lib.storage.Magazines;
import com.niveales.wind.R;

public class StartupActivity extends BaseActivity {
	private BroadcastReceiver br;
	private static final String TAG = "StartupActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		File f = new File(getStoragePath());
		if(!f.exists()){
			Log.d(TAG,"onCreate directory was create");
			f.mkdirs();
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.startup);
		DataBaseHelper dbhelp = new DataBaseHelper(this);
		SQLiteDatabase db = dbhelp.getReadableDatabase();
		Cursor c = db.rawQuery("select * from "+Magazines.TABLE_NAME, null);
		if(c.getCount()>0){
		
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					startMagazinesView();
				}
			}, 2000);
		} else if(LibrelioApplication.thereIsConnection(this)){
			Intent intent = new Intent(this, DownloadMagazineListService.class);
			startService(intent);
			
			br = new BroadcastReceiver() {			
				@Override
				public void onReceive(Context context, Intent intent) {
					startMagazinesView();
				}
			};
			IntentFilter filter = new IntentFilter(BROADCAST_ACTION);
			registerReceiver(br, filter);
		} else {
			String[] assetsList = null;
			try {
				assetsList = getResources().getAssets().list("");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			for(String file : assetsList){
				if(file.contains(".plist")||file.contains(".png")){
					try {
						int count;
						InputStream input = getAssets().open(file);
						OutputStream output = new FileOutputStream(getStoragePath()+file);
						byte data[] = new byte[1024];
						
						long total = 0;

						while ((count = input.read(data)) != -1) {
							total += count;
							output.write(data, 0, count);
						}
						output.flush();
						output.close();
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			Intent intent = new Intent(this, DownloadMagazineListService.class);
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
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		setContentView(R.layout.startup);
		super.onConfigurationChanged(newConfig);
	}
}
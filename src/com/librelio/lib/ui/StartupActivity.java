package com.librelio.lib.ui;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import com.librelio.lib.service.DownloadMagazineListService;
import com.librelio.lib.storage.DataBaseHelper;
import com.librelio.lib.storage.Magazines;
import com.niveales.wind.R;

public class StartupActivity extends Activity {
	public static final String BROADCAST_ACTION = "com.librelio.lib.service.broadcast";
	
	private static final String TAG = "StartupActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.startup);
		DataBaseHelper dbhelp = new DataBaseHelper(this);
		SQLiteDatabase db = dbhelp.getReadableDatabase();
		Cursor c = db.rawQuery("select * from "+Magazines.TABLE_NAME, null);
		if(c.getCount()>0){
		
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					Intent intent = new Intent(getApplicationContext(),
							MainMagazineActivity.class);
					startActivity(intent);
					finish();
				}
			}, 2000);
		} else {
			Intent intent = new Intent(this, DownloadMagazineListService.class);
			startService(intent);
			
			BroadcastReceiver br = new BroadcastReceiver() {			
				@Override
				public void onReceive(Context context, Intent intent) {
					Intent intent1 = new Intent(getApplicationContext(),
							MainMagazineActivity.class);
					startActivity(intent1);
					finish();
				}
			};
			IntentFilter filter = new IntentFilter(BROADCAST_ACTION);
			registerReceiver(br, filter);
		}
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
}
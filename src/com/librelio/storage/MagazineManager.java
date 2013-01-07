package com.librelio.storage;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.librelio.base.BaseManager;
import com.librelio.model.Magazine;

public class MagazineManager extends BaseManager {
	private static final String TAG = "MagazineManager";

	public static final String TEST_FILE_NAME = "test/test.pdf";
	public static final String TABLE_NAME = "Magazines";
	public static final String FIELD_ID = "_id";
	public static final String FIELD_TITLE = "title";
	public static final String FIELD_SUBTITLE = "subtitle";
	public static final String FIELD_FILE_NAME = "filename";
	public static final String FIELD_DOWNLOAD_DATE = "downloaddate";

	public MagazineManager(Context context) {
		super(context);
	}

	public List<Magazine> getMagazines(boolean hasTestMagazine) {
		List<Magazine> magazines = new ArrayList<Magazine>();
		if (hasTestMagazine) {
			magazines.add(new Magazine(TEST_FILE_NAME, "TEST", "test", "", getContext()));
		}
		DataBaseHelper dbhelp = new DataBaseHelper(getContext());
		SQLiteDatabase db = dbhelp.getReadableDatabase();
		Cursor c = db.rawQuery("select * from " + MagazineManager.TABLE_NAME, null);
		if(c.getCount()>0){
			c.moveToFirst();
			do{
				magazines.add(new Magazine(c, getContext()));
			}  while(c.moveToNext());
		}
		c.close();
		db.close();
		return magazines;
	}

	public synchronized void addMagazine(Magazine magazine) {
		SQLiteDatabase db;
		DataBaseHelper dbhelp = new DataBaseHelper(getContext());
		db = dbhelp.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(MagazineManager.FIELD_FILE_NAME, magazine.getFileName());
		cv.put(MagazineManager.FIELD_DOWNLOAD_DATE, magazine.getDownloadDate());
		cv.put(MagazineManager.FIELD_TITLE, magazine.getTitle());
		cv.put(MagazineManager.FIELD_SUBTITLE, magazine.getSubtitle());
		db.insert(MagazineManager.TABLE_NAME, null, cv);
		db.close();
	}

	public int getCount() {
		DataBaseHelper dbhelp = new DataBaseHelper(getContext());
		SQLiteDatabase db = dbhelp.getReadableDatabase();
		int count = (int) DatabaseUtils.longForQuery(db, "select COUNT(" + FIELD_ID + ") from " + MagazineManager.TABLE_NAME, null);
		db.close();
		return count;
	}

	public synchronized void cleanMagazines(){
		SQLiteDatabase db;
		DataBaseHelper dbhelp = new DataBaseHelper(getContext());
		db = dbhelp.getWritableDatabase();
		db.execSQL("DELETE FROM " + MagazineManager.TABLE_NAME + " WHERE 1");
		db.close();
		Log.d(TAG, "at cleanMagazinesListInBase: " + MagazineManager.TABLE_NAME + " table was clean");
	}
}

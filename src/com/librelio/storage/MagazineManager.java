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

	public MagazineManager(Context context) {
		super(context);
	}

	public List<Magazine> getMagazines(boolean hasTestMagazine, String tableName) {
		List<Magazine> magazines = new ArrayList<Magazine>();
		if (hasTestMagazine) {
			magazines.add(new Magazine(TEST_FILE_NAME, "TEST", "test", "", getContext()));
		}
		DataBaseHelper dbhelp = new DataBaseHelper(getContext());
		SQLiteDatabase db = dbhelp.getReadableDatabase();
		Cursor c = db.rawQuery("select * from " + tableName, null);
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

	public synchronized void addMagazine(Magazine magazine, String tableName, boolean withSample) {
		SQLiteDatabase db;
		DataBaseHelper dbhelp = new DataBaseHelper(getContext());
		db = dbhelp.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(Magazine.FIELD_FILE_NAME, magazine.getFileName());
		cv.put(Magazine.FIELD_DOWNLOAD_DATE, magazine.getDownloadDate());
		cv.put(Magazine.FIELD_TITLE, magazine.getTitle());
		cv.put(Magazine.FIELD_SUBTITLE, magazine.getSubtitle());
		if (withSample){
			cv.put(Magazine.FIELD_IS_SAMPLE, magazine.isSampleForBase());
		}
		db.insert(tableName, null, cv);
		db.close();
	}
	
	public synchronized void removeMagazine(String tableName, String whereClauseField, String whereClauseValue) {
		SQLiteDatabase db;
		DataBaseHelper dbhelp = new DataBaseHelper(getContext());
		db = dbhelp.getWritableDatabase();
		
		String deleteQuery = new StringBuilder("DELETE FROM ")
								.append(tableName)
								.append(" WHERE ")
								.append(whereClauseField)
								.append(" = ")
								.append(whereClauseValue).toString();
		
		db.execSQL(deleteQuery);
		db.close();
	}

	public int getCount(String tableName) {
		DataBaseHelper dbhelp = new DataBaseHelper(getContext());
		SQLiteDatabase db = dbhelp.getReadableDatabase();
		int count = (int) DatabaseUtils.longForQuery(db, "select COUNT(" + Magazine.FIELD_ID + ") from " + tableName, null);
		db.close();
		return count;
	}

	public synchronized void cleanMagazines(String tableName){
		SQLiteDatabase db;
		DataBaseHelper dbhelp = new DataBaseHelper(getContext());
		db = dbhelp.getWritableDatabase();
		db.execSQL("DELETE FROM " + tableName + " WHERE 1");
		db.close();
		Log.d(TAG, "at cleanMagazinesListInBase: " + tableName + " table was clean");
	}

	/**
	 * Look up magazine by path
	 * @param path
	 * @return
	 */
	public Magazine findByFileName(String path, String tableName) {
		Magazine magazine = null;
		SQLiteDatabase db;
		DataBaseHelper dbhelp = new DataBaseHelper(getContext());
		db = dbhelp.getReadableDatabase();
//		Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
//		while(cursor.moveToNext()) {
//			magazine = new Magazine(cursor, getContext());
//			Log.d(TAG, "'" + magazine.getFileName() + "' <====> '" + path + "'");
//		}
		Cursor cursor = db.query(tableName, null, Magazine.FIELD_FILE_NAME + "=?", new String[]{path}, null, null, null);
		if (cursor.moveToFirst()) {
			magazine = new Magazine(cursor, getContext());
		}

		cursor.close();
		db.close();
		return magazine;
	}
}

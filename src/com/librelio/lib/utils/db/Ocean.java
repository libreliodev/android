/**********************************************************************************************************************************************************************
 ****** AUTO GENERATED FILE BY ANDROID SQLITE HELPER SCRIPT BY FEDERICO PAOLINELLI. ANY CHANGE WILL BE WIPED OUT IF THE SCRIPT IS PROCESSED AGAIN. *******
 **********************************************************************************************************************************************************************/
package com.librelio.lib.utils.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

import java.util.Date;

public class Ocean {

	private static final String TAG = "Ocean";

	private static final String DATABASE_NAME = "dbhelperDb.db";
	private static final int DATABASE_VERSION = 3;

	// Variable to hold the database instance
	protected SQLiteDatabase mDb;
	// Context of the application using the database.
	private final Context mContext;
	// Database open/upgrade helper
	private MyDbHelper mDbHelper;

	public Ocean(Context context) {
		mContext = context;
		mDbHelper = new MyDbHelper(mContext, DATABASE_NAME, null,
				DATABASE_VERSION);
	}

	public Ocean open() throws SQLException {
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDb.close();
	}

	// -------------- MAGAZINE DEFINITIONS ------------

	public static final String MAGAZINE_TABLE = "Magazine";
	public static final String MAGAZINE_NAME_KEY = "name";
	protected static final int MAGAZINE_NAME_COLUMN = 1;
	public static final String MAGAZINE_VERSION_KEY = "serial";
	protected static final int MAGAZINE_VERSION_COLUMN = 2;
	public static final String MAGAZINE_BASE_URL_KEY = "version";
	protected static final int MAGAZINE_BASE_URL_COLUMN = 3;
	public static final String MAGAZINE_SKU_KEY = "sku";
	protected static final int MAGAZINE_SKU_COLUMN = 4;
	public static final String MAGAZINE_ROW_ID = "_id";

	// -------------- ISSUE DEFINITIONS ------------

	public static final String ISSUE_TABLE = "Issue";
	public static final String ISSUE_MAGAZINE_ID_KEY = "magazine_id";
	protected static final int ISSUE_MAGAZINE_ID_COLUMN = 1;
	public static final String ISSUE_STATE_KEY = "state";
	protected static final int ISSUE_STATE_COLUMN = 2;
	public static final String ISSUE_NAME_KEY = "name";
	protected static final int ISSUE_NAME_COLUMN = 3;
	public static final String ISSUE_DATE_KEY = "date";
	protected static final int ISSUE_DATE_COLUMN = 4;
	public static final String ISSUE_PRICE_KEY = "price";
	protected static final int ISSUE_PRICE_COLUMN = 5;
	public static final String ISSUE_PDF_URL_KEY = "pdf_url";
	protected static final int ISSUE_PDF_URL_COLUMN = 6;
	public static final String ISSUE_PREVIEW_PATH_KEY = "preview_path";
	protected static final int ISSUE_PREVIEW_PATH_COLUMN = 7;
	public static final String ISSUE_COVER_PATH_KEY = "cover_path";
	protected static final int ISSUE_COVER_PATH_COLUMN = 8;
	public static final String ISSUE_ISSUE_PATH_KEY = "issue_path";
	protected static final int ISSUE_ISSUE_PATH_COLUMN = 9;
	public static final String ISSUE_SKU_KEY = "sku";
	protected static final int ISSUE_SKU_COLUMN = 10;
	public static final String ISSUE_ROW_ID = "_id";

	// -------- TABLES CREATION ----------

	// Magazine CREATION
	private static final String DATABASE_MAGAZINE_CREATE = "create table "
			+ MAGAZINE_TABLE + " (" + MAGAZINE_ROW_ID
			+ " integer primary key autoincrement" + ", " + MAGAZINE_NAME_KEY
			+ " text  " + ", " + MAGAZINE_VERSION_KEY + " integer  " + ", "
			+ MAGAZINE_BASE_URL_KEY + " text  " + ", " + MAGAZINE_SKU_KEY
			+ " text  " + ");";

	// Issue CREATION
	private static final String DATABASE_ISSUE_CREATE = "create table "
			+ ISSUE_TABLE + " (" + ISSUE_ROW_ID
			+ " integer primary key autoincrement" + ", "
			+ ISSUE_MAGAZINE_ID_KEY + " integer  " + ", " + ISSUE_STATE_KEY
			+ " integer  " + ", " + ISSUE_NAME_KEY + " text  " + ", "
			+ ISSUE_DATE_KEY + " text  " + ", " + ISSUE_PRICE_KEY + " text  "
			+ ", " + ISSUE_PDF_URL_KEY + " text  " + ", "
			+ ISSUE_PREVIEW_PATH_KEY + " text  " + ", " + ISSUE_COVER_PATH_KEY
			+ " text  " + ", " + ISSUE_ISSUE_PATH_KEY + " text  " + ", "
			+ ISSUE_SKU_KEY + " text  " + ");";

	// -------------- MAGAZINE HELPERS ------------------
	public long addMagazine(String name, Integer serial, String version,
			String sku) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(MAGAZINE_NAME_KEY, name);
		contentValues.put(MAGAZINE_VERSION_KEY, serial);
		contentValues.put(MAGAZINE_BASE_URL_KEY, version);
		contentValues.put(MAGAZINE_SKU_KEY, sku);
		return mDb.insert(MAGAZINE_TABLE, null, contentValues);

	}

	public long updateMagazine(long rowIndex, String name, Integer serial,
			String version, String sku) {
		String where = MAGAZINE_ROW_ID + " = " + rowIndex;
		ContentValues contentValues = new ContentValues();
		contentValues.put(MAGAZINE_NAME_KEY, name);
		contentValues.put(MAGAZINE_VERSION_KEY, serial);
		contentValues.put(MAGAZINE_BASE_URL_KEY, version);
		contentValues.put(MAGAZINE_SKU_KEY, sku);
		return mDb.update(MAGAZINE_TABLE, contentValues, where, null);

	}

	public boolean removeMagazine(Long rowIndex) {
		return mDb.delete(MAGAZINE_TABLE, MAGAZINE_ROW_ID + " = " + rowIndex,
				null) > 0;
	}

	public boolean removeAllMagazine() {
		return mDb.delete(MAGAZINE_TABLE, null, null) > 0;
	}

	public Cursor getAllMagazine() {
		return mDb.query(MAGAZINE_TABLE, new String[] { MAGAZINE_ROW_ID,
				MAGAZINE_NAME_KEY, MAGAZINE_VERSION_KEY, MAGAZINE_BASE_URL_KEY,
				MAGAZINE_SKU_KEY }, null, null, null, null, null);
	}

	public Cursor getMagazine(long rowIndex) {
		Cursor res = mDb.query(MAGAZINE_TABLE, new String[] { MAGAZINE_ROW_ID,
				MAGAZINE_NAME_KEY, MAGAZINE_VERSION_KEY, MAGAZINE_BASE_URL_KEY,
				MAGAZINE_SKU_KEY }, MAGAZINE_ROW_ID + " = " + rowIndex, null,
				null, null, null);
		if (res != null) {
			res.moveToFirst();
		}
		return res;
	}

	// -------------- ISSUE HELPERS ------------------
	public long addIssue(Integer magazine_id, Integer state, String name,
			String date, String price, String pdf_url, String preview_path,
			String cover_path, String issue_path, String sku) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(ISSUE_MAGAZINE_ID_KEY, magazine_id);
		contentValues.put(ISSUE_STATE_KEY, state);
		contentValues.put(ISSUE_NAME_KEY, name);
		contentValues.put(ISSUE_DATE_KEY, date);
		contentValues.put(ISSUE_PRICE_KEY, price);
		contentValues.put(ISSUE_PDF_URL_KEY, pdf_url);
		contentValues.put(ISSUE_PREVIEW_PATH_KEY, preview_path);
		contentValues.put(ISSUE_COVER_PATH_KEY, cover_path);
		contentValues.put(ISSUE_ISSUE_PATH_KEY, issue_path);
		contentValues.put(ISSUE_SKU_KEY, sku);
		return mDb.insert(ISSUE_TABLE, null, contentValues);

	}

	public long updateIssue(long rowIndex, Integer magazine_id, Integer state,
			String name, String date, String price, String pdf_url,
			String preview_path, String cover_path, String issue_path,
			String sku) {
		String where = ISSUE_ROW_ID + " = " + rowIndex;
		ContentValues contentValues = new ContentValues();
		contentValues.put(ISSUE_MAGAZINE_ID_KEY, magazine_id);
		contentValues.put(ISSUE_STATE_KEY, state);
		contentValues.put(ISSUE_NAME_KEY, name);
		contentValues.put(ISSUE_DATE_KEY, date);
		contentValues.put(ISSUE_PRICE_KEY, price);
		contentValues.put(ISSUE_PDF_URL_KEY, pdf_url);
		contentValues.put(ISSUE_PREVIEW_PATH_KEY, preview_path);
		contentValues.put(ISSUE_COVER_PATH_KEY, cover_path);
		contentValues.put(ISSUE_ISSUE_PATH_KEY, issue_path);
		contentValues.put(ISSUE_SKU_KEY, sku);
		return mDb.update(ISSUE_TABLE, contentValues, where, null);

	}

	public boolean removeIssue(Long rowIndex) {
		return mDb.delete(ISSUE_TABLE, ISSUE_ROW_ID + " = " + rowIndex, null) > 0;
	}

	public boolean removeAllIssue() {
		return mDb.delete(ISSUE_TABLE, null, null) > 0;
	}

	public Cursor getAllIssue() {
		return mDb.query(ISSUE_TABLE, new String[] { ISSUE_ROW_ID,
				ISSUE_MAGAZINE_ID_KEY, ISSUE_STATE_KEY, ISSUE_NAME_KEY,
				ISSUE_DATE_KEY, ISSUE_PRICE_KEY, ISSUE_PDF_URL_KEY,
				ISSUE_PREVIEW_PATH_KEY, ISSUE_COVER_PATH_KEY,
				ISSUE_ISSUE_PATH_KEY, ISSUE_SKU_KEY }, null, null, null, null,
				null);
	}

	public Cursor getIssue(long rowIndex) {
		Cursor res = mDb.query(ISSUE_TABLE, new String[] { ISSUE_ROW_ID,
				ISSUE_MAGAZINE_ID_KEY, ISSUE_STATE_KEY, ISSUE_NAME_KEY,
				ISSUE_DATE_KEY, ISSUE_PRICE_KEY, ISSUE_PDF_URL_KEY,
				ISSUE_PREVIEW_PATH_KEY, ISSUE_COVER_PATH_KEY,
				ISSUE_ISSUE_PATH_KEY, ISSUE_SKU_KEY }, ISSUE_ROW_ID + " = "
				+ rowIndex, null, null, null, null);
		if (res != null) {
			res.moveToFirst();
		}
		return res;
	}
	
	public Cursor getIssueBySKU(String sku) {
		Cursor res = mDb.query(ISSUE_TABLE, new String[] { ISSUE_ROW_ID,
				ISSUE_MAGAZINE_ID_KEY, ISSUE_STATE_KEY, ISSUE_NAME_KEY,
				ISSUE_DATE_KEY, ISSUE_PRICE_KEY, ISSUE_PDF_URL_KEY,
				ISSUE_PREVIEW_PATH_KEY, ISSUE_COVER_PATH_KEY,
				ISSUE_ISSUE_PATH_KEY, ISSUE_SKU_KEY }, ISSUE_SKU_KEY + " = "
				+ "'" + sku + "'", null, null, null, null);
		if (res != null) {
			res.moveToFirst();
		}
		return res;
	}

	private static class MyDbHelper extends SQLiteOpenHelper {

		public MyDbHelper(Context context, String name, CursorFactory factory,
				int version) {
			super(context, name, factory, version);
		}

		// Called when no database exists in disk and the helper class needs
		// to create a new one.
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_MAGAZINE_CREATE);
			db.execSQL(DATABASE_ISSUE_CREATE);

		}

		// Called when there is a database version mismatch meaning that the
		// version
		// of the database on disk needs to be upgraded to the current version.
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Log the version upgrade.
			Log.w(TAG, "Upgrading from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");

			// Upgrade the existing database to conform to the new version.
			// Multiple
			// previous versions can be handled by comparing _oldVersion and
			// _newVersion
			// values.

			// The simplest case is to drop the old table and create a new one.
			db.execSQL("DROP TABLE IF EXISTS " + MAGAZINE_TABLE + ";");
			db.execSQL("DROP TABLE IF EXISTS " + ISSUE_TABLE + ";");

			// Create a new one.
			onCreate(db);
		}
	}

	/** Dummy object to allow class to compile */
}
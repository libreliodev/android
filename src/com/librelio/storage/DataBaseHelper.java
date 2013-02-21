package com.librelio.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.librelio.model.Magazine;

public class DataBaseHelper extends SQLiteOpenHelper implements BaseColumns{
	private static final int DB_VERSION = 1;
	private static final String DB_NAME = "windDataBase";

	public DataBaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		createTable(db, Magazine.TABLE_MAGAZINES, false);
		createTable(db, Magazine.TABLE_DOWNLOADED_MAGAZINES, true);
	}
	
	private void createTable(SQLiteDatabase db, String tableName, boolean columnSample){
		db.execSQL("CREATE TABLE "+tableName+ "(" 
				+ Magazine.FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ Magazine.FIELD_FILE_NAME + " TEXT, "
				+ Magazine.FIELD_TITLE + " TEXT, "
				+ Magazine.FIELD_DOWNLOAD_DATE + " TEXT, "
				+ Magazine.FIELD_SUBTITLE + " TEXT"
				+ (columnSample ? ", " + Magazine.FIELD_IS_SAMPLE + " INTEGER);" : ");"));
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + Magazine.TABLE_MAGAZINES);
		db.execSQL("DROP TABLE IF EXISTS " + Magazine.TABLE_DOWNLOADED_MAGAZINES);
		onCreate(db);
	}
}

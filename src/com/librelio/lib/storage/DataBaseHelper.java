package com.librelio.lib.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class DataBaseHelper extends SQLiteOpenHelper implements BaseColumns{
	private static final int DB_VERSION = 1;
	private static final String DB_NAME = "windDataBase";

	public DataBaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE "+Magazines.TABLE_NAME+ "(" 
							+ Magazines.FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
							+ Magazines.FIELD_FILE_NAME + " TEXT, "
							+ Magazines.FIELD_TITLE + " TEXT, "
							+ Magazines.FIELD_DOWNLOAD_DATE + " TEXT, "
							+ Magazines.FIELD_SUBTITLE + " TEXT);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + Magazines.TABLE_NAME);
		onCreate(db);
	}
}

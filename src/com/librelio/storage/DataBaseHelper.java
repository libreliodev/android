package com.librelio.storage;

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
		db.execSQL("CREATE TABLE "+MagazineManager.TABLE_NAME+ "(" 
							+ MagazineManager.FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
							+ MagazineManager.FIELD_FILE_NAME + " TEXT, "
							+ MagazineManager.FIELD_TITLE + " TEXT, "
							+ MagazineManager.FIELD_DOWNLOAD_DATE + " TEXT, "
							+ MagazineManager.FIELD_SUBTITLE + " TEXT);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + MagazineManager.TABLE_NAME);
		onCreate(db);
	}
}

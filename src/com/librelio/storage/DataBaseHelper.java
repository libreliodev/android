package com.librelio.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.librelio.model.Magazine;

public class DataBaseHelper extends SQLiteOpenHelper implements BaseColumns{
	private static final int DB_VERSION = 2;
	private static final String DB_NAME = "windDataBase";

    private static DataBaseHelper mInstance = null;

    public static DataBaseHelper getInstance(Context ctx) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new DataBaseHelper(ctx.getApplicationContext());
        }
        return mInstance;
    }

	private DataBaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		createMagazinesTable(db);
		createDownloadedMagazinesTable(db);
        createAssetsTable(db);
	}
	
	private void createMagazinesTable(SQLiteDatabase db){
		db.execSQL("CREATE TABLE " + Magazine.TABLE_MAGAZINES + "("
				+ Magazine.FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ Magazine.FIELD_FILE_NAME + " TEXT, "
				+ Magazine.FIELD_TITLE + " TEXT, "
				+ Magazine.FIELD_DOWNLOAD_DATE + " TEXT, "
				+ Magazine.FIELD_SUBTITLE + " TEXT);");
	}

    private void createDownloadedMagazinesTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE " + Magazine.TABLE_DOWNLOADED_MAGAZINES + "("
                + Magazine.FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Magazine.FIELD_FILE_NAME + " TEXT, "
                + Magazine.FIELD_TITLE + " TEXT, "
                + Magazine.FIELD_DOWNLOAD_DATE + " TEXT, "
                + Magazine.FIELD_SUBTITLE + " TEXT, "
                + Magazine.FIELD_IS_SAMPLE + " INTEGER, "
                + Magazine.FIELD_DOWNLOAD_MANAGER_ID + " INTEGER);");
    }

    private void createAssetsTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE "+Magazine.TABLE_ASSETS+ "("
                + Magazine.FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Magazine.FIELD_FILE_NAME + " TEXT, "
                + Magazine.FIELD_ASSET_FILE_NAME + " TEXT, "
                + Magazine.FIELD_ASSET_IS_DOWNLOADED + " INTEGER, "
                + Magazine.FIELD_DOWNLOAD_MANAGER_ID + " INTEGER);");
    }
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		db.execSQL("DROP TABLE IF EXISTS " + Magazine.TABLE_MAGAZINES);
//		db.execSQL("DROP TABLE IF EXISTS " + Magazine.TABLE_DOWNLOADED_MAGAZINES);
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + Magazine.TABLE_DOWNLOADED_MAGAZINES + " ADD COLUMN " + Magazine.FIELD_DOWNLOAD_MANAGER_ID + " INTEGER;");
            createAssetsTable(db);
        }
//		onCreate(db);
	}
}

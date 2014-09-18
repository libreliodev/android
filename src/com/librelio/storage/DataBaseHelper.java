package com.librelio.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;


public class DataBaseHelper extends SQLiteOpenHelper implements BaseColumns{
	private static final int DB_VERSION = 5;
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

	public static final String TABLE_DOWNLOADED_ITEMS = "DownloadedMagazines";
	public static final String TABLE_DOWNLOADS = "Downloads";
	public static final String FIELD_ID = "_id";
	public static final String FIELD_TITLE = "title";
	public static final String FIELD_SUBTITLE = "subtitle";
	public static final String FIELD_FILE_PATH = "filename";
	public static final String FIELD_ASSET_FILE_PATH = "assetfilename";
	public static final String FIELD_ASSET_URL = "asseturl";
	public static final String FIELD_RETRY_COUNT = "retrycount";
	public static final String FIELD_ASSET_DOWNLOAD_STATUS = "assetdownloadstatus";
	public static final String FIELD_DOWNLOAD_DATE = "downloaddate";
	public static final String FIELD_IS_SAMPLE = "sample";
	public static final String FIELD_DOWNLOAD_STATUS = "downloadstatus";

	private DataBaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		createDownloadedMagazinesTable(db);
        createDownloadsTable(db);
	}

    private void createDownloadedMagazinesTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE " + DataBaseHelper.TABLE_DOWNLOADED_ITEMS + "("
                + DataBaseHelper.FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DataBaseHelper.FIELD_FILE_PATH + " TEXT, "
                + DataBaseHelper.FIELD_TITLE + " TEXT, "
                + DataBaseHelper.FIELD_DOWNLOAD_DATE + " TEXT, "
                + DataBaseHelper.FIELD_SUBTITLE + " TEXT, "
                + DataBaseHelper.FIELD_IS_SAMPLE + " INTEGER, "
                + DataBaseHelper.FIELD_DOWNLOAD_STATUS + " INTEGER DEFAULT -2);");
    }
    
    private void createDownloadsTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE "+DataBaseHelper.TABLE_DOWNLOADS+ "("
                + DataBaseHelper.FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DataBaseHelper.FIELD_FILE_PATH + " TEXT, "
                + DataBaseHelper.FIELD_ASSET_FILE_PATH + " TEXT, "
                + DataBaseHelper.FIELD_ASSET_URL + " TEXT, "
                + DataBaseHelper.FIELD_RETRY_COUNT + " INTEGER, "
                + DataBaseHelper.FIELD_ASSET_DOWNLOAD_STATUS + " INTEGER);");
    }
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		db.execSQL("DROP TABLE IF EXISTS " + Magazine.TABLE_DOWNLOADED_MAGAZINES);
        if (oldVersion < 2) {
//            db.execSQL("ALTER TABLE " + DataBaseHelper.TABLE_DOWNLOADED_MAGAZINES + " ADD COLUMN " + DataBaseHelper.FIELD_DOWNLOAD_MANAGER_ID + " INTEGER;");
//            createAssetsTable(db);
        }
        if (oldVersion < 3) {
		    db.execSQL("DROP TABLE IF EXISTS Magazines");
        }
        if (oldVersion < 4) {
        	db.execSQL("DROP TABLE IF EXISTS Assets");
        	createDownloadsTable(db);
        }
        if (oldVersion < 5) {
        	 db.execSQL("ALTER TABLE " + DataBaseHelper.TABLE_DOWNLOADED_ITEMS + " ADD COLUMN " + DataBaseHelper.FIELD_DOWNLOAD_STATUS + " INTEGER DEFAULT -2;");
        }
//		onCreate(db);
	}
}

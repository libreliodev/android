package com.librelio.storage;

import java.util.ArrayList;
import java.util.List;

import android.app.DownloadManager;
import android.app.NotificationManager;
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
    private final DownloadManager downloadManager;

    public MagazineManager(Context context) {
		super(context);

        downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
	}

	public List<Magazine> getMagazines(boolean hasTestMagazine) {
		List<Magazine> magazines = new ArrayList<Magazine>();
		if (hasTestMagazine) {
			magazines.add(new Magazine(TEST_FILE_NAME, "TEST", "test", "", getContext()));
		}
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getReadableDatabase();
        Cursor c = db.rawQuery("select Magazines._id,Magazines.filename,Magazines.title," +
                "DownloadedMagazines.downloaddate,Magazines.subtitle,DownloadedMagazines.sample," +
                "DownloadedMagazines.downloadmanagerid from " + Magazine
                .TABLE_MAGAZINES + " LEFT JOIN " + Magazine
                .TABLE_DOWNLOADED_MAGAZINES + " ON " +
                Magazine.TABLE_MAGAZINES + "." + Magazine.FIELD_FILE_NAME + "=" + Magazine
                .TABLE_DOWNLOADED_MAGAZINES + "." + Magazine.FIELD_FILE_NAME,
                null);
        while (c.moveToNext()) {
            Magazine magazine = new Magazine(c, getContext());
            // Update download status from DownloadManager
            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(magazine.getDownloadManagerId());
            Cursor cursor = downloadManager.query(q);
            if (cursor.moveToFirst()) {
                magazine.setDownloadStatus(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)));
                long fileSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                long bytesDL = cursor.getLong(cursor.getColumnIndex(DownloadManager
                        .COLUMN_BYTES_DOWNLOADED_SO_FAR));
                magazine.setDownloadProgress((int) ((bytesDL * 100.0f) / fileSize));
            }
            cursor.close();
            magazine.setTotalAssetCount(getTotalAssetCount(magazine));
            magazine.setDownloadedAssetCount(getDownloadedAssetCount(magazine));
            magazines.add(magazine);
        }
        c.close();
		return magazines;
	}

    public List<Magazine> getDownloadedMagazines(boolean hasTestMagazine) {
        List<Magazine> magazines = new ArrayList<Magazine>();
        if (hasTestMagazine) {
            magazines.add(new Magazine(TEST_FILE_NAME, "TEST", "test", "", getContext()));
        }
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getReadableDatabase();
        Cursor c = db.rawQuery("select * from " + Magazine.TABLE_DOWNLOADED_MAGAZINES, null);
        while (c.moveToNext()) {
            Magazine magazine = new Magazine(c, getContext());
            if (magazine.isDownloaded() || magazine.isSampleDownloaded()) {
                magazines.add(magazine);
            }
        }
        c.close();
        return magazines;
    }

	public synchronized void addMagazine(Magazine magazine, String tableName, boolean withSample) {
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(Magazine.FIELD_FILE_NAME, magazine.getFileName());
		cv.put(Magazine.FIELD_DOWNLOAD_DATE, magazine.getDownloadDate());
		cv.put(Magazine.FIELD_TITLE, magazine.getTitle());
		cv.put(Magazine.FIELD_SUBTITLE, magazine.getSubtitle());
		if (withSample){
			cv.put(Magazine.FIELD_IS_SAMPLE, magazine.isSampleForBase());
            cv.put(Magazine.FIELD_DOWNLOAD_MANAGER_ID, magazine.getDownloadManagerId());
		}
        // Add magazine and set id of magazine to newly created row id
		magazine.setId(db.insert(tableName, null, cv));
	}
	
	public synchronized void removeDownloadedMagazine(Magazine magazine) {
        DownloadManager dm = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getWritableDatabase();

        // cancel any download and notification for this magazine
        Cursor c = db.query(Magazine.TABLE_DOWNLOADED_MAGAZINES, new String[] {Magazine.FIELD_DOWNLOAD_MANAGER_ID}, 
                Magazine.FIELD_FILE_NAME + "=?", new String[] {magazine.getFileName()}, null, null, null);
        while (c.moveToNext()) {
            int downloadManagerID = c.getInt(c.getColumnIndex(Magazine.FIELD_DOWNLOAD_MANAGER_ID));
            removeNotification(downloadManagerID);
            dm.remove(downloadManagerID);
        }
        c.close();

        // cancel any asset downloads for this magazine
       c = db.query(Magazine.TABLE_ASSETS, new String[] {Magazine.FIELD_DOWNLOAD_MANAGER_ID},
               Magazine.FIELD_FILE_NAME + "=?", new String[] {magazine.getFileName()}, null, null, null);
        while (c.moveToNext()) {
            int downloadManagerID = c.getInt(c.getColumnIndex(Magazine.FIELD_DOWNLOAD_MANAGER_ID));
//            removeNotification(downloadManagerID);
            dm.remove(downloadManagerID);
        }
        c.close();

        db.delete(Magazine.TABLE_DOWNLOADED_MAGAZINES, Magazine.FIELD_FILE_NAME + "=?", new String[] {magazine.getFileName()});
        db.delete(Magazine.TABLE_ASSETS, Magazine.FIELD_FILE_NAME + "=?", new String[] {magazine.getFileName()});
	}

    public void removeNotification(int notificationId) {
        // Clear downloaded notification for magazine if visible
        NotificationManager mNotificationManager =
                (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationId);
    }

	public int getCount(String tableName) {
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getReadableDatabase();
		int count = (int) DatabaseUtils.longForQuery(db, "select COUNT(" + Magazine.FIELD_ID + ") from " + tableName, null);
		return count;
	}

	public synchronized void cleanMagazines(String tableName){
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getWritableDatabase();
		db.execSQL("DELETE FROM " + tableName + " WHERE 1");
		Log.d(TAG, "at cleanMagazinesListInBase: " + tableName + " table was clean");
	}

	/**
	 * Look up magazine by path
	 * @param path
	 * @return
	 */
	public Magazine findByFileName(String path, String tableName) {
		Magazine magazine = null;
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getReadableDatabase();
		Cursor cursor = db.query(tableName, null, Magazine.FIELD_FILE_NAME + "=?", new String[]{path}, null, null, null);
		if (cursor.moveToFirst()) {
			magazine = new Magazine(cursor, getContext());
		}

		cursor.close();
		return magazine;
	}

    /**
     * Look up magazine by DownloadManager ID
     * @param downloadManagerID
     * @return
     */
    public Magazine findByDownloadManagerID(long downloadManagerID, String tableName) {
        Magazine magazine = null;
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getReadableDatabase();
        Cursor cursor = db.query(tableName, null, Magazine.FIELD_DOWNLOAD_MANAGER_ID + "=?",
                new String[] {String.valueOf(downloadManagerID)}, null, null, null);
        if (cursor.moveToFirst()) {
            magazine = new Magazine(cursor, getContext());
        }

        cursor.close();
        return magazine;
    }

    public synchronized void setAssetDownloaded(long downloadManagerID) {
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(Magazine.FIELD_ASSET_IS_DOWNLOADED, true);
        db.update(Magazine.TABLE_ASSETS, cv, Magazine.FIELD_DOWNLOAD_MANAGER_ID + "=?",
                new String[] {String.valueOf(downloadManagerID)});
    }

    public String getAssetFilename(long downloadManagerID) {
        String assetFilename = null;
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getReadableDatabase();
        Cursor cursor = db.query(Magazine.TABLE_ASSETS, null, Magazine.FIELD_DOWNLOAD_MANAGER_ID + "=?",
                new String[] {String.valueOf(downloadManagerID)}, null, null, null);
        if (cursor.moveToFirst()) {
            assetFilename = cursor.getString(cursor.getColumnIndex(Magazine.FIELD_ASSET_FILE_NAME));
        }
        cursor.close();
        return assetFilename;
    }

    public int getTotalAssetCount(Magazine magazine) {
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getReadableDatabase();
        int count = (int) DatabaseUtils.longForQuery(db, "select COUNT(" + Magazine.FIELD_ID + ") from " + Magazine
                .TABLE_ASSETS + " WHERE " + Magazine.FIELD_FILE_NAME + "=?",
                new String[] {magazine.getFileName()});
        return count;
    }

    public int getDownloadedAssetCount(Magazine magazine) {
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getReadableDatabase();
        int count = (int) DatabaseUtils.longForQuery(db, "select COUNT(" + Magazine.FIELD_ID + ") from " + Magazine
                .TABLE_ASSETS + " WHERE " + Magazine.FIELD_FILE_NAME + "=? AND " + Magazine.FIELD_ASSET_IS_DOWNLOADED
                + "='1'",
                new String[] {magazine.getFileName()});
        return count;
    }

}

package com.librelio.storage;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.Pair;
import android.util.Log;

import com.librelio.base.BaseManager;
import com.librelio.event.ReloadPlistEvent;
import com.librelio.exception.MagazineNotFoundInDatabaseException;
import com.librelio.model.Asset;
import com.librelio.model.DownloadStatusCode;
import com.librelio.model.dictitem.DictItem;
import com.librelio.model.dictitem.DownloadableDictItem;
import com.librelio.model.dictitem.MagazineItem;
import com.librelio.model.dictitem.ProductsItem;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class DownloadsManager extends BaseManager {
	private static final String TAG = "MagazineManager";

	public static final int ASSET_NOT_DOWNLOADED = 0;
	public static final int ASSET_DOWNLOADED = 1;
	public static final int ASSET_DOWNLOAD_FAILED = 2;
	private static final int NUMBER_OF_RETRY_ATTEMPTS = 10;

	public DownloadsManager(Context context) {
		super(context);

	}

	public List<DownloadableDictItem> getDownloadedMagazines() {
		List<DownloadableDictItem> magazines = new ArrayList<>();
		SQLiteDatabase db = DataBaseHelper.getInstance(getContext())
				.getReadableDatabase();
		Cursor c = db.rawQuery("select * from "
				+ DataBaseHelper.TABLE_DOWNLOADED_ITEMS + " where " + DataBaseHelper.FIELD_DOWNLOAD_STATUS + "=101", null);
		while (c.moveToNext()) {
            String filePath = c.getString(c.getColumnIndex(DataBaseHelper.FIELD_FILE_PATH));
            if (filePath.contains("sqlite")) {
                ProductsItem item = new ProductsItem(getContext(), c);
                magazines.add(item);
            } else {
                MagazineItem magazine = new MagazineItem(getContext(), c);
                magazines.add(magazine);
            }
		}
		c.close();
		return magazines;
	}

	public synchronized void addDownload(DictItem magazine, String tableName,
                                         boolean isSample) {
		SQLiteDatabase db = DataBaseHelper.getInstance(getContext())
				.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(DataBaseHelper.FIELD_FILE_PATH, magazine.getFilePath());
		cv.put(DataBaseHelper.FIELD_TITLE, magazine.getTitle());
		cv.put(DataBaseHelper.FIELD_SUBTITLE, magazine.getSubtitle());
		cv.put(DataBaseHelper.FIELD_IS_SAMPLE, isSample);
		db.insert(tableName, null, cv);

		EventBus.getDefault().post(new ReloadPlistEvent());
	}

	public static synchronized void removeDownload(Context context,
                                                   DictItem magazine) {
		SQLiteDatabase db = DataBaseHelper.getInstance(context)
				.getWritableDatabase();

		// cancel any download and notification for this magazine
		// replace this with checking while downloading if cancelled.

		// Cursor c = db.query(DataBaseHelper.TABLE_DOWNLOADED_MAGAZINES, new
		// String[] {DataBaseHelper.FIELD_DOWNLOAD_STATUS},
		// DataBaseHelper.FIELD_FILE_NAME + "=?", new String[]
		// {magazine.getFileName()}, null, null, null);
		// while (c.moveToNext()) {
		// int downloadManagerID =
		// c.getInt(c.getColumnIndex(DataBaseHelper.FIELD_DOWNLOAD_STATUS));

		// dm.remove(downloadManagerID);
		// }
		// c.close();

		// cancel any asset downloads for this magazine
		// Cursor c = db.query(DataBaseHelper.TABLE_DOWNLOADS, new String[]
		// {DataBaseHelper.FIELD_DOWNLOAD_MANAGER_ID},
		// DataBaseHelper.FIELD_FILE_NAME + "=?", new String[]
		// {magazine.getFileName()}, null, null, null);
		// while (c.moveToNext()) {
		// int downloadManagerID =
		// c.getInt(c.getColumnIndex(DataBaseHelper.FIELD_DOWNLOAD_MANAGER_ID));
		// // removeNotification(downloadManagerID);
		// dm.remove(downloadManagerID);
		// }
		// c.close();

		removeNotification(context, magazine.getFilePath().hashCode());

		// Should try to stop assets that are downloading

		db.delete(DataBaseHelper.TABLE_DOWNLOADED_ITEMS,
				DataBaseHelper.FIELD_FILE_PATH + "=?",
				new String[] { magazine.getFilePath() });
		db.delete(DataBaseHelper.TABLE_DOWNLOADS,
				DataBaseHelper.FIELD_FILE_PATH + "=?",
				new String[] { magazine.getFilePath() });

		EventBus.getDefault().post(new ReloadPlistEvent());
	}

	public static void removeNotification(Context context, long notificationId) {
		// Clear downloaded notification for magazine if visible
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel((int) notificationId);
	}

	public synchronized void cleanMagazines(String tableName) {
		SQLiteDatabase db = DataBaseHelper.getInstance(getContext())
				.getWritableDatabase();
		db.execSQL("DELETE FROM " + tableName + " WHERE 1");
		Log.d(TAG, "at cleanMagazinesListInBase: " + tableName
				+ " table was clean");
	}

	// public Magazine findById(long id, String tableName) throws
	// MagazineNotFoundInDatabaseException {
	// Magazine magazine = null;
	// SQLiteDatabase db =
	// DataBaseHelper.getInstance(getContext()).getReadableDatabase();
	// Cursor cursor = db.query(tableName, null, DataBaseHelper.FIELD_ID + "=?",
	// new String[]{String.valueOf(id)}, null, null, null);
	// if (cursor.moveToFirst()) {
	// magazine = new Magazine(getContext(), cursor);
	// }
	// cursor.close();
	// if (magazine == null) {
	// throw new MagazineNotFoundInDatabaseException();
	// }
	// return magazine;
	// }

	public MagazineItem findByFilePath(String path, String tableName)
			throws MagazineNotFoundInDatabaseException {
		MagazineItem magazine = null;
		SQLiteDatabase db = DataBaseHelper.getInstance(getContext())
				.getReadableDatabase();
		Cursor cursor = db.query(tableName, null,
				DataBaseHelper.FIELD_FILE_PATH + "=?", new String[] { path },
				null, null, null);
		if (cursor.moveToFirst()) {
			magazine = new MagazineItem(getContext(), cursor);
		}

		cursor.close();
		if (magazine == null) {
			throw new MagazineNotFoundInDatabaseException();
		}
		return magazine;
	}

	public boolean doesMagazineExistInDatabase(String filePath, String tableName) {
		SQLiteDatabase db = DataBaseHelper.getInstance(getContext())
				.getReadableDatabase();
		Cursor cursor = db.query(tableName, null,
				DataBaseHelper.FIELD_FILE_PATH + "=?",
				new String[] { filePath }, null, null, null);
		if (cursor.moveToFirst()) {
			cursor.close();
			return true;
		}
		cursor.close();
		return false;
	}

//	public synchronized ArrayList<MagazineItem> getMagazinesToDownload() {
//		ArrayList<MagazineItem> magazinesToDownload = new ArrayList<MagazineItem>();
//		SQLiteDatabase db = DataBaseHelper.getInstance(getContext())
//				.getWritableDatabase();
//		Cursor cursor = db.query(
//				DataBaseHelper.TABLE_DOWNLOADED_ITEMS,
//				null,
//				DataBaseHelper.FIELD_DOWNLOAD_STATUS + ">= ? AND "
//						+ DataBaseHelper.FIELD_ASSET_DOWNLOAD_STATUS
//						+ "< ?AND " + DataBaseHelper.FIELD_RETRY_COUNT + "<10",
//				new String[] { String.valueOf(DownloadStatusCode.QUEUED),
//						String.valueOf(DownloadStatusCode.DOWNLOADED),
//						String.valueOf(NUMBER_OF_RETRY_ATTEMPTS) }, null, null,
//				null);
//		if (cursor.moveToFirst()) {
//			while (!cursor.isAfterLast()) {
//				MagazineItem magazine = new MagazineItem(getContext(), cursor);
//				magazinesToDownload.add(magazine);
//				setDownloadStatus(magazine.getFilePath(), DownloadStatusCode.QUEUED);
//				cursor.moveToNext();
//			}
//		}
//		cursor.close();
//		return magazinesToDownload;
//	}

	public synchronized void setDownloadStatus(MagazineItem magazine, long status) {
		SQLiteDatabase db = DataBaseHelper.getInstance(getContext())
				.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(DataBaseHelper.FIELD_DOWNLOAD_STATUS, status);
		db.update(DataBaseHelper.TABLE_DOWNLOADED_ITEMS, cv,
				DataBaseHelper.FIELD_FILE_PATH + "=?",
				new String[] { magazine.getFilePath() });
	}

	public synchronized Pair<Integer, Boolean> getDownloadStatus(String filePath) {
		SQLiteDatabase db = DataBaseHelper.getInstance(getContext())
				.getWritableDatabase();
		Cursor c = db.query(DataBaseHelper.TABLE_DOWNLOADED_ITEMS,
				new String[]{DataBaseHelper.FIELD_DOWNLOAD_STATUS,
						DataBaseHelper.FIELD_IS_SAMPLE},
				DataBaseHelper.FIELD_FILE_PATH + "=?",
				new String[]{filePath}, null, null, null);

		// Set default values if magazine not in database
		int downloadStatus = DownloadStatusCode.NOT_DOWNLOADED;
		boolean isSample = false;
		if (c.moveToFirst()) {
			downloadStatus = c.getInt(c
					.getColumnIndex(DataBaseHelper.FIELD_DOWNLOAD_STATUS));
			isSample = c.getInt(c.getColumnIndex
					(DataBaseHelper.FIELD_IS_SAMPLE)) == 1 ? true : false;
		}
		c.close();;
		return new Pair<>(downloadStatus, isSample);
	}

	public synchronized void addAsset(DownloadableDictItem magazine, String assetFile,
			String assetUrl) {
		SQLiteDatabase db = DataBaseHelper.getInstance(getContext())
				.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(DataBaseHelper.FIELD_FILE_PATH, magazine.getFilePath());
		cv.put(DataBaseHelper.FIELD_ASSET_FILE_PATH, magazine.getItemStorageDir(getContext())
				+ assetFile);
		cv.put(DataBaseHelper.FIELD_ASSET_URL, assetUrl);
		cv.put(DataBaseHelper.FIELD_RETRY_COUNT, 0);
		cv.put(DataBaseHelper.FIELD_ASSET_DOWNLOAD_STATUS, false);
		db.insert(DataBaseHelper.TABLE_DOWNLOADS, null, cv);
	}

	public synchronized ArrayList<Asset> getAssetsToDownload() {
		ArrayList<Asset> assetsToDownload = new ArrayList<Asset>();
		SQLiteDatabase db = DataBaseHelper.getInstance(getContext())
				.getWritableDatabase();
		Cursor cursor = db.query(
				DataBaseHelper.TABLE_DOWNLOADS,
				null,
				DataBaseHelper.FIELD_ASSET_DOWNLOAD_STATUS + "!=? AND "
						+ DataBaseHelper.FIELD_RETRY_COUNT + "<?",
				new String[] { String.valueOf(ASSET_DOWNLOADED),
						String.valueOf(NUMBER_OF_RETRY_ATTEMPTS) }, null, null,
				null);
		if (cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				Asset asset = new Asset(
						cursor.getInt(cursor
								.getColumnIndex(DataBaseHelper.FIELD_ID)),
						cursor.getString(cursor
								.getColumnIndex(DataBaseHelper.FIELD_FILE_PATH)),
						cursor.getString(cursor
								.getColumnIndex(DataBaseHelper.FIELD_ASSET_FILE_PATH)),
						cursor.getString(cursor
								.getColumnIndex(DataBaseHelper.FIELD_ASSET_URL)),
						cursor.getInt(cursor
								.getColumnIndex(DataBaseHelper.FIELD_RETRY_COUNT)));
				assetsToDownload.add(asset);
				setAssetStatus(asset.id, DownloadsManager.ASSET_NOT_DOWNLOADED);
				cursor.moveToNext();
			}
		}
		cursor.close();
		return assetsToDownload;
	}

	public synchronized void setAssetStatus(long id, int status) {
		SQLiteDatabase db = DataBaseHelper.getInstance(getContext())
				.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(DataBaseHelper.FIELD_ASSET_DOWNLOAD_STATUS, status);
		db.update(DataBaseHelper.TABLE_DOWNLOADS, cv, DataBaseHelper.FIELD_ID
				+ "=?", new String[] { String.valueOf(id) });
	}

	public synchronized void incrementRetryCount(Asset asset) {
		SQLiteDatabase db = DataBaseHelper.getInstance(getContext())
				.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(DataBaseHelper.FIELD_RETRY_COUNT, asset.retryCount + 1);
		db.update(DataBaseHelper.TABLE_DOWNLOADS, cv, DataBaseHelper.FIELD_ID
				+ "=?", new String[] { String.valueOf(asset.id) });
	}

	public static int getTotalAssetCount(Context context, DownloadableDictItem magazine) {
		SQLiteDatabase db = DataBaseHelper.getInstance(context)
				.getReadableDatabase();
		int count = (int) DatabaseUtils.longForQuery(db, "select COUNT("
				+ DataBaseHelper.FIELD_ID + ") from "
				+ DataBaseHelper.TABLE_DOWNLOADS + " WHERE "
				+ DataBaseHelper.FIELD_FILE_PATH + "=?  AND "
				+ DataBaseHelper.FIELD_RETRY_COUNT + "<"
				+ NUMBER_OF_RETRY_ATTEMPTS,
				new String[] { magazine.getFilePath() });
		return count;
	}

	public static int getDownloadedAssetCount(Context context, DownloadableDictItem magazine) {
		SQLiteDatabase db = DataBaseHelper.getInstance(context)
				.getReadableDatabase();
		int count = (int) DatabaseUtils.longForQuery(
				db,
				"select COUNT(" + DataBaseHelper.FIELD_ID + ") from "
						+ DataBaseHelper.TABLE_DOWNLOADS + " WHERE "
						+ DataBaseHelper.FIELD_FILE_PATH + "=? AND "
						+ DataBaseHelper.FIELD_ASSET_DOWNLOAD_STATUS
						+ "=? AND " + DataBaseHelper.FIELD_RETRY_COUNT + "<"
						+ NUMBER_OF_RETRY_ATTEMPTS,
				new String[] { magazine.getFilePath(),
						String.valueOf(ASSET_DOWNLOADED) });
		return count;
	}

	public static int getFailedAssetCount(Context context, DownloadableDictItem magazine) {
		SQLiteDatabase db = DataBaseHelper.getInstance(context)
				.getReadableDatabase();
		int count = (int) DatabaseUtils.longForQuery(
				db,
				"select COUNT(" + DataBaseHelper.FIELD_ID + ") from "
						+ DataBaseHelper.TABLE_DOWNLOADS + " WHERE "
						+ DataBaseHelper.FIELD_FILE_PATH + "=? AND "
						+ DataBaseHelper.FIELD_ASSET_DOWNLOAD_STATUS
						+ "=? AND " + DataBaseHelper.FIELD_RETRY_COUNT + "<"
						+ NUMBER_OF_RETRY_ATTEMPTS,
				new String[] { magazine.getFilePath(),
						String.valueOf(ASSET_DOWNLOAD_FAILED) });
		return count;
	}

}

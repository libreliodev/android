package com.librelio.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.artifex.mupdfdemo.LinkInfoExternal;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.analytics.tracking.android.EasyTracker;
import com.librelio.LibrelioApplication;
import com.librelio.activity.MuPDFActivity;
import com.librelio.event.LoadPlistEvent;
import com.librelio.event.MagazineDownloadedEvent;
import com.librelio.event.MagazinesUpdatedEvent;
import com.librelio.exception.MagazineNotFoundInDatabaseException;
import com.librelio.lib.utils.PDFParser;
import com.librelio.model.DownloadStatusCode;
import com.librelio.model.dictitem.DictItem;
import com.librelio.model.dictitem.MagazineItem;
import com.librelio.model.dictitem.ProductsItem;
import com.librelio.storage.DataBaseHelper;
import com.librelio.storage.MagazineManager;
import com.niveales.wind.BuildConfig;
import com.niveales.wind.R;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import de.greenrobot.event.EventBus;

public class ProductsDownloadService extends WakefulIntentService {

	private static final String TAG = "MagazineDownloadService";

	private MagazineManager manager;

	private final static int BUFFER_SIZE = 1024 * 8;

	private static final String TEMP_FILE_SUFFIX = ".temp";

	private static final String EXTRA_IS_SAMPLE = "extra_is_sample";

	private static final String EXTRA_TEMP_URL_KEY = "temp_url_key";

	private static final String EXTRA_IS_TEMP = "is_temp";

	public ProductsDownloadService() {
		super("productsdownload");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	protected void doWakefulWork(Intent intent) {

		if (BuildConfig.DEBUG) {
			Log.d(TAG, "ProductsDownloadService doWakefulWork");
		}
		// manager = new MagazineManager(this);
		// // downloadPendingMagazines();
		// downloadMagazine(intent);
		downloadProducts(intent);
	}

	// private void downloadPendingMagazines() {
	// ArrayList<Magazine> magazinesToDownload =
	// manager.getMagazinesToDownload();
	// for (Magazine magazine: magazinesToDownload) {
	// downloadMagazine(magazine);
	// }
	// }

	private void downloadProducts(Intent intent) {
//		MagazineItem magazine = null;
//		// TODO Fix this properly - why isn't the magazine in the database
//		try {
//			magazine = manager.findByFilePath(
//					intent.getStringExtra(DataBaseHelper.FIELD_FILE_PATH),
//					DataBaseHelper.TABLE_DOWNLOADED_ITEMS);
//		} catch (MagazineNotFoundInDatabaseException e1) {
//			e1.printStackTrace();
//			Handler h = new Handler(getMainLooper());
//
//			h.post(new Runnable() {
//				@Override
//				public void run() {
//
//					Toast.makeText(ProductsDownloadService.this,
//							"Magazine not found - please try again",
//							Toast.LENGTH_SHORT).show();
//				}
//			});
//			return;
//		}

		ProductsItem item = new ProductsItem(this, "faketitle", "fakesubtitle",
				intent.getStringExtra(DataBaseHelper.FIELD_FILE_PATH));

		String fileUrl = item.getItemUrl();
		String filePath = item.getItemFileName();
		boolean isSample = intent.getBooleanExtra(EXTRA_IS_SAMPLE, false);
//		if (isSample) {
//			// If sample
//			fileUrl = magazine.getSamplePdfUrl();
//			filePath = magazine.getSamplePdfPath();
//		} else
			if (intent.getBooleanExtra(EXTRA_IS_TEMP, false)) {
			// If temp url
			fileUrl = intent.getStringExtra(EXTRA_TEMP_URL_KEY);
		}
		Log.d(TAG, "isSample: " + isSample + "\nfileUrl: " + fileUrl
				+ "\nfilePath: " + filePath);
		EasyTracker.getInstance().setContext(this);
		EasyTracker.getTracker().sendView(
				"Downloading/" + FilenameUtils.getBaseName(filePath));

		String tempFilePath = filePath + TEMP_FILE_SUFFIX;

		Request.Builder requestBuilder = new Request.Builder().url(fileUrl);

		File currentFile = new File(tempFilePath);

		// FIXME Download never resumes because the magazine directory is
		// deleted just before starting download
		if (currentFile.exists()) {
			// Add Range header to restart download
			requestBuilder.addHeader("Range", "bytes=" + currentFile.length()
					+ "-");

			if (BuildConfig.DEBUG) {
				Log.v(TAG, "File is not complete, resuming download.");
				Log.v(TAG, "Current file length:" + currentFile.length()
						+ " totalSize:");
				// totalSize);
			}
		}
		try {
			Response response = LibrelioApplication.getOkHttpClient()
					.newCall(requestBuilder.build()).execute();

			long totalSize = response.body().contentLength();

			if (response.code() == 200) {
			}

			RandomAccessFile out = new RandomAccessFile(tempFilePath, "rw");

			byte[] buffer = new byte[BUFFER_SIZE];

			BufferedInputStream in = new BufferedInputStream(response.body()
					.byteStream(), BUFFER_SIZE);
			// if (BuildConfig.DEBUG) {
			// Log.v(TAG, "current file length " + out.length());
			// }

			int bytesCount = 0, bytesRead = 0;

			try {

				out.seek(out.length());

				while (true) {
					// while (!interrupt) {
					bytesRead = in.read(buffer, 0, BUFFER_SIZE);
					if (bytesRead == -1) {
						break;
					}
					out.write(buffer, 0, bytesRead);
					manager.setDownloadStatus(item.getFilePath(),
							(int) ((bytesCount * 100) / totalSize));
					bytesCount += bytesRead;

					// check if download exists in database - if not, cancel
					// download
					// TODO don't check too often - log to see how often this is
					// called
					if (!manager.doesMagazineExistInDatabase(intent
							.getStringExtra(DataBaseHelper.FIELD_FILE_PATH),
							DataBaseHelper.TABLE_DOWNLOADED_ITEMS)) {
						Log.d(TAG, "DOWNLOAD CANCELLED!");
						return;
					}
				}
			} finally {
				out.close();
				if (in != null) {
					in.close();
				}
				in.close();
			}

			Log.d(TAG, "Downloaded " + item.getFilePath());

			File tempFile = new File(filePath + TEMP_FILE_SUFFIX);
			tempFile.renameTo(new File(filePath));

			MagazineManager.removeDownloadedMagazine(this, item);
			manager.addMagazine(item,
					DataBaseHelper.TABLE_DOWNLOADED_ITEMS, true);

//			addAssetsToDatabase(this, magazine);

			manager.setDownloadStatus(item.getFilePath(),
					DownloadStatusCode.DOWNLOADED);

			EventBus.getDefault().post(new LoadPlistEvent());
//			EventBus.getDefault().post(new MagazineDownloadedEvent(magazine));

//			NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
//					this)
//					.setSmallIcon(R.drawable.ic_launcher)
//					.setContentTitle(
//							magazine.getTitle() + (isSample ? " sample" : "")
//									+ " downloaded")
//					.setContentText("Click to read");
//
//			// Create large icon from magazine cover png
//			Resources res = getResources();
//			int height = (int) res
//					.getDimension(android.R.dimen.notification_large_icon_height);
//			int width = (int) res
//					.getDimension(android.R.dimen.notification_large_icon_width);
//			// mBuilder.setLargeIcon(SystemHelper.decodeSampledBitmapFromFile(
//			// magazine.getPngPath(), height, width));
//
//			// TODO show magazine cover as large image
//
//			Intent resultIntent = new Intent(this, MuPDFActivity.class);
//			resultIntent.setAction(Intent.ACTION_VIEW);
//			resultIntent.setData(Uri.parse(isSample ? magazine
//					.getSamplePdfPath() : magazine.getItemFileName()));
//			resultIntent.putExtra(DataBaseHelper.FIELD_TITLE,
//					magazine.getTitle());
//
//			TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//			stackBuilder.addParentStack(MuPDFActivity.class);
//			stackBuilder.addNextIntent(resultIntent);
//			PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
//					0, PendingIntent.FLAG_UPDATE_CURRENT);
//			mBuilder.setContentIntent(resultPendingIntent);
//			mBuilder.setAutoCancel(true);
//			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//			mNotificationManager.notify(magazine.getFilePath().hashCode(),
//					mBuilder.build());

			EventBus.getDefault().post(new MagazinesUpdatedEvent());
//			AssetDownloadService.startAssetDownloadService(this);
		} catch (IOException e) {
			e.printStackTrace();
			manager.setDownloadStatus(item.getFilePath(),
					DownloadStatusCode.FAILED);
			Log.d(TAG, "failed to download " + item.getFilePath());
		}
	}

	private void addAssetsToDatabase(Context context, MagazineItem magazine) {
		Log.d(TAG, "addAssetsToDatabase " + magazine.getFilePath());

		SQLiteDatabase db = DataBaseHelper.getInstance(context)
				.getWritableDatabase();
		db.beginTransaction();

		ArrayList<String> assetsNames = new ArrayList<String>();
		//
		String filePath = magazine.isSample() ? magazine.getSamplePdfPath()
				: magazine.getItemFileName();
		PDFParser linkGetter = new PDFParser(filePath);
		SparseArray<LinkInfoExternal[]> linkBuf = linkGetter.getLinkInfo();
		if (linkBuf == null) {
			Log.d(TAG, "There is no links");
			return;
		}
		for (int i = 0; i < linkBuf.size(); i++) {
			int key = linkBuf.keyAt(i);
			Log.d(TAG, "--- i = " + i);
			if (linkBuf.get(key) != null) {
				for (int j = 0; j < linkBuf.get(key).length; j++) {
					// LinkInfoExternal extLink = linkBuf.get(key)[j];
					String link = linkBuf.get(key)[j].url;
					Log.d(TAG, "link[" + j + "] = " + link);
					String local = "http://localhost";
					if (link.startsWith(local)) {
						int startIdx = local.length() + 1;
						int finIdx = link.length();
						if (link.contains("?")) {
							finIdx = link.indexOf("?");
						}
						String assetsFileName = link
								.substring(startIdx, finIdx);
						assetsNames.add(assetsFileName);

						String uriString = magazine.getAssetUrl(assetsFileName);
						Log.d(TAG, "   file: " + assetsFileName);
						Log.d(TAG, "  link to download: " + uriString);

						manager.addAsset(magazine, assetsFileName, uriString);
					}
				}
			}
		}
		try {
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public static void startProductsDownload(Context context, DictItem item,
			boolean isSample) {
		startProductsDownload(context, item, false, null, isSample);
	}

	public static void startProductsDownload(Context context, DictItem item,
			boolean isTemp, String tempUrlKey, boolean isSample) {

		MagazineManager magazineManager = new MagazineManager(context);
		MagazineManager.removeDownloadedMagazine(context, item);
		magazineManager.addMagazine(item,
				DataBaseHelper.TABLE_DOWNLOADED_ITEMS, true);
		magazineManager.setDownloadStatus(item.getFilePath(),
				DownloadStatusCode.QUEUED);
		// magazine.clearMagazineDir();
		item.makeMagazineDir(context);

		EventBus.getDefault().post(new LoadPlistEvent());

		Intent intent = new Intent(context, ProductsDownloadService.class);
		intent.putExtra(DataBaseHelper.FIELD_FILE_PATH, item.getFilePath());
		intent.putExtra(EXTRA_IS_TEMP, isTemp);
		intent.putExtra(EXTRA_TEMP_URL_KEY, tempUrlKey);
		intent.putExtra(EXTRA_IS_SAMPLE, isSample);
		ProductsDownloadService.sendWakefulWork(context, intent);
	}

}

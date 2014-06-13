package com.librelio.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.util.SparseArray;

import com.artifex.mupdf.LinkInfoExternal;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.analytics.tracking.android.EasyTracker;
import com.librelio.LibrelioApplication;
import com.librelio.activity.MuPDFActivity;
import com.librelio.event.ChangeInDownloadedMagazinesEvent;
import com.librelio.event.LoadPlistEvent;
import com.librelio.event.MagazineDownloadedEvent;
import com.librelio.lib.utils.PDFParser;
import com.librelio.model.DownloadStatus;
import com.librelio.model.Magazine;
import com.librelio.storage.DataBaseHelper;
import com.librelio.storage.MagazineManager;
import com.librelio.utils.SystemHelper;
import com.niveales.wind.BuildConfig;
import com.niveales.wind.R;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import de.greenrobot.event.EventBus;

public class MagazineDownloadService extends WakefulIntentService {

	private static final String TAG = "MagazineDownloadService";

	private MagazineManager manager;
	
	private final static int BUFFER_SIZE = 1024 * 8;

	private static final String TEMP_FILE_SUFFIX = ".temp";

	public MagazineDownloadService() {
		super("magazinedownload");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	protected void doWakefulWork(Intent intent) {

		if (BuildConfig.DEBUG) {
			Log.d(TAG, "MagazineDownloadService doWakefulWork");
		}
		manager = new MagazineManager(this);
//		downloadPendingMagazines();
		downloadMagazine(intent);
	}

//	private void downloadPendingMagazines() {
//		ArrayList<Magazine> magazinesToDownload = manager.getMagazinesToDownload();
//		for (Magazine magazine: magazinesToDownload) {
//			downloadMagazine(magazine);
//		}
//	}

	private void downloadMagazine(Intent intent) {
		Magazine magazine = manager.findById(
				intent.getLongExtra(DataBaseHelper.FIELD_ID, -1),
				DataBaseHelper.TABLE_DOWNLOADED_MAGAZINES);

		String fileUrl = magazine.getItemUrl();
		String filePath = magazine.getFilename();
		if (magazine.isSample()) {
			// If sample
			fileUrl = magazine.getSamplePdfUrl();
			filePath = magazine.getSamplePdfPath();
		} else if (intent.getBooleanExtra("is_temp", false)) {
			// If temp url
			fileUrl = intent.getStringExtra("temp_url_key");
		}
		Log.d(TAG, "isSample: " + magazine.isSample() + "\nfileUrl: " + fileUrl
				+ "\nfilePath: " + filePath);
		EasyTracker.getInstance().setContext(this);
		EasyTracker.getTracker().sendView(
				"Downloading/" + FilenameUtils.getBaseName(filePath));
		
		String tempFilePath = filePath + TEMP_FILE_SUFFIX;
		
		Request.Builder requestBuilder = new Request.Builder().url(fileUrl);
		
		File currentFile = new File(tempFilePath);
		long previousFileSize = 0;
		
		if (currentFile.exists()) {
			previousFileSize = currentFile.length();
			requestBuilder.addHeader("Range", "bytes=" + currentFile.length()
					+ "-");
			
			 if (BuildConfig.DEBUG) {
			 Log.v(TAG, "File is not complete, resuming download.");
			 Log.v(TAG, "Current file length:" + currentFile.length() + " totalSize:");
//			 totalSize);
			 }
		}
		try {
			Response response = LibrelioApplication.getOkHttpClient().newCall(requestBuilder.build()).execute();

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
					manager.setDownloadStatus(magazine.getId(),
							(int) ((bytesCount * 100) / totalSize));
					bytesCount += bytesRead;
					
					// TODO don't check too often - log to see how often this is called
					if (!manager.doesMagazineExist(intent.getLongExtra(DataBaseHelper.FIELD_ID, -1),
							DataBaseHelper.TABLE_DOWNLOADED_MAGAZINES)) {
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

			Log.d(TAG, "Downloaded " + magazine.getFileName());
			
			File tempFile = new File(filePath + TEMP_FILE_SUFFIX);
			tempFile.renameTo(new File(filePath));

			Date date = Calendar.getInstance().getTime();
			// String downloadDate = new
			// SimpleDateFormat(" dd.MM.yyyy").format(date);
			String downloadDate = DateFormat.getDateInstance().format(date);
			magazine.setDownloadDate(downloadDate);
			MagazineManager.removeDownloadedMagazine(this, magazine);
			manager.addMagazine(magazine,
					DataBaseHelper.TABLE_DOWNLOADED_MAGAZINES, true);
			
			addAssetsToDatabase(this, magazine);
			
			manager.setDownloadStatus(magazine.getId(),
					DownloadStatus.DOWNLOADED);

//			magazine.makeCompleteFile(magazine.isSample());
			
			EventBus.getDefault().post(new LoadPlistEvent());
			EventBus.getDefault().post(new MagazineDownloadedEvent(magazine));

			NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
					this)
					.setSmallIcon(R.drawable.ic_launcher)
					.setContentTitle(
							magazine.getTitle()
									+ (magazine.isSample() ? " sample" : "")
									+ " downloaded")
					.setContentText("Click to read");

			// Create large icon from magazine cover png
			Resources res = getResources();
			int height = (int) res
					.getDimension(android.R.dimen.notification_large_icon_height);
			int width = (int) res
					.getDimension(android.R.dimen.notification_large_icon_width);
			mBuilder.setLargeIcon(SystemHelper.decodeSampledBitmapFromFile(
					magazine.getPngPath(), height, width));

			// TODO show magazine cover as large image

			Intent resultIntent = new Intent(this, MuPDFActivity.class);
			resultIntent.setAction(Intent.ACTION_VIEW);
			resultIntent.setData(Uri.parse(magazine.isSample() ? magazine
					.getSamplePdfPath() : magazine.getFilename()));
			resultIntent.putExtra(DataBaseHelper.FIELD_TITLE,
					magazine.getTitle());

			TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
			stackBuilder.addParentStack(MuPDFActivity.class);
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
					0, PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder.setContentIntent(resultPendingIntent);
			mBuilder.setAutoCancel(true);
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.notify(magazine.getFileName().hashCode(),
					mBuilder.build());

			EventBus.getDefault().post(new ChangeInDownloadedMagazinesEvent());
			AssetDownloadService.startAssetDownloadService(this);
		} catch (IOException e) {
			e.printStackTrace();
			manager.setDownloadStatus(magazine.getId(), DownloadStatus.FAILED);
			Log.d(TAG, "failed to download " + magazine.getFileName());
		}
	}

	private void addAssetsToDatabase(Context context, Magazine magazine) {
		Log.d(TAG, "addAssetsToDatabase " + magazine.getFileName());

		SQLiteDatabase db = DataBaseHelper.getInstance(context)
				.getWritableDatabase();
		db.beginTransaction();

		ArrayList<String> assetsNames = new ArrayList<String>();
		//
		String filePath = magazine.isSample() ? magazine.getSamplePdfPath()
				: magazine.getFilename();
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
						String assetsFile = link.substring(startIdx, finIdx);
						assetsNames.add(assetsFile);

						String uriString = Magazine.getServerBaseURL(magazine
								.getFileName()) + assetsFile;
						Log.d(TAG, "   file: " + assetsFile);
						Log.d(TAG, "  link to download: " + uriString);

						manager.addAsset(magazine, assetsFile, uriString);
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

	public static void startMagazineDownload(Context context,
			Magazine currentMagazine) {
		startMagazineDownload(context, currentMagazine, false, null);
	}

	public static void startMagazineDownload(Context context,
			Magazine magazine, boolean isTemp, String tempUrlKey) {

		MagazineManager magazineManager = new MagazineManager(context);
		MagazineManager.removeDownloadedMagazine(context, magazine);
		magazineManager.addMagazine(magazine,
				DataBaseHelper.TABLE_DOWNLOADED_MAGAZINES, true);
		magazineManager.setDownloadStatus(magazine.getId(),
				DownloadStatus.QUEUED);
		// magazine.clearMagazineDir();
		magazine.makeMagazineDir();
		EventBus.getDefault().post(new LoadPlistEvent());

		Intent intent = new Intent(context, MagazineDownloadService.class);
		intent.putExtra(DataBaseHelper.FIELD_ID, magazine.getId());
		intent.putExtra("is_temp", isTemp);
		intent.putExtra("temp_url_key", tempUrlKey);
		MagazineDownloadService.sendWakefulWork(context, intent);
	}

}

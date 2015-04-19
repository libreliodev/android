package com.librelio.service;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.librelio.LibrelioApplication;
import com.librelio.event.ReloadPlistEvent;
import com.librelio.exception.FileAlreadyExistException;
import com.librelio.exception.NoMemoryException;
import com.librelio.model.Asset;
import com.librelio.storage.DownloadsManager;
import com.librelio.utils.StorageUtils;
import com.niveales.wind.BuildConfig;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import de.greenrobot.event.EventBus;

public class AssetDownloadService extends WakefulIntentService {

	private static final String TAG = "AssetDownloadService";
	
	private DownloadsManager manager;
	
	private static final String TEMP_SUFFIX = ".temp";
	private final static int BUFFER_SIZE = 1024 * 8;

	public AssetDownloadService() {
		super("assetdownload");
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		manager = new DownloadsManager(this);
		downloadPendingAssets();
	}

	private void downloadPendingAssets() {

		for (Asset asset : manager.getAssetsToDownload()) {
			try {
				download(asset);
			} catch (FileAlreadyExistException ignored) {
			} catch (NetworkErrorException e) {
				e.printStackTrace();
				assetDownloadFailed(asset);
			} catch (NoMemoryException e) {
				e.printStackTrace();
				assetDownloadFailed(asset);
			} catch (IOException e) {
				e.printStackTrace();
				assetDownloadFailed(asset);
			}
		}
	}

	private void assetDownloadFailed(Asset asset) {
		manager.incrementRetryCount(asset);
		manager.setAssetStatus(asset.id, DownloadsManager.ASSET_DOWNLOAD_FAILED);
		Log.w(TAG, "asset download failed");
	}

	private void download(Asset asset)
			throws NetworkErrorException, IOException,
			FileAlreadyExistException, NoMemoryException {

		File file = new File(asset.assetfilename);
		File tempFile = new File(asset.assetfilename + TEMP_SUFFIX);
		long previousFileSize = 0;

		// /*
		// * check net work
		// */
		// // if (!NetworkUtils.isNetworkAvailable(context)) {
		// // throw new NetworkErrorException("Network blocked.");
		// // }

		Request.Builder requestBuilder = new Request.Builder()
				.url(asset.assetUrl);

		if (tempFile.exists()) {
			previousFileSize = tempFile.length();
			requestBuilder.addHeader("Range", "bytes=" + tempFile.length()
					+ "-");
			//
			// if (DEBUG) {
			// Log.v(TAG, "File is not complete, download now.");
			// Log.v(TAG, "File length:" + tempFile.length() + " totalSize:" +
			// totalSize);
			// }
		}

		Response response = LibrelioApplication.getOkHttpClient().newCall(requestBuilder.build()).execute();

		long totalSize = response.body().contentLength();

		if (BuildConfig.DEBUG) {
			Log.d("asset size", totalSize + " bytes");
		}

		if (file.exists() && totalSize == file.length()) {
			if (BuildConfig.DEBUG) {
                Log.v(null, "Output file already exists and is correct size. Marking as downloaded.");
            }
			manager.setAssetStatus(asset.id, DownloadsManager.ASSET_DOWNLOADED);
			EventBus.getDefault().post(new ReloadPlistEvent());

			throw new FileAlreadyExistException(
					"Output file already exists. Skipping download.");
		}

		// Check storage space
		long storage = StorageUtils.getAvailableStorage();
		if (BuildConfig.DEBUG) {
			Log.i(null, "available storage:" + storage + " asset size:"
					+ totalSize);
		}

		if (totalSize - tempFile.length() > storage) {
			throw new NoMemoryException("Not enough storage space.");
		}

		RandomAccessFile outputStream = new RandomAccessFile(tempFile, "rw");

		InputStream input = response.body().byteStream();
		int bytesDownloaded = download(input, outputStream);

		if ((bytesDownloaded) != totalSize && totalSize != -1) {
			// && !interrupt) {
			throw new IOException("Download incomplete: previous file size:"
					+ previousFileSize + " bytes downloaded:" + bytesDownloaded
					+ " previous+downloaded:"
					+ (previousFileSize + bytesDownloaded) + " != totalsize:"
					+ totalSize);
		}

		StorageUtils.move(tempFile.getPath(), file.getPath());
		manager.setAssetStatus(asset.id, DownloadsManager.ASSET_DOWNLOADED);
		EventBus.getDefault().post(new ReloadPlistEvent());

		if (BuildConfig.DEBUG) {
			Log.v(TAG, "Download completed successfully.");
		}

		//
		// return bytesCopied;
		//
	}

	public int download(InputStream input, RandomAccessFile out)
			throws IOException, NetworkErrorException {

		byte[] buffer = new byte[BUFFER_SIZE];

		BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);
		// if (BuildConfig.DEBUG) {
		// Log.v(TAG, "current file length " + out.length());
		// }

		int count = 0, n = 0;
		long errorBlockTimePreviousTime = -1, expireTime = 0;

		try {

			out.seek(out.length());

			while (true) {
				// while (!interrupt) {
				n = in.read(buffer, 0, BUFFER_SIZE);
				if (n == -1) {
					break;
				}
				out.write(buffer, 0, n);
				count += n;

				/*
				 * check network
				 */
				// if (!NetworkUtils.isNetworkAvailable(context)) {
				// throw new NetworkErrorException("Network blocked.");
				// }
				//
				// if (networkSpeed == 0) {
				// if (errorBlockTimePreviousTime > 0) {
				// expireTime = System.currentTimeMillis() -
				// errorBlockTimePreviousTime;
				// if (expireTime > TIME_OUT) {
				// throw new ConnectTimeoutException("connection time out.");
				// }
				// } else {
				// errorBlockTimePreviousTime = System.currentTimeMillis();
				// }
				// } else {
				// expireTime = 0;
				// errorBlockTimePreviousTime = -1;
				// }
			}
		} finally {
			out.close();
			if (in != null) {
				in.close();
			}
			input.close();
		}

		return count;

	}

	public static void startAssetDownloadService(Context context) {
		WakefulIntentService.sendWakefulWork(context,
				AssetDownloadService.class);
	}
	
}

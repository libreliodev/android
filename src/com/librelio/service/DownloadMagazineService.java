package com.librelio.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;

import android.accounts.NetworkErrorException;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
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
import com.librelio.activity.MuPDFActivity;
import com.librelio.event.ChangeInDownloadedMagazinesEvent;
import com.librelio.event.LoadPlistEvent;
import com.librelio.event.MagazineDownloadedEvent;
import com.librelio.exception.FileAlreadyExistException;
import com.librelio.exception.NoMemoryException;
import com.librelio.lib.utils.PDFParser;
import com.librelio.model.Asset;
import com.librelio.model.Magazine;
import com.librelio.storage.DataBaseHelper;
import com.librelio.storage.MagazineManager;
import com.librelio.utils.StorageUtils;
import com.librelio.utils.SystemHelper;
import com.niveales.wind.BuildConfig;
import com.niveales.wind.R;
import com.squareup.okhttp.OkHttpClient;

import de.greenrobot.event.EventBus;

public class DownloadMagazineService extends WakefulIntentService {
    private static final String TAG = "DownloadMagazineService";
	private static final String TEMP_SUFFIX = ".temp";
	public final static int TIME_OUT = 30000;
    private final static int BUFFER_SIZE = 1024 * 8;
    private DownloadManager mDManager;
    private MagazineManager manager;

    public DownloadMagazineService() {
        super("magazineprocessing");
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
	protected void doWakefulWork(Intent intent) {
    	
    	Log.d(TAG, "DownloadMagazineService start");

        mDManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadManagerID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);

        if (BuildConfig.DEBUG) {
        	DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(downloadManagerID);
            Cursor c = mDManager.query(q);
            if (c.moveToFirst()) {
            	Log.d("DOWNLOAD", downloadManagerID +
        			", Download Status: " + c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
            }
        	c.close();
        }
        
        manager = new MagazineManager(this);
        Magazine magazine = manager.findByDownloadManagerID(downloadManagerID, DataBaseHelper.TABLE_DOWNLOADED_MAGAZINES);

        if (magazine != null) {
            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(downloadManagerID);
            Cursor c = mDManager.query(q);
            if (c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                	Log.d(TAG, "Download manager says download successful for " + magazine.getFileName());
                    // process download
                	String srcFileName = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
            		File srcFile = new File(srcFileName);

            		if (srcFile.length() == 0) {
            			// download failed - retry
            			Log.d(TAG, "But the file is actually 0 bytes so retry " + magazine.getFileName());
            			String url = c.getString(c.getColumnIndex(DownloadManager.COLUMN_URI));
            	        String filePath = magazine.getItemPath();
            	        if (magazine.isSample()) {
            	            filePath = magazine.getSamplePdfPath();
            	        }
            	        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            	        request.setVisibleInDownloadsUi(false).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            	                .setDescription(magazine.getSubtitle()).setTitle(magazine.getTitle() + (magazine.isSample() ? " Sample" : ""))
            	                .setDestinationInExternalFilesDir(this, null, FilenameUtils.getName(filePath));
            	        //TODO should use cache directory?
            	        magazine.setDownloadManagerId(mDManager.enqueue(request));

            	        MagazineManager magazineManager = new MagazineManager(this);
            	        MagazineManager.removeDownloadedMagazine(this, magazine);
            	        magazineManager.addMagazine(
            	                magazine,
            	                DataBaseHelper.TABLE_DOWNLOADED_MAGAZINES,
            	                true);
                        return;
            		}

                    magazine.clearMagazineDir();
                    magazine.makeMagazineDir();
                    Log.d(TAG, "Attempting to move file for " + magazine.getFileName());
                    int bytesMoved = StorageUtils.move(srcFileName, magazine.isSample() ?
                            magazine.getSamplePdfPath() :
                            magazine.getItemPath());
                    Log.d(TAG, bytesMoved + " bytes moved for " + magazine.getFileName());

                    Date date = Calendar.getInstance().getTime();
//                    String downloadDate = new SimpleDateFormat(" dd.MM.yyyy").format(date);
                    String downloadDate = DateFormat.getDateInstance().format(date);
                    magazine.setDownloadDate(downloadDate);
                    MagazineManager.removeDownloadedMagazine(this, magazine);
                    manager.addMagazine(
                            magazine,
                            DataBaseHelper.TABLE_DOWNLOADED_MAGAZINES,
                            true);
                    EventBus.getDefault().post(new LoadPlistEvent());
                    EventBus.getDefault().post(new MagazineDownloadedEvent(magazine));
                    magazine.makeCompleteFile(magazine.isSample());
                    

                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(this)
                                    .setSmallIcon(R.drawable.ic_launcher)
                                    .setContentTitle(magazine.getTitle() + (magazine.isSample() ? " sample" : "") + " downloaded")
                                    .setContentText("Click to read");

                    // Create large icon from magazine cover png
                    Resources res = getResources();
                    int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
                    int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
                    mBuilder.setLargeIcon(SystemHelper.decodeSampledBitmapFromFile(magazine.getPngPath(), height, width));

                    //TODO show magazine cover as large image

                    Intent resultIntent = new Intent(this, MuPDFActivity.class);
                    resultIntent.setAction(Intent.ACTION_VIEW);
                    resultIntent.setData(Uri.parse(magazine.isSample() ?
                            magazine.getSamplePdfPath() :
                            magazine.getItemPath()));
                    resultIntent.putExtra(DataBaseHelper.FIELD_TITLE, magazine.getTitle());

                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                    stackBuilder.addParentStack(MuPDFActivity.class);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);
                    mBuilder.setAutoCancel(true);
                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(magazine.getFileName().hashCode(), mBuilder.build());
                    
                    EventBus.getDefault().post(new ChangeInDownloadedMagazinesEvent());
                    addAssetsToDatabase(this, magazine);
                    downloadPendingAssets();
                } else if (status == DownloadManager.STATUS_FAILED) {
                	Log.d(TAG, "STATUS FAILED - REASON CODE: " + c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON)));
                }
            }
            c.close();
        } else {
        	// Just try to download any remaining failed assets
        	downloadPendingAssets();
        }
        Log.d(TAG, "DownloadMagazineService end");
    }
    
    private void addAssetsToDatabase(Context context, Magazine magazine) {
        Log.d(TAG, "addAssetsToDatabase " + magazine.getFileName());

        SQLiteDatabase db = DataBaseHelper.getInstance(context).getWritableDatabase();
        db.beginTransaction();

        ArrayList<String> assetsNames = new ArrayList<String>();
        //
        String filePath = magazine.isSample() ? magazine.getSamplePdfPath() : magazine.getItemPath();
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
                    LinkInfoExternal extLink = linkBuf.get(key)[j];
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
                        Log.d(TAG, "   link: " + Magazine.getAssetsBaseURL(magazine.getFileName()) + assetsFile);
                        Log.d(TAG, "   file: " + assetsFile);

                        String uriString = Magazine.getAssetsBaseURL(magazine.getFileName()) + assetsFile;
                        Log.d(TAG, "  link to download: " + uriString);
                        
//                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(uriString));
//                        request.setVisibleInDownloadsUi(false).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
//                                .setDescription("Subtitle for " + magazine.getSubtitle()).setTitle("Assets for " + magazine.getTitle())
//                                .setDestinationInExternalFilesDir(context, null, assetsFile);
////                        TODO should use cache directory?
//                        long downloadManagerID = mDManager.enqueue(request);
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

    private void downloadPendingAssets() {
    	ArrayList<Asset> assetsToDownload = new ArrayList<Asset>();
    	assetsToDownload = manager.getAssetsToDownload();
    	
		OkHttpClient client = new OkHttpClient();
    	
    	for (Asset asset : assetsToDownload) {
    		try {
				download(client, asset);
			} catch (NetworkErrorException e) {
				e.printStackTrace();
				assetDownloadFailed(asset);
			} catch (FileAlreadyExistException e) {
				e.printStackTrace();
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
		manager.setAssetStatus(asset.id, MagazineManager.FAILED);
		Log.w(TAG, "asset download failed");
    }
    
	private void download(OkHttpClient client, Asset asset) throws NetworkErrorException, IOException,
			FileAlreadyExistException, NoMemoryException {
		
        File file = new File(asset.assetfilename);
        File tempFile = new File(asset.assetfilename + TEMP_SUFFIX);
        long previousFileSize = 0;

//		/*
//		 * check net work
//		 */
//		// if (!NetworkUtils.isNetworkAvailable(context)) {
//		// throw new NetworkErrorException("Network blocked.");
//		// }

//		/*
//		 * check file length
//		 */
		HttpURLConnection connection = client.open(new URL(asset.assetUrl));
		 if (tempFile.exists()) {
				previousFileSize = tempFile.length();
				connection.setRequestProperty("Range", "bytes=" + tempFile.length() + "-");
				//
				// if (DEBUG) {
				// Log.v(TAG, "File is not complete, download now.");
				// Log.v(TAG, "File length:" + tempFile.length() + " totalSize:" +
				// totalSize);
				// }
			}
		 
		connection.connect();

		int totalSize = connection.getContentLength();
		Log.d("asset size", totalSize + " bytes");

		if (file.exists() && totalSize == file.length()) {
			if (BuildConfig.DEBUG) {
				Log.v(null, "Output file already exists. Skipping download.");
			}

			throw new FileAlreadyExistException(
					"Output file already exists. Skipping download.");
		}
//
//		/*
//		 * check memory
//		 */
		long storage = StorageUtils.getAvailableStorage();
		if (BuildConfig.DEBUG) {
			Log.i(null, "available storage:" + storage + " asset size:" + totalSize);
		}

		if (totalSize - tempFile.length() > storage) {
			throw new NoMemoryException("Not enough storage space.");
		}
//
//		/*
//		 * start download
//		 */
		RandomAccessFile outputStream = new RandomAccessFile(tempFile, "rw");

		InputStream input = connection.getInputStream();
		int bytesDownloaded = download(connection, input, outputStream);
//
		if ((bytesDownloaded) != totalSize && totalSize != -1)
				{
//				&& !interrupt) {
			throw new IOException("Download incomplete: previous file size:"
					+ previousFileSize + " bytes downloaded:" + bytesDownloaded
					+ " previous+downloaded:"
					+ (previousFileSize + bytesDownloaded) + " != totalsize:"
					+ totalSize);
		}

		StorageUtils.move(tempFile.getPath(), file.getPath());
		manager.setAssetStatus(asset.id, MagazineManager.DOWNLOADED);
		EventBus.getDefault().post(new ChangeInDownloadedMagazinesEvent());
		
		if (BuildConfig.DEBUG) {
			Log.v(TAG, "Download completed successfully.");
		}
		
//
//		return bytesCopied;
//
	}
	
	public int download(HttpURLConnection connection, InputStream input, RandomAccessFile out)
			throws IOException, NetworkErrorException {

		if (input == null || out == null) {
			return -1;
		}

		byte[] buffer = new byte[BUFFER_SIZE];

		BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);
//		if (BuildConfig.DEBUG) {
//			Log.v(TAG, "current file length " + out.length());
//		}

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

    public static void startMagazineDownload(Context context, Magazine currentMagazine) {
        startMagazineDownload(context, currentMagazine, false, null);
    }

    public static void startMagazineDownload(Context context, Magazine magazine, boolean isTemp, String tempUrlKey) {
        String fileUrl = magazine.getItemUrl();
        String filePath = magazine.getItemPath();
        if (magazine.isSample()) {
            fileUrl = magazine.getSamplePdfUrl();
            filePath = magazine.getSamplePdfPath();
        } else if (isTemp) {
            fileUrl = tempUrlKey;
        }
        Log.d(TAG, "isSample: " + magazine.isSample() + "\nfileUrl: " + fileUrl + "\nfilePath: " + filePath);
        EasyTracker.getInstance().setContext(context);
        EasyTracker.getTracker().sendView(
                "Downloading/" + FilenameUtils.getBaseName(filePath));
        DownloadManager dm = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setVisibleInDownloadsUi(false).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDescription(magazine.getSubtitle()).setTitle(magazine.getTitle() + (magazine.isSample() ? " Sample" : ""))
                .setDestinationInExternalFilesDir(context, null, FilenameUtils.getName(filePath));
        //TODO should use cache directory?
        magazine.setDownloadManagerId(dm.enqueue(request));

        MagazineManager magazineManager = new MagazineManager(context);
        MagazineManager.removeDownloadedMagazine(context, magazine);
        magazineManager.addMagazine(
                magazine,
                DataBaseHelper.TABLE_DOWNLOADED_MAGAZINES,
                true);
//        magazine.clearMagazineDir();
        EventBus.getDefault().post(new LoadPlistEvent());
    }

	public static void startPendingAssetsDownload(Context context) {
		WakefulIntentService.sendWakefulWork(context, DownloadMagazineService.class);
	}
}

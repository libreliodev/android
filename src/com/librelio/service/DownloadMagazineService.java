package com.librelio.service;

import android.app.DownloadManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
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
import com.google.analytics.tracking.android.EasyTracker;
import com.librelio.activity.MainMagazineActivity;
import com.librelio.activity.MuPDFActivity;
import com.librelio.event.UpdateGridViewEvent;
import com.librelio.lib.utils.PDFParser;
import com.librelio.model.Magazine;
import com.librelio.storage.DataBaseHelper;
import com.librelio.storage.MagazineManager;
import com.librelio.utils.FilenameUtils;
import com.librelio.utils.StorageUtils;
import com.librelio.utils.SystemHelper;
import com.niveales.wind.R;
import de.greenrobot.event.EventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class DownloadMagazineService extends IntentService {
    private static final String TAG = "DownloadMagazineService";
    private DownloadManager mDManager;
    private MagazineManager manager;

    public DownloadMagazineService() {
        super("magazineprocessing");
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        mDManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadManagerID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);

        Log.d("DOWNLOAD", "DONE " + downloadManagerID);
        manager = new MagazineManager(this);
        Magazine magazine = manager.findByDownloadManagerID(downloadManagerID, Magazine.TABLE_DOWNLOADED_MAGAZINES);

        if (magazine != null) {
            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(downloadManagerID);
            Cursor c = mDManager.query(q);
            if (c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    // process download
                    String srcFileName = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                    magazine.clearMagazineDir();
                    magazine.makeMagazineDir();
                    StorageUtils.move(srcFileName, magazine.isSample() ?
                            magazine.getSamplePdfPath() :
                            magazine.getPdfPath());
                }
            }
            c.close();

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
                    magazine.getPdfPath()));
            resultIntent.putExtra(Magazine.FIELD_TITLE, magazine.getTitle());

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
            mNotificationManager.notify((int) magazine.getDownloadManagerId(), mBuilder.build());


            Date date = Calendar.getInstance().getTime();
            String downloadDate = new SimpleDateFormat(" dd.MM.yyyy").format(date);
            magazine.setDownloadDate(downloadDate);
            manager.removeDownloadedMagazine(magazine);
            manager.addMagazine(
                    magazine,
                    Magazine.TABLE_DOWNLOADED_MAGAZINES,
                    true);
            EventBus.getDefault().post(new UpdateGridViewEvent());
            startLinksDownload(this, magazine);
            magazine.makeCompleteFile(magazine.isSample());
        } else {
            // Asset downloaded
            manager.setAssetDownloaded(downloadManagerID);
            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(downloadManagerID);
            Cursor c = mDManager.query(q);
            if (c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    // process download
                    String srcFileName = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                    StorageUtils.move(srcFileName, manager.getAssetFilename(downloadManagerID));
                }
            }
            c.close();
        }
    }

    private void startLinksDownload(Context context, Magazine magazine) {
        Log.d(TAG, "Start DownloadLinksTask");

        SQLiteDatabase db = DataBaseHelper.getInstance(context).getWritableDatabase();
        db.beginTransaction();

        ArrayList<String> links = new ArrayList<String>();
        ArrayList<String> assetsNames = new ArrayList<String>();
        //
        String filePath = magazine.isSample() ? magazine.getSamplePdfPath() : magazine.getPdfPath();
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
                        links.add(Magazine.getAssetsBaseURL(magazine.getFileName()) + assetsFile);
                        assetsNames.add(assetsFile);
                        Log.d(TAG, "   link: " + Magazine.getAssetsBaseURL(magazine.getFileName()) + assetsFile);
                        Log.d(TAG, "   file: " + assetsFile);

                        String uriString = Magazine.getAssetsBaseURL(magazine.getFileName()) + assetsFile;
                        Log.d(TAG, "  link to download: " + uriString);
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(uriString));
                        request.setVisibleInDownloadsUi(false).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                                .setDescription("Subtitle for " + magazine.getSubtitle()).setTitle("Assets for " + magazine.getTitle())
                                .setDestinationInExternalFilesDir(context, null, assetsFile);
                        //TODO should use cache directory?
                        long downloadManagerID = mDManager.enqueue(request);
                        ContentValues cv = new ContentValues();
                        cv.put(Magazine.FIELD_FILE_NAME, magazine.getFileName());
                        cv.put(Magazine.FIELD_ASSET_FILE_NAME, magazine.getAssetsDir() + assetsFile);
                        cv.put(Magazine.FIELD_ASSET_IS_DOWNLOADED, false);
                        cv.put(Magazine.FIELD_DOWNLOAD_MANAGER_ID, downloadManagerID);
                        db.insert(Magazine.TABLE_ASSETS, null, cv);
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

    public static void startDownload(Context context, Magazine currentMagazine) {
        startDownload(context, currentMagazine, false, null);
    }

    public static void startDownload(Context context, Magazine magazine, boolean isTemp, String tempUrlKey) {
        String fileUrl = magazine.getPdfUrl();
        String filePath = magazine.getPdfPath();
        if (magazine.isSample()) {
            fileUrl = magazine.getSamplePdfUrl();
            filePath = magazine.getSamplePdfPath();
        } else if (isTemp) {
            fileUrl = tempUrlKey;
        }
        Log.d(TAG, "isSample: " + magazine.isSample() + "\nfileUrl: " + fileUrl + "\nfilePath: " + filePath);
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
        magazineManager.removeDownloadedMagazine(magazine);
        magazineManager.addMagazine(
                magazine,
                Magazine.TABLE_DOWNLOADED_MAGAZINES,
                true);
        magazine.clearMagazineDir();
        EventBus.getDefault().post(new UpdateGridViewEvent());
    }
}

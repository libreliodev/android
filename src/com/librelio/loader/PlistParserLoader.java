package com.librelio.loader;

import android.app.DownloadManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.librelio.event.InvalidateGridViewEvent;
import com.librelio.model.Magazine;
import com.librelio.storage.DataBaseHelper;
import com.librelio.storage.MagazineManager;
import com.librelio.utils.StorageUtils;
import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Array;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;
import de.greenrobot.event.EventBus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class PlistParserLoader extends AsyncTaskLoader<ArrayList<Magazine>> {

    private static final String FILE_NAME_KEY = "FileName";
    private static final String TITLE_KEY = "Title";
    private static final String SUBTITLE_KEY = "Subtitle";
    private static final String TAG = "PlistParserLoader";
    private final String plistName;
    private final boolean hasTestMagazine;
    private final DownloadManager downloadManager;

    public PlistParserLoader(Context context, String plistName, boolean hasTestMagazine) {
        super(context);
        this.plistName = plistName;
        this.hasTestMagazine = hasTestMagazine;
        downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        setUpdateThrottle(2000);
        onContentChanged();
    }

    public static int downloadFromUrl(String sUrl, String filePath) {
        int count = -1;
        try {
            URL url = new URL(sUrl);
            URLConnection connection = url.openConnection();
            connection.connect();

//            int lenghtOfFile = connection.getContentLength();
//            Log.d(TAG, "downloadFromUrl Lenght of file: " + lenghtOfFile);

            InputStream input = new BufferedInputStream(url.openStream());
            OutputStream output = new FileOutputStream(filePath);

            byte data[] = new byte[1024];
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
        } catch (Exception e) {
            Log.e(TAG, "Problem with download: " + filePath, e);
        }
        return count;
    }

    @Override
    public ArrayList<Magazine> loadInBackground() {

        ArrayList<Magazine> magazines = extractMagazinesFromPlist(plistName);
        return magazines;
    }

    @Override
    public void deliverResult(ArrayList<Magazine> data) {
        super.deliverResult(data);
    }

    private ArrayList<Magazine> extractMagazinesFromPlist(String plistName) {
        long startTime = System.currentTimeMillis();

        ArrayList<Magazine> magazines = new ArrayList<Magazine>();

        if (hasTestMagazine) {
            magazines.add(new Magazine(MagazineManager.TEST_FILE_NAME, "TEST", "test", "", getContext()));
        }

        //Convert plist to String for parsing
        String pList = StorageUtils.getStringFromFile(StorageUtils.getStoragePath(getContext()) + plistName);

        //Parsing
        PListXMLHandler handler = new PListXMLHandler();
        PListXMLParser parser = new PListXMLParser();
        parser.setHandler(handler);
        parser.parse(pList);
        PList list = ((PListXMLHandler) parser.getHandler()).getPlist();
        Array arr = (Array) list.getRootElement();
        for (int i = 0; i < arr.size(); i++) {
            Dict dict = (Dict) arr.get(i);
            String fileName = dict.getConfiguration(FILE_NAME_KEY).getValue().toString();
            String title = dict.getConfiguration(TITLE_KEY).getValue().toString();
            String subtitle = dict.getConfiguration(SUBTITLE_KEY).getValue().toString();

            Magazine magazine = new Magazine(fileName, title, subtitle, null, getContext());
            magazines.add(magazine);
        }

        for (Magazine magazine : magazines) {
            SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getReadableDatabase();
            Cursor c = db.query(Magazine.TABLE_DOWNLOADED_MAGAZINES, new String[]{Magazine.FIELD_IS_SAMPLE,
                    Magazine.FIELD_DOWNLOAD_MANAGER_ID}, Magazine.FIELD_FILE_NAME + "=?", new String[]{magazine.getFileName()},
                    null, null, null);
            while (c.moveToNext()) {
                magazine.setSample(c.getInt(c.getColumnIndex(Magazine.FIELD_IS_SAMPLE)) == 0 ? false : true);
                magazine.setDownloadManagerId(c.getLong(c.getColumnIndex(Magazine.FIELD_DOWNLOAD_MANAGER_ID)));
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(magazine.getDownloadManagerId());
                Cursor cursor = downloadManager.query(q);
                if (cursor.moveToFirst()) {
                    magazine.setDownloadStatus(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)));
                    long fileSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    long bytesDL = cursor.getLong(cursor.getColumnIndex(DownloadManager
                            .COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    magazine.setDownloadProgress((int) ((bytesDL * 100.0f) / fileSize));
                } else {
                    magazine.setDownloadProgress(0);
                    magazine.setDownloadStatus(-1);
                }
                cursor.close();
                magazine.setTotalAssetCount(getTotalAssetCount(magazine));
                magazine.setDownloadedAssetCount(getDownloadedAssetCount(magazine));
            }
        }
        Log.d("time", (System.currentTimeMillis() - startTime) + " ");
        EventBus.getDefault().post(new InvalidateGridViewEvent());
        for (Magazine magazine : magazines) {
            //saving png
            File png = new File(magazine.getPngPath());
            if (!png.exists()) {
//                if (isOnline() && !useStaticMagazines) {
                downloadFromUrl(magazine.getPngUrl(), magazine.getPngPath());
//                }
                Log.d(TAG, "Image download: " + magazine.getPngPath());
                EventBus.getDefault().post(new InvalidateGridViewEvent());
            } else {
//                Log.d(TAG, magazine.getPngPath() + " already exist");
            }
        }
        return magazines;
    }

    public int getTotalAssetCount(Magazine magazine) {
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getReadableDatabase();
        int count = (int) DatabaseUtils.longForQuery(db, "select COUNT(" + Magazine.FIELD_ID + ") from " + Magazine
                .TABLE_ASSETS + " WHERE " + Magazine.FIELD_FILE_NAME + "=?",
                new String[]{magazine.getFileName()});
        return count;
    }

    public int getDownloadedAssetCount(Magazine magazine) {
        SQLiteDatabase db = DataBaseHelper.getInstance(getContext()).getReadableDatabase();
        int count = (int) DatabaseUtils.longForQuery(db, "select COUNT(" + Magazine.FIELD_ID + ") from " + Magazine
                .TABLE_ASSETS + " WHERE " + Magazine.FIELD_FILE_NAME + "=? AND " + Magazine.FIELD_ASSET_IS_DOWNLOADED
                + "='1'",
                new String[]{magazine.getFileName()});
        return count;
    }

    @Override
    protected void onStartLoading() {
        if (takeContentChanged())
            forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }
}

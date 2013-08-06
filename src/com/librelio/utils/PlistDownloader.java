package com.librelio.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import com.librelio.LibrelioApplication;
import com.librelio.event.UpdatedPlistEvent;
import com.librelio.event.UpdateProgressEvent;
import com.librelio.utils.StorageUtils;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.niveales.wind.R;
import de.greenrobot.event.EventBus;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class PlistDownloader {

    private static final String IF_MODIFIED_SINCE_HEADER = "If-Modified-Since";
    static String LIBRELIO_SHARED_PREFERENCES = "LIBRELIO_SHARED_PREFERENCES";
    static String LAST_UPDATE_PREFERENCES_KEY = "LAST_UPDATE_PREFERENCES_KEY";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
    private static SimpleDateFormat updateDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    public static void doLoad(final Context context, final String plistName, boolean force) {

        String plistUrl = LibrelioApplication.getAmazonServerUrl() + plistName;
        EventBus.getDefault().post(new UpdateProgressEvent(true));
        AsyncHttpClient client = new AsyncHttpClient();
        if (!force) {
            client.addHeader(IF_MODIFIED_SINCE_HEADER, getLastUpdateDate(context, plistName));
        }
        client.get(plistUrl, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, String s) {
                super.onSuccess(i, s);
                if (i == 304) {
                    //no change
                    EventBus.getDefault().post(new UpdateProgressEvent(false));
                    return;
                }
                try {
                    FileUtils.writeStringToFile(new File(StorageUtils.getStoragePath(context) + plistName), s);
                    saveUpdateDate(context, plistName);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                EventBus.getDefault().post(new UpdatedPlistEvent(plistName));
                EventBus.getDefault().post(new UpdateProgressEvent(false));
            }

            @Override
            public void onFailure(Throwable throwable, String s) {
                super.onFailure(throwable, s);
//                Toast.makeText(context, context.getResources().getString(R.string.connection_failed),
//                        Toast.LENGTH_LONG).show();
//                Toast.makeText(context, throwable.toString() + " : " + s, Toast.LENGTH_SHORT).show();
                EventBus.getDefault().post(new UpdateProgressEvent(false));
            }
        });
    }

    private static void saveUpdateDate(Context context, String plistName) {
        Date date = Calendar.getInstance().getTime();
//		Log.d(TAG, "saveUpdateDate, date : "+updateDateFormat.format(date));
        getPreferences(context).edit().putString(LAST_UPDATE_PREFERENCES_KEY + plistName,
                updateDateFormat.format(date)).commit();
    }

    private static String getLastUpdateDate(Context context, String plistName) {
        String date = getPreferences(context).getString(LAST_UPDATE_PREFERENCES_KEY + plistName, "");
//		Log.d(TAG, "getLastUpdateDate, date : "+date);
        return date;
    }

    public static SharedPreferences getPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(LIBRELIO_SHARED_PREFERENCES,
                Context.MODE_PRIVATE);
        return sharedPreferences;
    }
}

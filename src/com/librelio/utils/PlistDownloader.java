package com.librelio.utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpResponseException;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.widget.Toast;

import com.librelio.event.LoadPlistEvent;
import com.librelio.event.UpdateProgressBarEvent;
import com.librelio.model.dictitem.PlistItem;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.niveales.wind.R;

import de.greenrobot.event.EventBus;

public class PlistDownloader {

    private static final String IF_MODIFIED_SINCE_HEADER = "If-Modified-Since";
    static String LIBRELIO_SHARED_PREFERENCES = "LIBRELIO_SHARED_PREFERENCES";
    static String LAST_UPDATE_PREFERENCES_KEY = "LAST_UPDATE_PREFERENCES_KEY";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
    private static SimpleDateFormat updateDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    public static void doLoad(final Context context, final String plistName, boolean force) {

        final PlistItem plistItem = new PlistItem(context, "", plistName);
        // Don't update if updates not required - i.e. waupdate=0
        if (plistItem.getUpdateFrequency() == -1) {
            EventBus.getDefault().post(new LoadPlistEvent());
            return;
        }

        Date lastUpdateDate = getLastUpdateDate(context, plistName);
//        Only update is long enough since last update or if forced
        if (!force && System.currentTimeMillis() - lastUpdateDate.getTime() < (DateUtils.MINUTE_IN_MILLIS * plistItem
                .getUpdateFrequency())) {
//            Toast.makeText(context, "less than updateFrequency minutes old", Toast.LENGTH_SHORT).show();
            EventBus.getDefault().post(new LoadPlistEvent());
            return;
        }

        EventBus.getDefault().post(new UpdateProgressBarEvent(true));
        AsyncHttpClient client = new AsyncHttpClient();
        if (!force) {
            client.addHeader(IF_MODIFIED_SINCE_HEADER, updateDateFormat.format(lastUpdateDate));
        }
        client.get(plistItem.getItemUrl(), new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, String s) {
                super.onSuccess(i, s);
                if (i == 304) {
                    //no change - but this never happens - 304 means failure due to empty string
                    EventBus.getDefault().post(new UpdateProgressBarEvent(false));
                    return;
                }
                try {
                    FileUtils.writeStringToFile(new File(StorageUtils.getStoragePath(context) + plistItem.getItemFileName()), s);
                    saveUpdateDate(context, plistName);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                EventBus.getDefault().post(new LoadPlistEvent());
            }

            @Override
            public void onFailure(Throwable throwable, String s) {
                super.onFailure(throwable, s);
                if (throwable instanceof HttpResponseException) {
                    int statusCode = ((HttpResponseException) throwable).getStatusCode();
                    if (statusCode == 304) {
                        // not modified - no problem
                        return;
                    }
                }
                Toast.makeText(context, context.getResources().getString(R.string.connection_failed),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFinish() {
                super.onFinish();
                EventBus.getDefault().post(new UpdateProgressBarEvent(false));
            }
        });
    }

    private static void saveUpdateDate(Context context, String plistName) {
        Date date = Calendar.getInstance().getTime();
//		Log.d(TAG, "saveUpdateDate, date : "+updateDateFormat.format(date));
        getPreferences(context).edit().putLong(LAST_UPDATE_PREFERENCES_KEY + plistName,
                date.getTime()).commit();
    }

    private static Date getLastUpdateDate(Context context, String plistName) {
        Date date = new Date(getPreferences(context).getLong(LAST_UPDATE_PREFERENCES_KEY + plistName, 0));
//		Log.d(TAG, "getLastUpdateDate, date : "+date);
        return date;
    }

    public static SharedPreferences getPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(LIBRELIO_SHARED_PREFERENCES,
                Context.MODE_PRIVATE);
        return sharedPreferences;
    }
}

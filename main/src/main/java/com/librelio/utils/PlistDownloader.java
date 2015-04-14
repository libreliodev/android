package com.librelio.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.widget.Toast;

import com.librelio.event.NewPlistDownloadedEvent;
import com.librelio.event.ReloadPlistEvent;
import com.librelio.event.UpdateIndeterminateProgressBarEvent;
import com.librelio.model.dictitem.PlistItem;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.niveales.wind.R;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpResponseException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.greenrobot.event.EventBus;

public class PlistDownloader {

    private static final String IF_MODIFIED_SINCE_HEADER = "If-Modified-Since";
    static String LIBRELIO_SHARED_PREFERENCES = "LIBRELIO_SHARED_PREFERENCES";
    static String LAST_UPDATE_PREFERENCES_KEY = "LAST_UPDATE_PREFERENCES_KEY";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
    private static SimpleDateFormat updateDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    public static void updateFromServer(final Context context, final String plistName,
                                        boolean force) {

        final PlistItem plistItem = new PlistItem(context, "", plistName);

        // Don't update if updates not required - i.e. waupdate=0
        if (plistItem.getUpdateFrequency() == -1) {
            EventBus.getDefault().post(new ReloadPlistEvent(plistName));
            return;
        }

        Date lastUpdateDate = getLastUpdateDate(context, plistName);
//        Only update is long enough since last update or if forced
        if (!force && System.currentTimeMillis() - lastUpdateDate.getTime() < (DateUtils.MINUTE_IN_MILLIS * plistItem
                .getUpdateFrequency())) {
            EventBus.getDefault().post(new ReloadPlistEvent(plistName));
            return;
        }

        EventBus.getDefault().post(new UpdateIndeterminateProgressBarEvent(true));
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
                    EventBus.getDefault().post(new UpdateIndeterminateProgressBarEvent(false));
                    return;
                }
                try {
                    FileUtils.writeStringToFile(new File(StorageUtils.getStoragePath(context) + plistItem.getItemFileName()), s);
                    saveUpdateDate(context, plistName);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                EventBus.getDefault().post(new ReloadPlistEvent(plistName));
                EventBus.getDefault().post(new NewPlistDownloadedEvent(plistName));
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
                EventBus.getDefault().post(new UpdateIndeterminateProgressBarEvent(false));
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

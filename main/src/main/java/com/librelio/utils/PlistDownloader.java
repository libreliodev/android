package com.librelio.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.widget.Toast;

import com.librelio.LibrelioApplication;
import com.librelio.event.ReloadPlistEvent;
import com.librelio.event.ShowProgressBarEvent;
import com.librelio.model.dictitem.PlistItem;
import com.librelio.model.dictitem.UpdatesPlistItem;
import com.niveales.wind.R;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.commons.io.FileUtils;

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
                                        boolean force, boolean downloadUpdateFile) {

        final PlistItem plistItem;
        if (downloadUpdateFile) {
            plistItem = new UpdatesPlistItem(context, "", plistName);
        } else {
            plistItem = new PlistItem(context, "", plistName);
        }

        // Don't update if updates not required - i.e. waupdate=0
        if (!force && plistItem.getUpdateFrequency() == -1) {
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

        EventBus.getDefault().post(new ShowProgressBarEvent(plistName, true));

        Request.Builder builder = new Request.Builder().url(plistItem.getItemUrl());

        if (!force) {
            builder.addHeader(IF_MODIFIED_SINCE_HEADER, updateDateFormat.format(lastUpdateDate));
        }

        LibrelioApplication.getOkHttpClient().newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onResponse(Response response) throws IOException {
                EventBus.getDefault().post(new ShowProgressBarEvent(plistName, false));
                if (response.code() == 304) {
                    // no change - empty string so don't change
                    return;
                }
                try {
                    String string = response.body().string();
                    FileUtils.writeStringToFile(new File(StorageUtils.getStoragePath(context)
                                    + plistItem.getItemFileName()), string);
                    saveUpdateDate(context, plistName);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                EventBus.getDefault().post(new ReloadPlistEvent(plistName));
            }

            @Override
            public void onFailure(Request request, IOException e) {
                Toast.makeText(context, context.getResources().getString(R.string.connection_failed),
                        Toast.LENGTH_LONG).show();
                EventBus.getDefault().post(new ReloadPlistEvent(plistName));
                EventBus.getDefault().post(new ShowProgressBarEvent(plistName, false));
            }
        });
    }

    private static void saveUpdateDate(Context context, String plistName) {
        Date date = Calendar.getInstance().getTime();
        getPreferences(context).edit().putLong(LAST_UPDATE_PREFERENCES_KEY + plistName,
                date.getTime()).commit();
    }

    private static Date getLastUpdateDate(Context context, String plistName) {
        Date date = new Date(getPreferences(context).getLong(LAST_UPDATE_PREFERENCES_KEY + plistName, 0));
        return date;
    }

    public static SharedPreferences getPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(LIBRELIO_SHARED_PREFERENCES,
                Context.MODE_PRIVATE);
        return sharedPreferences;
    }
}

package com.librelio.utils;

import android.content.Context;
import android.os.Environment;
import com.niveales.wind.R;

public class StorageUtils {

    public static String getInternalPath(Context context) {
        return context.getDir("librelio", Context.MODE_PRIVATE).getAbsolutePath() + "/";
    }

    public static String getExternalPath(Context context) {
//        return context.getExternalFilesDir(null).getAbsolutePath();
        return Environment.getExternalStorageDirectory() + "/librelio/";
    }

    public static String getExternalCachePath(Context context) {
        return context.getExternalCacheDir().getAbsolutePath();
    }

    public static String getStoragePath(Context context) {
        if (context.getResources().getBoolean(R.bool.use_internal_storage)) {
            return getInternalPath(context);
        } else {
            return getExternalPath(context);
        }
    }
}

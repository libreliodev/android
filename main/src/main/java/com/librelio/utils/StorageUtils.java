package com.librelio.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.niveales.wind.R;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StorageUtils {

    private static final String TAG = "StorageUtils";

    public static String getInternalPath(Context context) {
        return context.getDir("librelio", Context.MODE_PRIVATE).getAbsolutePath() + "/";
    }

    public static String getExternalPath(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath();
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

    /**
     * Move files between directorires
     *
     * @param src
     *            the source target
     * @param dst
     *            the destination target
     */
    public static int move(String src, String dst){
        if (src == null || dst == null) {
            return -1;
        }
        int bytesRead = -1;
        int bytesCount = 0;
        Log.d(TAG, "move " + src + " => " + dst);
        try {
            InputStream input = new FileInputStream(src);
            OutputStream output = new FileOutputStream(dst);
            byte data[] = new byte[1024];

            while ((bytesRead = input.read(data)) != -1) {
                output.write(data, 0, bytesRead);
                bytesCount += bytesRead;
            }
            output.flush();
            output.close();
            input.close();
        } catch (IOException e) {
            Log.e(TAG, "move failed", e);
        }
        new File(src).delete();
        return bytesCount;
    }

    public static String getStringFromFile(String path){
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Problem with open file", e);
            return null;
        }
        char[] buf = new char[1024];
        int numRead = 0;
        try {
            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
            reader.close();
        } catch (IOException e) {
            Log.e(TAG,"Problem with reading file", e);
            return null;
        }
        return fileData.toString();
    }
    
    public static long getAvailableStorage() {

        String storageDirectory = null;
        storageDirectory = Environment.getDataDirectory().getPath();

        try {
            StatFs stat = new StatFs(storageDirectory);
            long avaliableSize = ((long) stat.getAvailableBlocks() * (long) stat.getBlockSize());
            return avaliableSize;
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    public static long getTotalStorage() {

        String storageDirectory = null;
        storageDirectory = Environment.getDataDirectory().getPath();

        try {
            StatFs stat = new StatFs(storageDirectory);
            long totalSize = ((long) stat.getBlockCount() * (long) stat.getBlockSize());
            return totalSize;
        } catch (RuntimeException ex) {
            return 0;
        }
    }

	public static String getFilePathFromAssetsOrLocalStorage(Context context, String filename) {
		String path = getStoragePath(context) + filename;
		// If file exists in local storage then use that
		if (new File(path).exists()) {
			return getStringFromFile(path);
		} else {
			// Otherwise use the version from the assets folder
			try {
				InputStream is = context.getResources().getAssets().open(filename);
				String string = IOUtils.toString(is);
				IOUtils.closeQuietly(is);
				return string;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static String copyFileToExternalDirectory(Context context, String pic,
			AssetManager assets) {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			File externalDir = context.getExternalCacheDir();
			if (externalDir.canWrite()) {
				try {
					String fileName = pic.split("/")[pic.split("/").length - 1];
					File newPic = new File(externalDir.getAbsolutePath() + "/" + fileName);
					byte[] buffer = new byte[1024];
					BufferedOutputStream bos = new BufferedOutputStream(
							new FileOutputStream(newPic));
					BufferedInputStream bis = new BufferedInputStream(
							assets.open(pic));
					int count = 0;
					while ((count = bis.read(buffer, 0, 1024)) > 0) {
						bos.write(buffer, 0, count);
					}
					bos.close();
					bis.close();
					return newPic.getAbsolutePath();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			} else {
				return null;
			}
		}
		return null;
	}

}

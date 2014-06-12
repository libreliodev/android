package com.librelio.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import android.content.Context;

public class AssetsUtils {

	public static String getStringFromFilename(Context context, String filename) {
		String path = StorageUtils.getStoragePath(context) + filename;
		// If file exists in local storage then use that
		if (new File(path).exists()) {
			return StorageUtils.getStringFromFile(path);
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
}

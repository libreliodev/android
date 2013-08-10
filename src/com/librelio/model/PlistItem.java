package com.librelio.model;

import android.content.Context;
import com.librelio.LibrelioApplication;
import com.librelio.utils.StorageUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlistItem extends DictItem {

    private final Context context;
    private int updateFrequency = -1;

    public PlistItem(String fullFileName, String title, Context context) {
        this.title = title;
        this.context = context;
        this.fileName = fullFileName;

        valuesInit(fullFileName);
    }

    private void valuesInit(String fullFileName) {

        String actualFileName;
        Pattern actualFileNamePattern = Pattern.compile("(?=.*\\?)[^\\?]+");
        Matcher actualFileNameMatcher = actualFileNamePattern.matcher(fullFileName);
        if (actualFileNameMatcher.find()) {
            actualFileName = actualFileNameMatcher.group();
        } else {
            actualFileName = fullFileName;
        }

        Pattern updateFrequencyPattern = Pattern.compile("waupdate=([0-9]+)");
        Matcher updateFrequencyMatcher = updateFrequencyPattern.matcher(fullFileName);
        if (updateFrequencyMatcher.find()) {
            updateFrequency = Integer.parseInt(updateFrequencyMatcher.group(1));
        }

        itemUrl = LibrelioApplication.getAmazonServerUrl() + actualFileName;
        itemPath = StorageUtils.getStoragePath(context) + actualFileName;
        pngUrl = itemUrl.replace(".plist", ".png");
        pngPath = (StorageUtils.getStoragePath(context) + actualFileName).replace(".plist", ".png");
    }

    public int getUpdateFrequency() {
        return updateFrequency;
    }
}

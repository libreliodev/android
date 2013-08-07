package com.librelio.model;

import android.content.Context;
import com.librelio.LibrelioApplication;
import com.librelio.utils.StorageUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlistItem extends DictItem {

    private final Context context;
    private int updateFrequency = 0;


    public PlistItem(String fileName, String title, Context context) {
        this.title = title;
        this.context = context;

        Pattern actualFileNamePattern = Pattern.compile("(?=.*\\?)[^\\?]+");
        Matcher actualFileNameMatcher = actualFileNamePattern.matcher(fileName);
        if (actualFileNameMatcher.find()) {
            this.fileName = actualFileNameMatcher.group();
        } else {
            this.fileName = fileName;
        }

        Pattern updateFrequencyPattern = Pattern.compile("waupdate=([0-9]+)");
        Matcher updateFrequencyMatcher = updateFrequencyPattern.matcher(fileName);
        if (updateFrequencyMatcher.find()) {
            updateFrequency = Integer.parseInt(updateFrequencyMatcher.group(1));
        }

        valuesInit(this.fileName);
    }

    private void valuesInit(String fileName) {
        itemUrl = LibrelioApplication.getAmazonServerUrl() + fileName;
        itemPath = StorageUtils.getStoragePath(context) + fileName;
        pngUrl = itemUrl.replace(".plist", ".png");
        pngPath = itemPath.replace(".plist", ".png");
    }

    public int getUpdateFrequency() {
        return updateFrequency;
    }
}

package com.librelio.model;

import android.content.Context;
import com.librelio.LibrelioApplication;
import com.librelio.utils.StorageUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RssFeedItem extends DictItem {

    private final Context context;
    private int updateFrequency = -1;

    public RssFeedItem(Context context, String rssFeedAddress, String title) {
        this.title = title;
        this.context = context;
        this.fileName = rssFeedAddress;

        valuesInit(rssFeedAddress);
    }

    private void valuesInit(String fullFileName) {

        Pattern actualFileNamePattern = Pattern.compile("waurl=([^\\?]+)&");
        Matcher actualFileNameMatcher = actualFileNamePattern.matcher(fullFileName);
        if (actualFileNameMatcher.find()) {
        	try {
				this.pdfUrl = URLDecoder.decode(actualFileNameMatcher.group(1), "utf-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        Pattern updateFrequencyPattern = Pattern.compile("waupdate=([0-9]+)");
        Matcher updateFrequencyMatcher = updateFrequencyPattern.matcher(fullFileName);
        if (updateFrequencyMatcher.find()) {
            updateFrequency = Integer.parseInt(updateFrequencyMatcher.group(1));
        }
    }

    public int getUpdateFrequency() {
        return updateFrequency;
    }
}

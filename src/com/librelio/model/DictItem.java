package com.librelio.model;

import android.content.Context;
import com.longevitysoft.android.xml.plist.domain.Dict;

public class DictItem {

    private static final String FILE_NAME_KEY = "FileName";
    private static final String TITLE_KEY = "Title";
    private static final String SUBTITLE_KEY = "Subtitle";
    protected String fileName;
    protected String title = "";
    protected String itemUrl;
    protected String itemFilename;
    protected String pngUrl;
    protected String pngPath;

    public static DictItem parse(Context context, Dict dict) {
        String fileName = dict.getString(FILE_NAME_KEY).getValue().toString();
        String title = dict.getString(TITLE_KEY).getValue().toString();

        if (fileName.contains("pdf")) {
            String subtitle = dict.getString(SUBTITLE_KEY).getValue().toString();
            Magazine magazine = new Magazine(fileName, title, subtitle, null, context);
            return magazine;
        } else if (fileName.contains("plist")) {
        	return new PlistItem(fileName, title, context);
        } else if (fileName.contains(".rss")) {
        	return new RssFeedItem(context, fileName, title);
        } else if (fileName.contains("http")) {
        	return new WebAddressItem(fileName, title);
        } else if (fileName.contains("file://")) {
        	return new DownloadsItem(title);
        }	
        
        return null;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTitle() {
        return title;
    }

    public String getItemUrl() {
        return itemUrl;
    }

    public String getFilename() {
        return itemFilename;
    }

    public String getPngUrl() {
        return pngUrl;
    }

    public String getPngPath() {
        return pngPath;
    }
}

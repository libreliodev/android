package com.librelio.model.dictitem;

import android.content.Context;

import com.librelio.utils.StorageUtils;
import com.longevitysoft.android.xml.plist.domain.Dict;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class DictItem {

    private static final String FILE_NAME_KEY = "FileName";
    private static final String TITLE_KEY = "Title";
    private static final String SUBTITLE_KEY = "Subtitle";
    static Context context;
    protected String filePath;
    protected String title = "";
    protected String subtitle = "";
    protected String itemFilename;
    protected String pngUrl;
    private int updateFrequency = -1;

    public static DictItem parse(Context context, Dict dict, String pathBit) {
        DictItem.context = context;

        String title = dict.getString(TITLE_KEY).getValue().toString();
    	
        String filePathFromPlist = dict.getString(FILE_NAME_KEY).getValue().toString();
        
        DictItem parsedItem = null;

        if (filePathFromPlist.contains("pdf")) {
            String subtitle = dict.getString(SUBTITLE_KEY).getValue().toString();
            MagazineItem magazine = new MagazineItem(context, title, subtitle, pathBit + filePathFromPlist);
            return magazine;
        } else if (filePathFromPlist.contains("plist")) {
        	parsedItem = new PlistItem(context, title, filePathFromPlist);
        } else if (filePathFromPlist.contains(".rss")) {
        	parsedItem = new RssFeedItem(title, filePathFromPlist);
        } else if (filePathFromPlist.startsWith("http") || filePathFromPlist.startsWith("file:///android_asset/")) {
        	parsedItem = new WebViewItem(title, filePathFromPlist);
        } else if (filePathFromPlist.contains("file://")) {
        	parsedItem = new DownloadsItem(title);
        } else if (filePathFromPlist.contains(".sqlite")) {
        	String subtitle = dict.getString(SUBTITLE_KEY).getValue().toString();
        	parsedItem = new ProductsItem(context, title, subtitle, filePathFromPlist);
        }
        
        String baseName = FilenameUtils.getBaseName(filePathFromPlist);
        String ext = FilenameUtils.getExtension(filePathFromPlist);
        String name = FilenameUtils.getName(filePathFromPlist);
        String prefix = FilenameUtils.getPrefix(filePathFromPlist);
        String path = FilenameUtils.getPath(filePathFromPlist);
        String pathNoEnd = FilenameUtils.getPathNoEndSeparator(filePathFromPlist);

        Pattern updateFrequencyPattern = Pattern.compile("waupdate=([0-9]+)");
        Matcher updateFrequencyMatcher = updateFrequencyPattern.matcher(filePathFromPlist);
        if (updateFrequencyMatcher.find()) {
            parsedItem.setUpdateFrequency(Integer.parseInt(updateFrequencyMatcher.group(1)));
        }
        
        parsedItem.initOtherValues();
        
        return parsedItem;
    }
    
    protected void initOtherValues() {
		
	}

	protected DictItem() {
    	
    }

    public String getFilePath() {
        return filePath;
    }

    public String getTitle() {
        return title;
    }

    public abstract String getItemUrl();

    public String getItemFileName() {
        return itemFilename;
    }

    public String getPngUrl() {
        return pngUrl;
    }

	public int getUpdateFrequency() {
		return updateFrequency;
	}

	protected void setUpdateFrequency(int updateFrequency) {
		this.updateFrequency = updateFrequency;
	}
	
	// Following method are probably only for MagazineItem and ProductsItem

	public String getSubtitle() {
		return subtitle;
	}
	
	public String getItemStorageDir(Context context) {
		return StorageUtils.getStoragePath(context)
				+ FilenameUtils.getPath(filePath);
	}

	public void makeLocalStorageDir(Context context) {
		File magazineDir = new File(getItemStorageDir(context));
		if (!magazineDir.exists()) {
			magazineDir.mkdirs();
		}
	}
}

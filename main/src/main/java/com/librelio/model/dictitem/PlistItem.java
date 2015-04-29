package com.librelio.model.dictitem;

import android.content.Context;
import android.support.v4.app.Fragment;

import com.librelio.LibrelioApplication;
import com.librelio.fragments.PlistGridFragment;
import com.librelio.model.interfaces.DisplayableAsGridItem;
import com.librelio.model.interfaces.DisplayableAsTab;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlistItem extends DictItem implements DisplayableAsTab, DisplayableAsGridItem {

    private final Context context;
    private int updateFrequency = -1;
	private String itemUrl;

    public PlistItem(Context context, String title, String filePath) {
        this.title = title;
        this.context = context;
        this.filePath = filePath;

        valuesInit();
    }

    void valuesInit() {

        String actualFileName;
        Pattern actualFileNamePattern = Pattern.compile("(?=.*\\?)[^\\?]+");
        Matcher actualFileNameMatcher = actualFileNamePattern.matcher(filePath);
        if (actualFileNameMatcher.find()) {
            actualFileName = actualFileNameMatcher.group();
        } else {
            actualFileName = filePath;
        }

        Pattern updateFrequencyPattern = Pattern.compile("waupdate=([0-9]+)");
        Matcher updateFrequencyMatcher = updateFrequencyPattern.matcher(filePath);
        if (updateFrequencyMatcher.find()) {
            updateFrequency = Integer.parseInt(updateFrequencyMatcher.group(1));
        }

        itemUrl = LibrelioApplication.getAmazonServerUrl() + actualFileName;
        itemFilename = actualFileName;
        pngUrl = itemUrl.replace(".plist", ".png");
//        pngPath = (StorageUtils.getStoragePath(context) + actualFileName).replace(".plist", ".png");
    }

    public int getUpdateFrequency() {
        return updateFrequency;
    }

	@Override
	public Fragment getFragment() {
		// TODO send full PlistItem
		return PlistGridFragment.newInstance(getFilePath());
	}

	@Override
	public String getSubtitle() {
		return "";
	}

	@Override
	public String getPngUri() {
		return null;
	}

	@Override
	public String getItemUrl() {
		return itemUrl;
	}
}

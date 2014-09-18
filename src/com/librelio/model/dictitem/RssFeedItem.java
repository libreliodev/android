package com.librelio.model.dictitem;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.support.v4.app.Fragment;

import com.librelio.fragments.RssFragment;
import com.librelio.model.interfaces.DisplayableAsTab;

public class RssFeedItem extends DictItem implements DisplayableAsTab {
	
	String rssUrl;
	private String itemUrl;

    public RssFeedItem(String title, String rssFeedAddress) {
        this.title = title;
        this.filePath = rssFeedAddress;
    }
    
    @Override
    protected void initOtherValues() {
    	super.initOtherValues();
        Pattern actualFileNamePattern = Pattern.compile("waurl=([^\\?]+)&");
        Matcher actualFileNameMatcher = actualFileNamePattern.matcher(filePath);
        if (actualFileNameMatcher.find()) {
        	try {
				this.itemUrl = URLDecoder.decode(actualFileNameMatcher.group(1), "utf-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
        }
    }

	@Override
	public Fragment getFragment() {
		return RssFragment.newInstance(getItemUrl());
	}

	@Override
	public String getItemUrl() {
		return itemUrl;
	}
}

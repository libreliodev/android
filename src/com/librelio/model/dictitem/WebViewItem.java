package com.librelio.model.dictitem;

import android.support.v4.app.Fragment;

import com.librelio.fragments.WebViewFragment;
import com.librelio.model.interfaces.DisplayableAsTab;


public class WebViewItem extends DictItem implements DisplayableAsTab {
	
	private String itemUrl;

    public WebViewItem(String title, String webAddress) {
        this.title = title;
        this.itemUrl = webAddress;
    }

	@Override
	public Fragment getFragment() {
		return WebViewFragment.newInstance(getItemUrl());
	}

	@Override
	public String getItemUrl() {
		return itemUrl;
	}

}

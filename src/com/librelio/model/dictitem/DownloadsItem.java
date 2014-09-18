package com.librelio.model.dictitem;

import android.support.v4.app.Fragment;

import com.librelio.fragments.DownloadedMagazinesFragment;
import com.librelio.model.interfaces.DisplayableAsTab;


public class DownloadsItem extends DictItem implements DisplayableAsTab {
	
	public DownloadsItem(String title) {
		this.title = title;
	}
	
	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public Fragment getFragment() {
		return new DownloadedMagazinesFragment();
	}

	@Override
	public String getItemUrl() {
		return null;
	}

}

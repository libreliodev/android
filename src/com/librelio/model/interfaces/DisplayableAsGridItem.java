package com.librelio.model.interfaces;

import android.content.Context;

public interface DisplayableAsGridItem {
	
	public String getTitle();
	
	public String getSubtitle();
	
	public String getPngUri();

	public void onThumbnailClick(Context context);
	
}

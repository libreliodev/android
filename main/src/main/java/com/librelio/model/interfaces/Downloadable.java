package com.librelio.model.interfaces;

import android.content.Context;

import com.librelio.model.DownloadStatusCode;

public interface Downloadable {
	
	DownloadStatusCode statusCode = null;
	
	public String getItemUrl();

}

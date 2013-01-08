package com.artifex.mupdf;

import android.graphics.RectF;
import android.net.Uri;

public class LinkInfo extends RectF {
	public int pageNumber = -1;  // for compatibility reasons
	public String uri;
	
	public LinkInfo(float l, float t, float r, float b, int p) {
		super(l, t, r, b);
		pageNumber = p;
	}
	
	public LinkInfo(float l, float t, float r, float b, String u) {
		super(l, t, r, b);
		uri = u;
		// for compatibility reasons
		pageNumber = -1;
	}
	
	public boolean isMediaURI() {
		return uri.startsWith("http") 
				&& (uri.contains("youtube") 
						|| uri.contains("vimeo") 
						|| uri.contains("localhost")
				);
	}

	public boolean isAutoPlay() {
		return Uri.parse(uri).getQueryParameter("waplay") != null 
				&& Uri.parse(uri).getQueryParameter("waplay").equals("auto");
	}

	public boolean isFullScreen() {
		return Uri.parse(uri).getQueryParameter("warect") != null 
				&& Uri.parse(uri).getQueryParameter("warect").equals("full");
	}

	public boolean isExternal() {
		return uri.startsWith("http://localhost/");
	}

	public boolean hasVideoData() {
		return uri.contains("mp4");
	}

	public boolean isImageFormat() {
		final String path = Uri.parse(uri).getPath();
		return path.endsWith("jpg") 
				|| path.endsWith("png") 
				|| path.endsWith("bmp");
	}

	public boolean isVideoFormat() {
		final String path = Uri.parse(uri).getPath();
		return path.endsWith("mp4");
	}

	@Override
	public String toString() {
		return "LinkInfo ["
				+ "isVideoFormat=" + isVideoFormat() 
				+ ", isImageFormat=" + isImageFormat() 
				+ ", hasVideoData=" + hasVideoData() 
				+ ", isExternal=" + isExternal() 
				+ ", isFullScreen=" + isFullScreen() 
				+ ", isAutoPlay=" + isAutoPlay() 
				+ ", uri=" + uri 
				+ "]";
	}
	
	
}

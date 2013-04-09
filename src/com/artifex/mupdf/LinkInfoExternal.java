package com.artifex.mupdf;

import android.net.Uri;
import android.util.Log;

public class LinkInfoExternal extends LinkInfo {
	final public String url;

	public LinkInfoExternal(float l, float t, float r, float b, String u) {
		super(l, t, r, b);
		url = u;
	}

	public void acceptVisitor(LinkInfoVisitor visitor) {
		visitor.visitExternal(this);
	}
	
	public boolean isMediaURI() {
		return hasVideoData()||isImageFormat();
	}

	public boolean isAutoPlay() {
		return Uri.parse(url).getQueryParameter("waplay") != null 
				&& Uri.parse(url).getQueryParameter("waplay").equals("auto");
	}

	public boolean isFullScreen() {
		Uri uri = Uri.parse(url);
		// Suppress UnsupoprtedOperationException 
		if(uri.isHierarchical())
			return uri.getQueryParameter("warect") != null 
				&& uri.getQueryParameter("warect").equals("full");
		return false;
	}

	public boolean isExternal() {
		return url.startsWith("http://localhost/");
	}

	public boolean hasVideoData() {
		final String path = Uri.parse(url).getPath();

		return path != null && path.endsWith("mp4");
	}

	public boolean isImageFormat() {
		final String path = Uri.parse(url).getPath();
		return (path != null)&&
				(path.endsWith("jpg") 
				|| path.endsWith("png") 
				|| path.endsWith("bmp"));
	}

	public boolean isVideoFormat() {
		final String path = Uri.parse(url).getPath();
		return path != null && path.endsWith("mp4");
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
				+ ", uri=" + url
				+ "]";
	}
}

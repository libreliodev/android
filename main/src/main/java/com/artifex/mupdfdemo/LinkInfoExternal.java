package com.artifex.mupdfdemo;

import android.net.Uri;

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

	public boolean isToggleFullscreenAllowed() {
		if (Uri.parse(url).getQueryParameter("watoggle") == null) {
			// by default allow toggle
			return true;
		}
		return !Uri.parse(url).getQueryParameter("watoggle").equals("no");
	}

	public boolean isFullScreen() {
		Uri uri = Uri.parse(url);
		if(uri.isHierarchical())
			return uri.getQueryParameter("warect") != null 
				&& uri.getQueryParameter("warect").equals("full");
		return false;
	}

	public boolean isLandscapeOnly() {
		Uri uri = Uri.parse(url);
		if(uri.isHierarchical())
			return uri.getQueryParameter("waorientation") != null
					&& uri.getQueryParameter("waorientation").equals("landscape");
		return false;
	}

	public boolean isPortraitOnly() {
		Uri uri = Uri.parse(url);
		if(uri.isHierarchical())
			return uri.getQueryParameter("waorientation") != null
					&& uri.getQueryParameter("waorientation").equals("portrait");
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
	
	public boolean isPdf() {
		final String path = Uri.parse(url).getPath();
		return path != null && path.endsWith("pdf");
	}
}

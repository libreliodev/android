package com.artifex.mupdf;

import android.graphics.RectF;

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
}

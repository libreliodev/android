package com.artifex.mupdfdemo.domain;

import android.graphics.RectF;

//TODO: Added multi-thread safe for singleton property
public class SearchTaskResult {
	public final String txt;
	public final int pageNumber;
	public final RectF searchBoxes[];

	private static SearchTaskResult singleton;

	static public SearchTaskResult get() {
		return singleton;
	}

	private SearchTaskResult(String txt, int pageNumber, RectF searchBoxes[]) {
		this.txt = txt;
		this.pageNumber = pageNumber;
		this.searchBoxes = searchBoxes;
	}

	public static SearchTaskResult init(String txt, int pageNumber, RectF searchBoxes[]) {
		singleton = new SearchTaskResult(txt, pageNumber, searchBoxes);
		return singleton;
	}

	public static void recycle() {
		singleton = null;
	}
}

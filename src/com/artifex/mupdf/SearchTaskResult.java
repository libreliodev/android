package com.artifex.mupdf;

import android.graphics.RectF;

class SearchTaskResult {
	public final String txt;
	public final int   pageNumber;
	public final RectF searchBoxes[];
	static private SearchTaskResult singleton;
	public static String currentMagazineFileName;

	SearchTaskResult(String _txt, int _pageNumber, RectF _searchBoxes[]) {
		txt = _txt;
		pageNumber = _pageNumber;
		searchBoxes = _searchBoxes;
	}

	static public SearchTaskResult get() {
		return singleton;
	}

	static public void set(SearchTaskResult r) {
		singleton = r;
	}
}

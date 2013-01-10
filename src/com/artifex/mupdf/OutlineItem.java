package com.artifex.mupdf;

public class OutlineItem {
	public final int level;
	public final String title;
	public final int page;

	OutlineItem(int level, String title, int page) {
		this.level = level;
		this.title = title;
		this.page = page;
	}

}

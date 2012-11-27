package com.artifex.mupdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;

public class MuPDFPageView extends PageView {
	private final MuPDFCore mCore;
	private int mPagesW; // number of pages in width
	private int mPagesH; // number of pages in height

	public MuPDFPageView(Context c, MuPDFCore core, Point parentSize) {
		super(c, parentSize);
		mCore = core;
	}
	
	public MuPDFPageView(Context c, MuPDFCore core, Point parentSize, int pagesW) {
		super(c, parentSize);
		mCore = core;
		mPagesW = pagesW;
	}

	public int hitLinkPage(float x, float y) {
		// Since link highlighting was implemented, the super class
		// PageView has had sufficient information to be able to
		// perform this method directly. Making that change would
		// make MuPDFCore.hitLinkPage superfluous.
		float scale = mSourceScale*(float)getWidth()/(float)mSize.x;
		float docRelX = (x - getLeft())/scale;
		float docRelY = (y - getTop())/scale;

		return mCore.hitLinkPage(mPageNumber, docRelX, docRelY);
	}
	
	public String hitLinkUri(float x, float y) {
		float scale = mSourceScale*(float)getWidth()/(float)mSize.x;
		float docRelX = (x - getLeft())/scale;
		float docRelY = (y - getTop())/scale;

		return mCore.hitUriPage(mPageNumber, docRelX, docRelY);
	}

	@Override
	protected void drawPage(Bitmap bm, int sizeX, int sizeY,
			int patchX, int patchY, int patchWidth, int patchHeight) {
		mCore.drawPage(mPageNumber, bm, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight);
	}

	@Override
	protected LinkInfo[] getLinkInfo() {
		return mCore.getPageLinks(mPageNumber);
	}
}

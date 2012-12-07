package com.artifex.mupdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.widget.FrameLayout;
import android.widget.Gallery;

public class MuPDFPageView extends PageView {
	private final MuPDFCore mCore;

	public MuPDFPageView(Context c, MuPDFCore core, Point parentSize) {
		super(c, parentSize);
		mCore = core;
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

		String uri = mCore.hitLinkUri(mPageNumber, docRelX, docRelY);
		if(uri == null)
			return null;
		if(uri.startsWith("http://localhost")) {
			LinkInfo[] links = mCore.getPageLinks(mPageNumber);
			LinkInfo currentLink = null;
			for(int i = 0; i < links.length; i++) {
				if(links[i].uri.equals(uri)) {
					currentLink = links[i];
					break;
				}
			}
			FrameLayout fl = new FrameLayout(getContext());
			fl.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			fl.setBackgroundColor(Color.BLACK);
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			lp.setMargins((int)(currentLink.left*scale), (int)(currentLink.top*scale),
					(int)(currentLink.right*scale), (int)(currentLink.bottom*scale));
			Gallery gallery = new Gallery(getContext());
			gallery.setAdapter(new PDFPreviewPagerAdapter(getContext(), mCore));
			gallery.setLayoutParams(lp);
			fl.addView(gallery);
			addView(fl);
			invalidate();
		}
		
		
		return uri;
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
	
	protected LinkInfo[] getExternalLinkInfo() {
		return mCore.getPageURIs(mPageNumber);
	}
}

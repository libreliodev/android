package com.artifex.mupdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;

public class MuPDFScreenView extends PageView {
	private final MuPDFCore mCore;
	private int mPagesW; // number of pages in width
	private int mPagesH; // number of pages in height
	private boolean mLandscape;

	public MuPDFScreenView(Context c, MuPDFCore core, Point parentSize) {
		super(c, parentSize);
		mCore = core;
	}
	
	public MuPDFScreenView(Context c, MuPDFCore core, Point parentSize, int pagesW) {
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
		LinkInfo [] leftPageLinkInfo;
		LinkInfo [] toReturn;
		leftPageLinkInfo = mCore.getPageLinks(mPageNumber);
		if(!isLandscape()) {
			return leftPageLinkInfo;
		} else {
			for(int i = 0; i < leftPageLinkInfo.length; i++) {
				leftPageLinkInfo[i].left /= mPagesW;
				leftPageLinkInfo[i].right /= mPagesW;
			}
			
			if(isLastPageInScreen(mPageNumber)) {
				LinkInfo [] rightPageLinkInfo = mCore.getPageLinks(mPageNumber+1);
				for(int i = 0; i < leftPageLinkInfo.length; i++) {
					rightPageLinkInfo[i].left /= mPagesW;
					rightPageLinkInfo[i].left += getWidth()/2;
					rightPageLinkInfo[i].right /= mPagesW;
					rightPageLinkInfo[i].right += getWidth()/2;
				}
				toReturn = new LinkInfo[leftPageLinkInfo.length + rightPageLinkInfo.length];
				for(int i = 0; i < leftPageLinkInfo.length; i++) {
					toReturn[i] = leftPageLinkInfo[i];
				}
				for(int i = leftPageLinkInfo.length, j = 0; i < toReturn.length; i++, j++) {
					toReturn[i] = rightPageLinkInfo[j];
				}
				return toReturn;
			} else return leftPageLinkInfo;
		}
	}

	/**
	 * @return
	 */
	private boolean isLastPageInScreen(int pPosition) {
		return pPosition + 1 >= mCore.countPages();
	}

	protected boolean isLandscape() {
		return mLandscape;
	}
	
	/**
	 * @param pPosition
	 * @param pPageSize
	 * @param pMPageWidth
	 */
	public void setPage(int pPosition, PointF pPageSize, int pMPageWidth) {
		// TODO Auto-generated method stub
		pPageSize.x = pPageSize.x/pMPageWidth;

		if(isLastPageInScreen(pPosition)){
			setPage(pPosition, pPageSize);
		} else {
			
		}
	}
}

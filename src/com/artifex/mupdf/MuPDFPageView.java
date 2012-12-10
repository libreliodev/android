package com.artifex.mupdf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.librelio.lib.LibrelioApplication;
import com.librelio.lib.ui.SlideShowActivity;
import com.librelio.lib.utils.SlideshowAdapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.Gallery;

public class MuPDFPageView extends PageView {
	private final MuPDFCore mCore;
	private HashMap<String, FrameLayout> mediaHolders = new HashMap<String, FrameLayout>();

	public MuPDFPageView(Context c, MuPDFCore core, Point parentSize) {
		super(c, parentSize);
		mCore = core;
	}

	public int hitLinkPage(float x, float y) {
		// Since link highlighting was implemented, the super class
		// PageView has had sufficient information to be able to
		// perform this method directly. Making that change would
		// make MuPDFCore.hitLinkPage superfluous.
		float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
		float docRelX = (x - getLeft()) / scale;
		float docRelY = (y - getTop()) / scale;

		return mCore.hitLinkPage(mPageNumber, docRelX, docRelY);
	}

	public String hitLinkUri(float x, float y) {
		float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
		float docRelX = (x - getLeft()) / scale;
		float docRelY = (y - getTop()) / scale;

		final String uriString = mCore
				.hitLinkUri(mPageNumber, docRelX, docRelY);
		LinkHolder holder = (LinkHolder) mediaHolders.get(uriString);
		if(holder != null) {
			holder.setVisibility(View.VISIBLE);
		}
		return uriString;
	}

	@Override
	public void setPage(int page, PointF size) {
		super.setPage(page, size);
		LinkInfo[] links = mCore.getPageLinks(page);
		if (links == null)
			return;
		for (int i = 0; i < links.length; i++) {
			if (links[i].uri != null && links[i].uri.startsWith("http")) {
				Uri uri = Uri.parse(links[i].uri);
				boolean fullScreen = uri.getQueryParameter("warect") != null && uri.getQueryParameter("warect").equals("full");
				if(!fullScreen) {
					LinkHolder h = new LinkHolder(getContext(), links[i]);
					this.mediaHolders.put(links[i].uri, h);
					addView(h);
				}
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		for (Map.Entry<String, FrameLayout> entry : mediaHolders.entrySet()) {
			LinkHolder mLinkHolder = (LinkHolder) entry.getValue();
			LinkInfo currentLink = mLinkHolder.getLinkInfo();
			float scale = mSourceScale * (float) getWidth() / (float) mSize.x;

			int width = (int) ((currentLink.right - currentLink.left) * scale);
			int height = (int) ((currentLink.bottom - currentLink.top) * scale);
//			mLinkHolder.measure(widthMeasureSpec, heightMeasureSpec);
			mLinkHolder.measure(View.MeasureSpec.EXACTLY | width,
					View.MeasureSpec.EXACTLY | height);
			
		}

	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		float scale = mSourceScale * (float) getWidth() / (float) mSize.x;

		for (Map.Entry<String, FrameLayout> entry : mediaHolders.entrySet()) {
			LinkHolder mLinkHolder = (LinkHolder) entry.getValue();
			LinkInfo currentLink = mLinkHolder.getLinkInfo();
			mLinkHolder.layout((int) (currentLink.left * scale),
					(int) (currentLink.top * scale),
					(int) (currentLink.right * scale),
					(int) (currentLink.bottom * scale));
		}
	}

	@Override
	protected void drawPage(Bitmap bm, int sizeX, int sizeY, int patchX,
			int patchY, int patchWidth, int patchHeight) {
		mCore.drawPage(mPageNumber, bm, sizeX, sizeY, patchX, patchY,
				patchWidth, patchHeight);
	}

	@Override
	protected LinkInfo[] getLinkInfo() {
		return mCore.getPageLinks(mPageNumber);
	}

	protected LinkInfo[] getExternalLinkInfo() {
		return mCore.getPageURIs(mPageNumber);
	}
}

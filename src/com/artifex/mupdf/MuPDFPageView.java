package com.artifex.mupdf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.librelio.lib.LibrelioApplication;
import com.librelio.lib.ui.SlideShowActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.net.Uri;
import android.view.View;
import android.view.ViewParent;
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

		if (uriString == null)
			return null;

		LinkInfo[] links = mCore.getPageLinks(getPage());
		if (links == null)
			return null;
		LinkInfo mLink = null;
		for (int i = 0; i < links.length; i++) {
			if (links[i].uri != null && links[i].uri.equals(uriString)) {
				mLink = links[i];
				break;
			}
		}
		if (uriString.startsWith("http") && (uriString.contains("youtube") || uriString.contains("vimeo") || uriString.contains("localhost"))) {
			final Uri uri = Uri.parse(uriString);
			boolean fullScreen = uri.getQueryParameter("warect") != null
					&& uri.getQueryParameter("warect").equals("full");
			try {
				if (!fullScreen) {
					final String basePath = mCore.getFileDirectory();
					MediaHolder h = new MediaHolder(getContext(), mLink,
							basePath);
					h.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View pV) {
							MediaHolder mh = (MediaHolder) pV;
							removeView(mh);
							mh.clearResources();
							Intent intent = new Intent(getContext(),
									SlideShowActivity.class);
							intent.putExtra("path", basePath + uri.getPath());
							getContext().startActivity(intent);
						}
					});
					this.mediaHolders.put(uriString, h);
					addView(h);
					h.setVisibility(View.VISIBLE);
				}
			} catch (IllegalStateException e) {
				e.printStackTrace();
				return null;
			}
		}
		return uriString;
	}

	@Override
	public void setPage(int page, PointF size) {
		super.setPage(page, size);

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		for (Map.Entry<String, FrameLayout> entry : mediaHolders.entrySet()) {
			MediaHolder mLinkHolder = (MediaHolder) entry.getValue();
			LinkInfo currentLink = mLinkHolder.getLinkInfo();
			float scale = mSourceScale * (float) getWidth() / (float) mSize.x;

			int width = (int) ((currentLink.right - currentLink.left) * scale);
			int height = (int) ((currentLink.bottom - currentLink.top) * scale);
			// mLinkHolder.measure(widthMeasureSpec, heightMeasureSpec);
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
			MediaHolder mLinkHolder = (MediaHolder) entry.getValue();
			LinkInfo currentLink = mLinkHolder.getLinkInfo();
			mLinkHolder.layout((int) (currentLink.left * scale),
					(int) (currentLink.top * scale),
					(int) (currentLink.right * scale),
					(int) (currentLink.bottom * scale));
		}
	}

	@Override
	public void blank(int page) {
		super.blank(page);
		Iterator<Entry<String, FrameLayout>> i = mediaHolders.entrySet()
				.iterator();
		while (i.hasNext()) {
			Entry<String, FrameLayout> entry = i.next();
			MediaHolder mLinkHolder = (MediaHolder) entry.getValue();
			i.remove();
			removeView(mLinkHolder);
			mLinkHolder = null;
		}
	}

	@Override
	public void removeHq() {
		super.removeHq();
		Iterator<Entry<String, FrameLayout>> i = mediaHolders.entrySet()
				.iterator();
		while (i.hasNext()) {
			Entry<String, FrameLayout> entry = i.next();
			MediaHolder mLinkHolder = (MediaHolder) entry.getValue();
			i.remove();
			removeView(mLinkHolder);
			mLinkHolder = null;
		}
	}

	@Override
	public void addHq() {
		super.addHq();
		for (Map.Entry<String, FrameLayout> entry : mediaHolders.entrySet()) {
			MediaHolder mLinkHolder = (MediaHolder) entry.getValue();

			mLinkHolder.bringToFront();
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

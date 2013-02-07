package com.artifex.mupdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

public class MuPDFPageView extends PageView {
	private static final String TAG = "MuPDFPageView";
	public static final String PATH_KEY = "path";
	public static final String LINK_URI_KEY = "link_uri";
	
	private final MuPDFCore muPdfCore;
	private HashMap<String, FrameLayout> mediaHolders = new HashMap<String, FrameLayout>();
	
	private ArrayList<String> runningLinks;

	public MuPDFPageView(Context c, MuPDFCore muPdfCore, Point parentSize) {
		super(c, parentSize);
		this.muPdfCore = muPdfCore;
		runningLinks = new ArrayList<String>();
	}

	public int hitLinkPage(float x, float y) {
		// Since link highlighting was implemented, the super class
		// PageView has had sufficient information to be able to
		// perform this method directly. Making that change would
		// make MuPDFCore.hitLinkPage superfluous.
		float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
		float docRelX = (x - getLeft()) / scale;
		float docRelY = (y - getTop()) / scale;

		return muPdfCore.hitLinkPage(mPageNumber, docRelX, docRelY);
	}

	public String hitLinkUri(float x, float y) {
		float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
		float docRelX = (x - getLeft()) / scale;
		float docRelY = (y - getTop()) / scale;

		String uriString = null;
		if(mLinks == null) 
			return null;
		for (LinkInfo l: mLinks)
			if (l.rect.contains(docRelX, docRelY) && (l instanceof LinkInfoExternal))
				uriString  = ((LinkInfoExternal) l).url;;
		
		if (uriString == null)
			return null;
		
		LinkInfo[] links = muPdfCore.getPageLinks(getPage());
		if (links == null) {
			return null;
		}
		LinkInfoExternal linkInfo = null;
		for (int i = 0; i < links.length; i++) {
			if( ! (links[i] instanceof LinkInfoExternal))
				continue;
			LinkInfoExternal extLinkInfo = (LinkInfoExternal) links[i];
			if (extLinkInfo.url != null && extLinkInfo.url.equals(uriString)) {
				linkInfo = extLinkInfo;
				break;
			}
		}
		
		if(runningLinks.contains(linkInfo.url)){
			Log.d(TAG,"Already running link: "+linkInfo.url);
			return linkInfo.url;
		} else if(!linkInfo.isFullScreen()){
			runningLinks.add(linkInfo.url);
		}
		
		if (linkInfo.isMediaURI()) {
			try {
				final String basePath = muPdfCore.getFileDirectory();
				MediaHolder h = new MediaHolder(getContext(), linkInfo, basePath);
				h.setVisibility(View.VISIBLE);
				this.mediaHolders.put(uriString, h);
				addView(h);
			} catch (IllegalStateException e) {
				Log.e(TAG, "hitLinkUri failed", e);
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
			LinkInfoExternal currentLink = mLinkHolder.getLinkInfo();
			float scale = mSourceScale * (float) getWidth() / (float) mSize.x;

			int width = (int) ((currentLink.rect.right - currentLink.rect.left) * scale);
			int height = (int) ((currentLink.rect.bottom - currentLink.rect.top) * scale);
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
			mLinkHolder.layout((int) (currentLink.rect.left * scale),
					(int) (currentLink.rect.top * scale),
					(int) (currentLink.rect.right * scale),
					(int) (currentLink.rect.bottom * scale));
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
			mLinkHolder.recycle();
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

	public void addMediaHolder(MediaHolder h,String uriString){
		this.mediaHolders.put(uriString, h);
	}
	
	public void cleanRunningLinkList(){
		runningLinks.clear();
	}
	@Override
	protected void drawPage(Bitmap bm, int sizeX, int sizeY, int patchX,
			int patchY, int patchWidth, int patchHeight) {
		if (null != bm) {
			muPdfCore.drawPage(mPageNumber, bm, sizeX, sizeY, patchX, patchY,
					patchWidth, patchHeight);
		} else {
			Log.w(TAG, "IGNORED drawPage");
		}

	}

	@Override
	protected LinkInfo[] getLinkInfo() {
		return muPdfCore.getPageLinks(mPageNumber);
	}

//	protected LinkInfo[] getExternalLinkInfo() {
//		return muPdfCore.getPageURIs(mPageNumber);
//	}	
}

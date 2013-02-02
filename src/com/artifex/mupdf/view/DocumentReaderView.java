package com.artifex.mupdf.view;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.artifex.mupdf.LinkInfo;
import com.artifex.mupdf.LinkInfoExternal;
import com.artifex.mupdf.MuPDFPageView;
import com.artifex.mupdf.PageView;
import com.artifex.mupdf.domain.SearchTaskResult;
import com.librelio.activity.SlideShowActivity;

public abstract class DocumentReaderView extends ReaderView {
	private static final String TAG = "DocumentReaderView";

	private enum LinkState {
		DEFAULT, HIGHLIGHT, INHIBIT
	};

	private static final int TAP_PAGE_MARGIN = 70;

	private LinkState linkState = LinkState.DEFAULT;

	private boolean showButtonsDisabled;

	public DocumentReaderView(Context context, 
			SparseArray<LinkInfoExternal[]> pLinkOfDocument) {
		super(context, pLinkOfDocument);
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (e.getX() < TAP_PAGE_MARGIN) {
			super.moveToPrevious();
		} else if (e.getX() > super.getWidth() - TAP_PAGE_MARGIN) {
			super.moveToNext();
		} else if (!showButtonsDisabled) {
			int linkPage = -1;
			String linkString = null;
			if (linkState != LinkState.INHIBIT) {
				MuPDFPageView pageView = (MuPDFPageView) getDisplayedView();
				if (pageView != null) {
					linkPage = pageView.hitLinkPage(e.getX(), e.getY());
					linkString = pageView.hitLinkUri(e.getX(),  e.getY());
				}
			}

			if (linkPage != -1) {
				setDisplayedViewIndex(linkPage);
			} else if (linkString != null) {
				// start intent with url as linkString
				openLink(linkString);
			} else {
				onContextMenuClick();
			}
		}
		return super.onSingleTapUp(e);
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector d) {
		// Disabled showing the buttons until next touch.
		// Not sure why this is needed, but without it
		// pinch zoom can make the buttons appear
		showButtonsDisabled = true;
		return super.onScaleBegin(d);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
			showButtonsDisabled = false;
		}
		return super.onTouchEvent(event);
	}

	public boolean isShowButtonsDisabled() {
		return showButtonsDisabled;
	}

	abstract protected void onContextMenuClick();
	abstract protected void onBuy(String path);

	//	protected void onChildSetup(int i, View v) {
//		if (SearchTaskResult.get() != null && SearchTaskResult.get().pageNumber == i)
//			((PageView)v).setSearchBoxes(SearchTaskResult.get().searchBoxes);
//		else
//			((PageView)v).setSearchBoxes(null);
//
//		((PageView)v).setLinkHighlighting(mLinkState == LinkState.HIGHLIGHT);
//	}

	@Override
	protected void onMoveToChild(View view, int i) {

//		mPageNumberView.setText(String.format("%d/%d", i+1, core.countPages()));
//		mPageSlider.setMax((core.countPages()-1) * mPageSliderRes);
//		mPageSlider.setProgress(i * mPageSliderRes);
		if (SearchTaskResult.get() != null && SearchTaskResult.get().pageNumber != i) {
			SearchTaskResult.recycle();
			resetupChildren();
		}
	}

	@Override
	protected void onSettle(View v) {
		((PageView)v).addHq();
	}

	@Override
	protected void onUnsettle(View v) {
		((PageView)v).removeHq();
	}

	@Override
	protected void onNotInUse(View v) {
		((PageView)v).releaseResources();
	}

	/**
	 * @param linkString - url to open
	 */
	private void openLink(String linkString) {
		Log.d(TAG, "!openLink " + linkString);
		Uri uri = Uri.parse(linkString);
		String warect = uri.getQueryParameter("warect");
		Boolean isFullScreen = warect != null && warect.equals("full");
		if(linkString.startsWith("http://localhost/")) {
			// display local content
			
			// get the current page view
			String path = uri.getPath();
			Log.d(TAG, "localhost path = " + path);
			if(path == null)
				return;
			
			if(path.endsWith("jpg") || path.endsWith("png") || path.endsWith("bmp")) {
				// start image slideshow
				Intent intent = new Intent(getContext(), SlideShowActivity.class);
				intent.putExtra("path", path);
				intent.putExtra("uri", linkString);
				Log.d(TAG,"basePath = "+path+"\nuri = "+ linkString);
				//startActivity(intent);
			}
			if(path.endsWith("mp4") && isFullScreen) {
				// start a video player
				//Uri videoUri = Uri.parse("file://" + getStoragePath() + "/wind_355" + path);
				//Intent intent = new Intent(Intent.ACTION_VIEW, videoUri);
				//startActivity(intent);
			}
		} else if(linkString.startsWith("buy://localhost")) {
			onBuy(uri.getPath().substring(1));
		} else {
			//TODO: replace with custom activity
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			getContext().startActivity(intent);
		}
		
	}
}

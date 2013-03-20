package com.artifex.mupdf;

import android.graphics.Bitmap;

public class BitmapHolder {
	private Bitmap bm;

	public BitmapHolder() {
		bm = null;
	}

	public synchronized void setBm(Bitmap abm) {
		if (bm != null && bm != abm && !bm.isRecycled()) {
			bm.recycle();
			bm = null;
		}
		bm = abm;
	}

	public synchronized void drop() {
		bm = null;
	}

	public synchronized Bitmap getBm() {
		return bm;
	}
}

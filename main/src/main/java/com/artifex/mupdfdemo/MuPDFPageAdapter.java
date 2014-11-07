package com.artifex.mupdfdemo;

import com.librelio.task.SafeAsyncTask;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class MuPDFPageAdapter extends BaseAdapter {
	private static final String TAG = "MuPDFPageAdapter";

	private final Context context;
	private final MuPDFCore core;
	private final SparseArray<PointF> mPageSizes = new SparseArray<PointF>();

	public MuPDFPageAdapter(Context context, MuPDFCore core) {
		this.context = context;
		this.core = core;
	}

	public int getCount() {
		return core.countPages();
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(final int position, View convertView, ViewGroup parent) {
		Log.d(TAG,"getView");
		final MuPDFPageView pageView;
		if (convertView == null) {
			pageView = new MuPDFPageView(context, core, new Point(parent.getWidth(), parent.getHeight()));
		} else {
			pageView = (MuPDFPageView)convertView;
		}

		PointF pageSize = mPageSizes.get(position);
		if (pageSize != null) {
			// We already know the page size. Set it up
			// immediately
			pageView.setPage(position, pageSize);
		} else {
			// Page size as yet unknown. Blank it for now, and
			// start a background task to find the size
			pageView.blank(position);
			SafeAsyncTask<Void,Void,PointF> sizingTask = new SafeAsyncTask<Void,Void,PointF>() {
				@Override
				protected PointF doInBackground(Void... arg0) {
					return core.getPageSize(position);
				}

				@Override
				protected void onPostExecute(PointF result) {
					if (isCancelled()) {
						return;
					}
					// We now know the page size
					mPageSizes.put(position, result);
					// Check that this view hasn't been reused for
					// another page since we started
					if (pageView.getPage() == position)
						pageView.setPage(position, result);
				}
			};

			sizingTask.safeExecute();
		}
		return pageView;
	}
}

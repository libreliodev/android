/**
 * 
 */
package com.artifex.mupdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.librelio.task.SafeAsyncTask;
import com.niveales.wind.R;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PointF;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

/**
 * @author Dmitry Valetin
 * 
 */
public class PDFPreviewPagerAdapter extends BaseAdapter {

	private static final String TAG = PDFPreviewPagerAdapter.class
			.getSimpleName();
	private Context mContext;
	private MuPDFCore mCore;

	private Point mPreviewSize;
	private final SparseArray<Bitmap> mBitmapCache = new SparseArray<Bitmap>();
	private String mPath;

	public PDFPreviewPagerAdapter(Context context, MuPDFCore core) {
		mContext = context;
		mCore = core;
		mPath = core.getFileDirectory() + "/previewcache/";
		File mCacheDirectory = new File(mPath);
		if (!mCacheDirectory.exists())
			mCacheDirectory.mkdirs();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.view.PagerAdapter#getCount()
	 */
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		int count = mCore.countSinglePages();
		return count;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int pPosition) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int pPosition) {
		// TODO Auto-generated method stub
		if(mCore.getDisplayPages() == 1) 
			return pPosition;
		else
			if(pPosition > 0)
				return (pPosition + 1) / 2;
			else 
				return 0;
	}

	public View getView(final int position, View convertView, ViewGroup parent) {
		final View pageView;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			pageView = inflater.inflate(R.layout.preview_pager_item_layout,
					parent, false);
		} else {
			pageView = (View) convertView;
		}
		final ImageView mPreviewPageImageView = (ImageView) pageView
				.findViewById(R.id.PreviewPageImageView);
		mPreviewPageImageView.setImageResource(R.drawable.darkdenim3);
		TextView mPageNumber = (TextView) pageView
				.findViewById(R.id.PreviewPageNumber);
		mPageNumber.setText(String.valueOf(position + 1));
		drawPageImageView(mPreviewPageImageView, position);
		return pageView;
	}

	private void drawPageImageView(final ImageView v, final int position) {
		SafeAsyncTask<Void, Void, Bitmap> drawTask = new SafeAsyncTask<Void, Void, Bitmap>() {

			@Override
			protected Bitmap doInBackground(Void... pParams) {
				if (mPreviewSize == null) {
					mPreviewSize = new Point();
					int padding = mContext.getResources()
							.getDimensionPixelSize(R.dimen.page_preview_size);
					PointF mPageSize = mCore.getSinglePageSize(position);
					float scale = mPageSize.y / mPageSize.x;
					mPreviewSize.x = (int) ((float) padding / scale);
					mPreviewSize.y = padding;
				}
				Bitmap lq = null;
				lq = getCachedBitmap(position);
				mBitmapCache.put(position, lq);
				return lq;
			}

			@Override
			protected void onPostExecute(Bitmap result) {

				v.setImageBitmap(result);
				v.setLayoutParams(new LinearLayout.LayoutParams(mPreviewSize.x,
						mPreviewSize.y));
//				if(v.getPaddingLeft() != 10 || v.getPaddingRight() != 10)
//					v.setPadding(10, 0, 10, 0);
//				v.requestLayout();
			}

		};
		Bitmap bmp = mBitmapCache.get(position);
		if (bmp == null)
			drawTask.safeExecute((Void) null);
		else
			v.setImageBitmap(bmp);
	}

	private Bitmap getCachedBitmap(int position) {
		String mCachedBitmapFilePath = mPath  + position
				+ ".jpg";
		File mCachedBitmapFile = new File(mCachedBitmapFilePath);
		Bitmap lq = null;
		try {
			if (mCachedBitmapFile.exists() && mCachedBitmapFile.canRead()) {
				Log.d(TAG, "page " + position + " found in cache");
				lq = BitmapFactory.decodeFile(mCachedBitmapFilePath);
				return lq;
			}
		} catch (Exception e) {
			e.printStackTrace();
			// some error with cached file,
			// delete the file and get rid of bitmap
			mCachedBitmapFile.delete();
			lq = null;
		}
		if (lq == null) {
			lq = Bitmap.createBitmap(mPreviewSize.x, mPreviewSize.y,
					Bitmap.Config.ARGB_8888);
			mCore.drawSinglePage(position, lq, mPreviewSize.x, mPreviewSize.y);
			try {
				lq.compress(CompressFormat.JPEG, 50, new FileOutputStream(
						mCachedBitmapFile));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				mCachedBitmapFile.delete();
			}
		}
		return lq;
	}
}

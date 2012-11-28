/**
 * 
 */
package com.artifex.mupdf;

import com.librelio.wind.R;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.support.v4.view.PagerAdapter;
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

	private Context mContext;
	private MuPDFCore mCore;

	private Point mPreviewSize;
	private final SparseArray<Bitmap> mBitmapCache = new SparseArray<Bitmap>();
	private boolean isBitmapsLoaded = false;
	private boolean isBitmapsLoading = false;

	public PDFPreviewPagerAdapter(Context context, MuPDFCore core){
		mContext = context;
		mCore = core;
		SafeAsyncTask<Void, Void, Void> loadBitmaps = new SafeAsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				isBitmapsLoading = true;
			}
			@Override
			protected Void doInBackground(Void... pParams) {
				if(mPreviewSize == null) {
					mPreviewSize = new Point();
					int padding = mContext.getResources().getDimensionPixelSize(R.dimen.page_preview_size);
					PointF mPageSize = mCore.getPageSize(1);
					float scale = mPageSize.y/ mPageSize.x;
					mPreviewSize.x = (int) ((float)padding/scale);
					mPreviewSize.y = padding;
				}
				for(int i = 0; i < mCore.countPages(); i++) {
					if(mBitmapCache.get(i) == null){ 
						Bitmap lq = Bitmap.createBitmap(mPreviewSize.x, mPreviewSize.y, Bitmap.Config.ARGB_8888);
						mBitmapCache.put(i, lq);
						mCore.drawPage(i, lq, mPreviewSize.x, mPreviewSize.y, 0, 0, mPreviewSize.x, mPreviewSize.y);
						
					}
				}
				return null;	
			}
			
			@Override
			protected void onPostExecute(Void result) {
				isBitmapsLoading = false;
				isBitmapsLoaded = true;
			}
		};
//		loadBitmaps.safeExecute((Void) null);
	}
	/* (non-Javadoc)
	 * @see android.support.v4.view.PagerAdapter#getCount()
	 */
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		int count = mCore.countPages();
		return count;
	}

	
	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int pPosition) {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int pPosition) {
		// TODO Auto-generated method stub
		return pPosition;
	}
	/* (non-Javadoc)
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
//	@Override
//	public View getView(final int position, View pConvertView, ViewGroup pParent) {
//
//		final MuPDFPageView pageView = new MuPDFPageView(mContext, mCore, mPreviewSize);
//		SafeAsyncTask<Void,Void,PointF> sizingTask = new SafeAsyncTask<Void,Void,PointF>() {
//			@Override
//			protected PointF doInBackground(Void... arg0) {
//				return mCore.getPageSize(position);
//			}
//
//			@Override
//			protected void onPostExecute(PointF result) {
//				super.onPostExecute(result);
//				// We now know the page size
//				mPageSizes.put(position, result);
//				// Check that this view hasn't been reused for
//				// another page since we started
//				if (pageView.getPage() == position)
//					pageView.setPage(position, result);
//			}
//		};
//
//		sizingTask.safeExecute((Void)null);
//		return pageView;
//	}
	public View getView(final int position, View convertView, ViewGroup parent) {
		final View pageView;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			pageView = inflater.inflate(R.layout.preview_pager_item_layout, parent, false);
//			pageView = new MuPDFPageView(mContext, mCore, this.mPreviewSize);
//			LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(mPreviewSize.x, mPreviewSize.y);
//			p.setMargins(0, 0, 0, 0);
//			pageView.setLayoutParams(p);
		} else {
			pageView = (View) convertView;
		}
		final ImageView mPreviewPageImageView = (ImageView) pageView.findViewById(R.id.PreviewPageImageView);
		mPreviewPageImageView.setImageResource(R.drawable.darkdenim3);
		TextView mPageNumber = (TextView) pageView.findViewById(R.id.PreviewPageNumber);
		mPageNumber.setText(String.valueOf(position+1));
		drawPageImageView(mPreviewPageImageView, position);
		return pageView;
	}
	
	private void drawPageImageView(final ImageView v, final int position) {
		SafeAsyncTask<Void, Void, Bitmap> drawTask = new SafeAsyncTask<Void, Void, Bitmap>() {

			@Override
			protected Bitmap doInBackground(Void... pParams) {
				if(mPreviewSize == null) {
					mPreviewSize = new Point();
					int padding = mContext.getResources().getDimensionPixelSize(R.dimen.page_preview_size);
					PointF mPageSize = mCore.getPageSize(position);
					float scale = mPageSize.y/ mPageSize.x;
					mPreviewSize.x = (int) ((float)padding/scale);
					mPreviewSize.y = padding;
				}
				
				Bitmap lq = Bitmap.createBitmap(mPreviewSize.x, mPreviewSize.y, Bitmap.Config.ARGB_8888);
				mCore.drawPage(position, lq, mPreviewSize.x, mPreviewSize.y, 0, 0, mPreviewSize.x, mPreviewSize.y);
				mBitmapCache.put(position, lq);
				return lq;
			}
			
			@Override 
			protected void onPostExecute(Bitmap result) {
				
				v.setImageBitmap(result);
//				v.setScaleType(ImageView.ScaleType.FIT_XY);
		        v.setLayoutParams(new LinearLayout.LayoutParams(mPreviewSize.x, mPreviewSize.y));
		        v.setPadding(10, 0, 10, 0);
		        v.requestLayout();
			}
			
		};
		Bitmap bmp = mBitmapCache.get(position);
		if(bmp == null)
			drawTask.safeExecute((Void) null);
		else 
			v.setImageBitmap(bmp);
	}
}

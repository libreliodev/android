/**
 * 
 */
package com.librelio.lib.adapter;

import java.io.File;

import com.artifex.mupdf.SafeAsyncTask;
import com.niveales.wind.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.SpinnerAdapter;

/**
 * @author Dmitry Valetin
 * 
 */
public class SlideshowAdapter extends BaseAdapter {

	private String mSlideshowAssetDir;
	private int mSlideshowCount;
	private String mSlideshowPreffix;
	private String mSlideshowSuffix;
	private LayoutInflater inflater;
	private OnClickListener listener;

	public SlideshowAdapter(Context pContext, String path) {
		inflater = (LayoutInflater) pContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		File mFile = new File(path);
		mSlideshowAssetDir = mFile.getParent();
		String mFileName = mFile.getName();
		mSlideshowCount = Integer
				.valueOf(mFileName.split("_")[1].split("\\.")[0]);
		mSlideshowPreffix = mFileName.split("_")[0];
		mSlideshowSuffix = mFileName.split("_")[1].split("\\.")[1];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mSlideshowCount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public String getItem(int pPosition) {
		// TODO Auto-generated method stub
		return mSlideshowAssetDir + "/" + mSlideshowPreffix + "_"
				+ String.valueOf(pPosition + 1) + "." + mSlideshowSuffix;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int pPosition) {
		// TODO Auto-generated method stub
		return pPosition;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getView(int, android.view.View,
	 * android.view.ViewGroup)
	 */
	@SuppressLint("NewApi")
	@Override
	public View getView(int pPosition, View pConvertView, ViewGroup pParent) {
		// TODO Auto-generated method stub
		View v = pConvertView;
		String path = getItem(pPosition);
		ImageView iv = null;
		Bitmap bmp = null;
		if (v == null) {
			v = inflater
					.inflate(R.layout.slideshow_item_layout, pParent, false);
			if (android.os.Build.VERSION.SDK_INT >= 9)
				v.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
			iv = (ImageView) v.findViewById(R.id.SlideshowImage);
			iv.setTag(path);			
		} else {
			iv = (ImageView) v.findViewById(R.id.SlideshowImage);
			bmp = ((BitmapDrawable) iv.getDrawable()).getBitmap();
			iv.setImageDrawable(null);
			if (bmp != null)
				bmp.recycle();
			
		}
		AsyncTask<Object, Void, Bitmap> mBitmapAsyncTask = new AsyncTask<Object, Void, Bitmap>() {
			ImageView iv;
			
			@Override
			protected Bitmap doInBackground(Object... pParams) {
				iv = (ImageView) pParams[0];
				String path = (String) pParams[1];
				return BitmapFactory.decodeFile(path);
			}
			
			@Override
			protected void onPostExecute(Bitmap bmp) {
				iv.setImageBitmap(bmp);
			}
		};
		mBitmapAsyncTask.execute(iv, path);
		iv.setScaleType(ScaleType.CENTER_INSIDE);
		iv.setScaleType(ScaleType.CENTER_INSIDE);
		iv.setTag(path);
		iv.setScaleType(ScaleType.CENTER_INSIDE);
		// iv.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View pV) {
		// if(listener != null)
		// listener.onClick(pV);
		// }});
		return v;
	}

	public void setOnClickListener(OnClickListener l) {
		listener = l;
	}
	

}

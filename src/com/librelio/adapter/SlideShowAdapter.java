package com.librelio.adapter;

/**
 *
 */

import java.io.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.niveales.wind.R;

/**
 * @author Dmitry Valetin
 * 
 */
public class SlideShowAdapter extends BaseAdapter {
	private static final String TAG = "SlideShowAdapter";

	private String mSlideshowAssetDir;
	private int mSlideshowCount;
	private String mSlideshowPreffix;
	private String mSlideshowSuffix;
	private LayoutInflater inflater;
	private OnClickListener listener;

	public SlideShowAdapter(Context context, String path) {
		Log.d(TAG, "" + path);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		File file = new File(path);
		mSlideshowAssetDir = file.getParent();
		String fileName = file.getName();
		mSlideshowCount = Integer.valueOf(fileName.split("_")[1].split("\\.")[0]);
//		mSlideshowCount = 1;
		mSlideshowPreffix = fileName.split("_")[0];
		mSlideshowSuffix = fileName.split("_")[1].split("\\.")[1];
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mSlideshowCount;
	}

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
			v = inflater.inflate(R.layout.slideshow_item_layout, pParent, false);
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

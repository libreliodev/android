/**
 * 
 */
package com.librelio.lib.adapter;

import java.io.File;

import com.librelio.lib.LibrelioApplication;
import com.niveales.wind.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

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

	public SlideshowAdapter(Context pContext, String path) {
		inflater = (LayoutInflater) pContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		File mFile = new File(path);
		mSlideshowAssetDir = mFile.getParent();
		String mFileName = mFile.getName();
		mSlideshowCount = Integer.valueOf(mFileName.split("_")[1].split("\\.")[0]);
		mSlideshowPreffix = mFileName.split("_")[0];
		mSlideshowSuffix = mFileName.split("_")[1].split("\\.")[1];
	}
	
	
	/* (non-Javadoc)
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mSlideshowCount;
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public String getItem(int pPosition) {
		// TODO Auto-generated method stub
		return mSlideshowAssetDir+"/"+mSlideshowPreffix+"_"+String.valueOf(pPosition+1)+"."+mSlideshowSuffix;
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
	@Override
	public View getView(int pPosition, View pConvertView, ViewGroup pParent) {
		// TODO Auto-generated method stub
		View v = pConvertView;
		String path = getItem(pPosition);
		Bitmap bmp = BitmapFactory.decodeFile(path);
		if(v == null) {
			v = inflater.inflate(R.layout.slideshow_item_layout, pParent, false);
		}
		ImageView iv = (ImageView) v.findViewById(R.id.SlideshowImage);
		iv.setImageBitmap(bmp);
		return v;
	}
}

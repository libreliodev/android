package com.librelio.products.utils.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.v4.util.SparseArrayCompat;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.librelio.products.ui.CustomizationHelper;

public class CursorViewBinder {
	Context context;
	protected SparseArrayCompat <String> map;
	
	
	public CursorViewBinder(Context context, String fieldArrayResourceName, String viewIdArrayResourceName) {
		this.context = context;
		initWithResources(fieldArrayResourceName, viewIdArrayResourceName);
	}
	
	public CursorViewBinder(Context context, String[] columns, int[] ids) {
		this.context = context;
		initWithArrays(columns, ids);
	}
	private void initWithArrays(String[] columns, int[] ids) {
		if(columns.length != ids.length)
			throw new IllegalStateException("Arrays length is not equal");
		map = new SparseArrayCompat<String>(ids.length);
		for(int i = 0; i < ids.length; i++) {
			map.put(ids[i], columns[i]);
		}
	}

	public void initWithResources(String fieldArrayResourceName, String viewIdArrayResourceName) {
		int fieldArrayId = context.getResources().getIdentifier(fieldArrayResourceName, "array", context.getPackageName());
		int viewArrayId = context.getResources().getIdentifier(viewIdArrayResourceName, "array", context.getPackageName());
		String[] fieldArray = context.getResources().getStringArray(fieldArrayId);
		String[] viewArray = context.getResources().getStringArray(viewArrayId);
		if(fieldArray.length != viewArray.length)
			throw new IllegalStateException("Arrays length is not equal: " + fieldArrayResourceName + " " + viewIdArrayResourceName);
		map = new SparseArrayCompat<String>(fieldArray.length);
		for(int i = 0; i < fieldArray.length; i++) {
			int resourceId = context.getResources().getIdentifier(viewArray[i], "id", context.getPackageName());
			map.put(resourceId, fieldArray[i]);
		}
	}

	public boolean bindView(View v, Cursor c) {
		int id = v.getId();
		String columnName = map.get(id);
		if (columnName == null)
			return false;
		int columnIndex = c.getColumnIndex(columnName);
		if (columnIndex == -1) {
			return false;
		}
		String value = c.getString(columnIndex);
		if (value == null) {
			return false;
		}
		if (v instanceof TextView) {
			TextView tv = (TextView) v;
			tv.setText((value==null) ? "" : Html.fromHtml(value));
			return true;
		}
		if (v instanceof ImageView) {
			ImageView iv = (ImageView) v;
			if(value.equals("")) {
				iv.setVisibility(View.GONE);
				return true;
			}
			// First, try to find the resource id
			String drawable = value.replace(".jpg", "").replace(".png", "").toLowerCase();
			int bitmapid = context.getResources().getIdentifier(drawable, "drawable", context.getPackageName());
			if(bitmapid != 0 ) {
				iv.setImageResource(bitmapid);
				iv.setVisibility(View.VISIBLE);
				return true;
			}
			try {
				Bitmap pic = BitmapFactory.decodeFile(CustomizationHelper.ASSETS_PHOTOS_URI + value);
				if(value.startsWith("Photos")) {
					Matrix mtx = new Matrix();
					mtx.postScale(0.5f, 0.5f);
					mtx.postRotate(-90, pic.getWidth() / 2, pic.getHeight() / 2);
					iv.setImageBitmap(Bitmap.createBitmap(pic,
							(int) (2 * pic.getWidth() /3) , 0,
							pic.getWidth() / 3 , pic.getHeight(), mtx, true));
				} else {
					iv.setImageBitmap(pic);
				}
				iv.setVisibility(View.VISIBLE);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
}

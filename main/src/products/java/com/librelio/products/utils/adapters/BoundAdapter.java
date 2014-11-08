package com.librelio.products.utils.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class BoundAdapter extends BaseAdapter {

	protected CursorViewBinder binder;
	private int listItemLayoutId;
	private LayoutInflater inflater;
	private Cursor cursor;
	private String mSortOrder = null;
	
	public BoundAdapter(Context context, Cursor c, int listItemLayoutId, CursorViewBinder binder) {
		super();
		this.binder = binder;
		this.listItemLayoutId = listItemLayoutId;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.cursor = c;
		cursor.moveToFirst();
		this.notifyDataSetChanged();
	}
	
	@Override
	public void notifyDataSetChanged() {
		if( cursor!= null)
			cursor.moveToFirst();
		super.notifyDataSetChanged();
	}
	
	public void setCursor(Cursor newCursor) {
		this.cursor = newCursor;
		this.notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return cursor.getCount();
	}

	@Override
	public Cursor getItem(int position) {
		cursor.moveToPosition(position);
		return cursor;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if(v == null) {
			v = inflater.inflate(listItemLayoutId, parent, false);
		}
		bindViews(v, position);
		return v;
	}


	protected void bindViews(View v, int position) {
		if(v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for(int i = 0; i < vg.getChildCount(); i++) {
				View child = vg.getChildAt(i);
				if(child instanceof ViewGroup) {
					bindViews(child, position);
				} else {
					binder.bindView(child, getItem(position));
				}
			}
		}
	}
	
	public void setSortOrder(String orderBy) {
		this.mSortOrder  = orderBy;
	}

}

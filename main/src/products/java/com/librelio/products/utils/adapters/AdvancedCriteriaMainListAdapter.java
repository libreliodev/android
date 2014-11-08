package com.librelio.products.utils.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.librelio.products.utils.db.ProductsDBHelper;

public class AdvancedCriteriaMainListAdapter extends BaseAdapter {

	ProductsDBHelper helper;
	Context context;
	int count;
	Cursor cursor;
	LayoutInflater inflater;
	int layout_id;
	int textViewId;
	private int criteriaTextView;

	public AdvancedCriteriaMainListAdapter(ProductsDBHelper helper, Context context, int layout_id, int textViewId, int criteriaTextView) {
		super();
		this.helper = helper;
		this.context = context;
		this.layout_id = layout_id;
		this.textViewId = textViewId;
		this.criteriaTextView = criteriaTextView;
		notifyDataSetChanged();
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	
	@Override
	public void notifyDataSetChanged() {
		cursor = helper.getAllAdvancedCriteria();
	}
	
	@Override
	public int getCount() {
		return cursor.getCount();
	}

	@Override
	public Object getItem(int position) {
		cursor.moveToPosition(position);
		return cursor.getString(cursor.getColumnIndexOrThrow(ProductsDBHelper.ADVANCED_CRITERIA_TITLE));
	}
	
	public String getColumnName(int position) {
		cursor.moveToPosition(position);
		return cursor.getString(cursor.getColumnIndexOrThrow(ProductsDBHelper.ADVANCED_CRITERIA_COLNAME));
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View view, ViewGroup viewGroup) {
		View v = view;
		if ( v == null) {
			v = this.inflater.inflate(layout_id, viewGroup, false);
		}
		TextView mTitle = (TextView) v.findViewById(textViewId);
		mTitle.setText(getItem(position).toString());
		TextView mDescription = (TextView) v.findViewById(criteriaTextView);
		mDescription.setText(helper.getUserSearchINputStringByColumn(getColumnName(position)));
		
		return v;
	}

	public String getPrevSearchText() {
		String text="";
		try {
			Cursor crit = ProductsDBHelper.getDBHelper().getAllFromTable("AdvancedCriteria");
			while(!crit.isAfterLast()) {
				String result = "";
				String title = crit.getString(crit.getColumnIndexOrThrow("Title"));
				String critColName = crit.getString(crit.getColumnIndexOrThrow("ColName"));
				Cursor c = ProductsDBHelper.getDBHelper().getAllFromTableWithWhereAndOrder("UserSearchInputsOld", "ColName LIKE '%" + critColName + "%'", null);
				if(c!= null && c.getCount() > 0) {
					while(!c.isAfterLast()) {
						result += c.getString(c.getColumnIndexOrThrow("Title")) + ",";
						c.moveToNext();
					}
				}  
				if(!result.equals("")) {
					text += title+":" + result;
					
				}
				crit.moveToNext();
				
			}

		} catch (Exception e) {
			// table does not exists, exiting
		}
		return text;
	}
}

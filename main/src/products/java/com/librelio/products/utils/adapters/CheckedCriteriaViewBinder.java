package com.librelio.products.utils.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.CheckBox;

public class CheckedCriteriaViewBinder extends CursorViewBinder {

	public CheckedCriteriaViewBinder(Context context, String[] columns,
			int[] ids) {
		super(context, columns, ids);
	}
	@Override
	public boolean bindView(View v, Cursor c) {
		int id = v.getId();
		String columnName = map.get(id);
		if(columnName == null)
			return false;
		String value = c.getString(c.getColumnIndexOrThrow(columnName));

		if(v instanceof CheckBox) {
			CheckBox cb = (CheckBox) v;
			cb.setTag(value);
			return true;
		} else return super.bindView(v, c);		
	}
}

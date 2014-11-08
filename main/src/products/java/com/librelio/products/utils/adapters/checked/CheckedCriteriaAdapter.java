package com.librelio.products.utils.adapters.checked;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.librelio.products.utils.adapters.BoundAdapter;
import com.librelio.products.utils.adapters.CursorViewBinder;
import com.librelio.products.utils.db.ProductsDBHelper;

public class CheckedCriteriaAdapter extends BoundAdapter {

	private CriteriaChangeListener listener;
	private ProductsDBHelper helper;
	private String columnName;

	public CheckedCriteriaAdapter(Context context, Cursor c, int listItemLayoutId,
			CursorViewBinder binder, ProductsDBHelper helper, String columnName) {
		super(context, c, listItemLayoutId, binder);
		this.helper = helper;
		this.columnName = columnName;
	}
	
	public void setOnCriteriaCangeListener (CriteriaChangeListener l) {
		listener = l;
	}

	@Override
	protected void bindViews(View v, int position) {
		if(v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for(int i = 0; i < vg.getChildCount(); i++) {
				View child = vg.getChildAt(i);
				if(child instanceof ViewGroup) {
					bindViews(child, position);
				} else {
					binder.bindView(child, getItem(position));
					if(child instanceof CheckBox) {
						CheckBox cb = (CheckBox)child;
						Cursor cursor = helper.rawQuery("SELECT * from UserSearchInputs WHERE "+
						"COLNAME = ? AND UserInput LIKE ?", new String [] {
								columnName,
								"%"+cb.getTag().toString()+"%"
								
						});
						cb.setChecked(cursor.getCount() > 0);
						cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {

							@Override
							public void onCheckedChanged(CompoundButton cb,
									boolean checked) {
								if(cb.isPressed()) {
									listener.onCriteriaChanged(cb.getTag().toString(), checked);
								}
							}});
					}
				}
			}
		}
	}
	
	public interface CriteriaChangeListener {
		public void onCriteriaChanged(String value, boolean checked);
	}
}

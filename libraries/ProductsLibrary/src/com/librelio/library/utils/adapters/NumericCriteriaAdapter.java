package com.librelio.library.utils.adapters;

import com.librelio.library.utils.db.DBHelper;

import android.content.Context;
import android.database.Cursor;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class NumericCriteriaAdapter extends MyBaseCriteriaAdapter {
	DBHelper helper;
	Context context;
	int count;
	Cursor cursor;
	LayoutInflater inflater;
	int layout_id;
	int textViewId;
	int editViewId;
	String mColName;

	public NumericCriteriaAdapter(DBHelper helper, Context context,
			String mColName, int layout_id, int textViewId, int editViewId) {
		super();
		this.helper = helper;
		this.context = context;
		this.layout_id = layout_id;
		this.textViewId = textViewId;
		this.editViewId = editViewId;
		this.mColName = mColName;
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		notifyDataSetChanged();
	}

	@Override
	public void notifyDataSetChanged() {
		// workaround for DB bugs
		mColName = mColName.replaceAll("Tailles", "Taille");
		cursor = helper.getAllFromTable("AdvancedInput__" + mColName);
		if (cursor != null) {
			cursor.moveToFirst();
			headerText = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.ADVANCED_SELECT_HEADER_KEY));
		}
		super.notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return cursor.getCount();
	}

	@Override
	public Cursor getItem(int position) {
		cursor.moveToPosition(position);
		return cursor;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			v = inflater.inflate(layout_id, parent, false);
		}
		Cursor c = getItem(position);
		int id = c.getColumnIndexOrThrow(DBHelper.ADVANCED_CRITERIA_TITLE);
		String title = c.getString(id);
		TextView tv = (TextView) v.findViewById(textViewId);
		tv.setText(title);
		EditText et = (EditText) v.findViewById(editViewId);
		et.setText(c.getString(c.getColumnIndexOrThrow(DBHelper.ADVANCED_SELECT_INPUT_KEY)));
		et.setTag(position);
		et.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView view, int actionId,
					KeyEvent event) {
				if (actionId != EditorInfo.IME_ACTION_DONE) {
					return false;
				}
				EditText et = (EditText) view;
				int position = (Integer) et.getTag();
				Cursor c = getItem(position);
				String value = et.getEditableText().toString();
				String action = c.getString(c.getColumnIndexOrThrow(DBHelper.ADVANCED_SELECT_DETAILLINK_KEY)).replaceAll("%@", value);

				Log.d("SQL", action);

				// value = "'" + value + "'";
				helper.rawQuery(action, null).moveToFirst();

				if (action.startsWith("DELETE")) {
					try {
						Integer.valueOf(value);
						Cursor cursor = getItem(position);
						String newAction = cursor
								.getString(
										c.getColumnIndexOrThrow(DBHelper.ADVANCED_SELECT_DETAILLINK_KEY))
								.replaceAll("%@", value);
						helper.rawQuery(newAction, null);
					} catch (NumberFormatException e) {

					}
				}

				// report to self and parent listview dataset has been
				// changed
				notifyDataSetChanged();
				c = getItem(position);

				return true;
			}
		});
		return v;
	}

}

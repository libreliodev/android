package com.librelio.library.ui.criteraselectors;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.librelio.library.utils.adapters.checked.CheckedCriteriaAdapter;
import com.librelio.library.utils.db.DBHelper;
import com.niveales.testskis.R;

public class RangeCriteriaSelectorFragment extends Fragment {
	View rootView;
	private String colName;

	// private int layout = R.layout.creteria_selector_fragment_layout;
	CheckedCriteriaAdapter mAdapter;
	private Context context;
	private int layoutId;
	private String type;
	private OnRangeCriteriaChangedListener listener;
	private int listViewId;
	private int minEditTextId;
	private int maxEditTextId;
	private String criteria;
	private int titleResourceId;
	private EditText min;
	private EditText max;

	/**
	 * 
	 * @param type
	 * @param criteria
	 * @param titleResourceId
	 * @param helper
	 * @param context
	 * @param colName
	 * @param layoutId
	 * @param minEditTextId
	 * @param maxEditTextId
	 * @param l
	 * @return
	 */
	public static RangeCriteriaSelectorFragment getInstance(String type,
			String criteria, int titleResourceId,
			Context context, String colName, int layoutId, int minEditTextId,
			int maxEditTextId, OnRangeCriteriaChangedListener l) {
		RangeCriteriaSelectorFragment f = new RangeCriteriaSelectorFragment();
		f.init(type, criteria, titleResourceId, context, colName,
				layoutId, minEditTextId, maxEditTextId, l);
		return f;
	}

	private void init(String type, String criteria, int titleResourceId,
			 Context context, String colName, int layoutId,
			int minEditTextId, int maxEditTextId,
			OnRangeCriteriaChangedListener l) {
		this.type = type;
		this.criteria = criteria;
		this.titleResourceId = titleResourceId;
		this.context = context;
		this.colName = colName;
		this.layoutId = layoutId;
		this.minEditTextId = minEditTextId;
		this.maxEditTextId = maxEditTextId;
		this.listener = l;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(layoutId, container, false);
		TextView title = (TextView) rootView.findViewById(titleResourceId);
		title.setText(criteria);

		min = (EditText) rootView.findViewById(minEditTextId);
		min.setTag("Mini");
		Cursor cursor = DBHelper.getDBHelper().rawQuery(
				"select * from UserSearchInputs where ColName='" + colName
						+ "' AND Title LIKE '%Mini%'", null);
		if (cursor.getCount() > 0) {
			min.setText(cursor.getString(cursor
					.getColumnIndexOrThrow("UserInput")));
		}
		
		min.addTextChangedListener( new TextWatcher() {

			@Override
			public void afterTextChanged(Editable pArg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void beforeTextChanged(CharSequence pArg0, int pArg1,
					int pArg2, int pArg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence seq, int pArg1, int pArg2,
					int pArg3) {
				saveEdit(max);
				saveEdit(min);
							
			}});
		
		min.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView view, int actionId,
					KeyEvent event) {
				saveEdit(max);
				saveEdit(min);
				return true;
			}
		});
		

		max = (EditText) rootView.findViewById(maxEditTextId);
		max.setTag("Max");
		cursor = DBHelper.getDBHelper().rawQuery(
				"select * from UserSearchInputs where ColName='" + colName
						+ "' AND Title LIKE '%Max%'", null);
		if (cursor.getCount() > 0) {
			max.setText(cursor.getString(cursor
					.getColumnIndexOrThrow("UserInput")));
		}
		max.addTextChangedListener( new TextWatcher() {

			@Override
			public void afterTextChanged(Editable pArg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void beforeTextChanged(CharSequence pArg0, int pArg1,
					int pArg2, int pArg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence seq, int pArg1, int pArg2,
					int pArg3) {
				saveEdit(max);
				saveEdit(min);
							
			}});
		max.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView view, int actionId,
					KeyEvent event) {
				saveEdit(max);
				saveEdit(min);
				return true;
			}
		});

		TextView description = (TextView) rootView
				.findViewById(R.id.CriteriaMaxDescription);
		description.setText(criteria + " maxi:");
		description = (TextView) rootView
				.findViewById(R.id.CriteriaMinDescription);
		description.setText(criteria + " mini:");
		description = (TextView) rootView.findViewById(R.id.CriteriaTitle);
		if (criteria.toLowerCase().equals("prix"))
			description.setText("Indiquez votre budget:");
		if (criteria.toLowerCase().equals("taille"))
			description.setText("Définissez une taille:");
		return rootView;
	};

	public void saveEdit(EditText et) {
		String metaIndicator = "";
		if (criteria.toLowerCase().startsWith("prix")) {
			metaIndicator = "€";
		}
		if (criteria.toLowerCase().equals("taille")) {
			metaIndicator = "cm";
		}
		String tag = et.getTag().toString();
		DBHelper.getDBHelper().rawQuery("delete from UserSearchInputs where ColName='"
				+ colName + "' AND Title LIKE '%" + tag + "%'", null);
		String value = et.getEditableText().toString();
		String selectionColumn = colName;
		if (!value.equals("")) {
//			if (colName.toLowerCase().equals("tailles")
//					&& tag.toLowerCase().equals("max")) {
//				selectionColumn = "TailleMax";
//			}
//			if (colName.toLowerCase().equals("tailles")
//					&& tag.toLowerCase().equals("mini")) {
//				selectionColumn = "TailleMin";
//			}
			if (colName.toLowerCase().equals("tailles")) {
				int min = getMinTaille();
				int max = getMaxiTaille();
				if (min > max) {
					// Toast.makeText(context, "Min can't be more than max",
					// Toast.LENGTH_SHORT).show();
					return;
				}
				StringBuilder builder = new StringBuilder();
				for (int i = min; i <= max; i++) {
					builder.append("Tailles").append(" LIKE '%")
							.append(String.valueOf(i)).append("%' OR ");
				}
				String string = builder.toString();
				DBHelper.getDBHelper().rawQuery(
						"insert into userSearchInputs values (?, ?, ?, ?)",
						new String[] { colName, value,
								tag + ":" + value + metaIndicator,
								string.substring(0, string.length() - 3) });
				if (tag.equals("min")) {
					// only fire listener once for min & max
					listener.onCriteriaChanged(colName);
				}
			} else {
				DBHelper.getDBHelper().rawQuery(
						"insert into userSearchInputs values (?, ?, ?, ?)",
						new String[] {
								colName,
								value,
								tag + ":" + value + metaIndicator,
								(tag.toLowerCase().equals("max")) ? selectionColumn
										+ " < " + value
										: selectionColumn + " > " + value, });
				listener.onCriteriaChanged(colName);
			}
		}
	}

	public void onPause() {
		saveEdit(max);
		saveEdit(min);
		super.onPause();
	}

	public interface OnRangeCriteriaChangedListener {
		public void onCriteriaChanged(String colName);
	}
	
	public int getMinTaille() {
		try {
			return Integer.parseInt(min.getEditableText().toString());
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public int getMaxiTaille() {
		try {
			return Integer.parseInt(max.getEditableText().toString());
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 200;
		}
	}
}

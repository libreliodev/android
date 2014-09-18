package com.librelio.library.ui.productsearch;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

import com.librelio.library.ui.Button3State;
import com.librelio.library.ui.Button3State.OnStateChanged;
import com.librelio.library.ui.productlist.ProductListFragment;
import com.librelio.library.utils.adapters.BoundAdapter;
import com.librelio.library.utils.adapters.CursorViewBinder;
import com.librelio.library.utils.adapters.search.SearchAdapter;
import com.librelio.library.utils.db.DBHelper;

public class ProductSearchFragment extends Fragment {
	public static final String SORT_UP="%S ASC";
	public static final String SORT_DOWN="%S DESC";
	
	ListView mProductListView;
	View rootView;
	private DBHelper helper;
	private Cursor cursor;
	private int layoutId;
	private int listViewId;
	private int itemLayoutId;
	String mSortOrder = null;
	private SearchAdapter mAdapter;
	private String table;
	Button3State mMarqueSortButton;
	Button3State mGammeSortButton;
	Button3State mPrixSortButton;
	String [] searchColumns;
	private CursorViewBinder binder;
	private OnProductSearchSelectedListener listener;
	private String whereClaus;
	private int editTextId;
	private EditText mSearchEditText;
	
	/**
	 * 
	 * @param helper - instance of DBHelper
	 * @param table - String with product list table name in DB
	 * @param productListFagmentLayout - id of the layout 
	 * @param productlistview -  id if the ListView instance which will hold the products info
	 * @param productListItemLayout - layout id of the product list item
	 * @param editTextId - id of the EditText with search words
	 * @param searchColumns - columns to search in
	 * @param binder - view binder
	 * @return - instance of ProductSearchFragment
	 */
	public static ProductSearchFragment getInstance(DBHelper helper,
			String table, int productListFagmentLayout, int productlistview,
			int productListItemLayout, int editTextId, String [] searchColumns, CursorViewBinder binder) {

		ProductSearchFragment f = new ProductSearchFragment();
		f.init(helper, table, productListFagmentLayout, productlistview, productListItemLayout, editTextId, searchColumns, binder);
		return f;
	}
	/**
	 * 
	 * @param pHelper
	 * @param table
	 * @param layoutId - ProductList layout
	 * @param listViewId - id if the ListView instance which will hold the products info
	 * @param itemLayoutId - layout id of the product list item
	 * @param sortButtonsIds - must be IDs of Button3State instances in layout layoutId
	 * @param sortColumns - array of strings with columns to sort by
	 * @param binder - CursorViewBinder to bind views
	 * @return
	 */
	public ProductSearchFragment init(DBHelper pHelper, String table, int layoutId, int listViewId, int itemLayoutId, int editTextId, String [] sortColumns, 
			CursorViewBinder binder){
		helper = pHelper;
		this.table = table;
		this.layoutId = layoutId;
		this.listViewId = listViewId;
		this.itemLayoutId = itemLayoutId;
		
		this.editTextId = editTextId;
		this.searchColumns = sortColumns;
		this.binder = binder;
		this.whereClaus = "";

		this.cursor = getCursor(table, null, null);
		Log.d("WHERE", whereClaus);
		return this;
	};
	
	public Cursor getCursor(String table, String where, String order) {
		return helper.getAllFromTableWithWhereAndOrder(table, where, order);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(layoutId,
				container, false);
		if(cursor == null)
			throw new IllegalStateException("Please do initWithHelper with Coursor instance.");
		mProductListView = (ListView) rootView.findViewById(listViewId);
		mAdapter = new SearchAdapter(getActivity(), cursor, itemLayoutId, binder);
		mProductListView.setAdapter(mAdapter);
		mProductListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> listView, View parent, int item,
					long id) {
				Object a = listView.getAdapter();
				if(a instanceof BoundAdapter) {
					BoundAdapter adapter = (BoundAdapter) a;
					Cursor cursor = adapter.getItem(item);
//					int productId = cursor.getInt(cursor.getColumnIndexOrThrow("id_modele"));
					onProductSelected(cursor);
				}
			}});
		mSearchEditText = (EditText) getActivity().findViewById(editTextId);
		mSearchEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable pArg0) {
			}

			@Override
			public void beforeTextChanged(CharSequence pArg0, int pArg1,
					int pArg2, int pArg3) {
			}

			@Override
			public void onTextChanged(CharSequence pArg0, int pArg1, int pArg2,
					int pArg3) {
				String search = mSearchEditText.getEditableText().toString();
				updateSearch(search);
				
			}} );
		this.updateSearch(mSearchEditText.getEditableText().toString());
		return rootView;
	}
	
	/**
	 * @param pSearch
	 */
	protected void updateSearch(String pSearch) {
		String [] searchWords = pSearch.split(" ");
		whereClaus = "";
		boolean isFirst = true;
		for(int i = 0; i < searchWords.length; i++) {
			String w = "";
			boolean isFirstJ = true;
			for(int j = 0; j < searchColumns.length; j++) {
				if(isFirstJ) {
					w += searchColumns[j] + " LIKE '%" + searchWords[i] + "%'";
					isFirstJ = false;
				} else {
					w += " OR " + searchColumns[j] + " LIKE '%" + searchWords[i] + "%'";
				}
			}
			if(isFirst) {
				whereClaus += "( " + w + " )";
				isFirst = false;
			} else {
				whereClaus += " AND ( " + w + " )";
			}
		}
		cursor = getCursor(table, whereClaus, null);
		mAdapter.setCursor(cursor);
		this.mProductListView.invalidateViews();
	}

	protected void onProductSelected(Cursor c) {
		listener.onSearchProductSelected(c);
	}

	public void setOnProductSearchSelectedListener(OnProductSearchSelectedListener l) {
		listener = l;
	}
	
	public interface OnProductSearchSelectedListener {
		public void onSearchProductSelected(Cursor c);
	}
}

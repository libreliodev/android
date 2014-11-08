package com.librelio.products.ui.productlist;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.librelio.products.ui.Button3State;
import com.librelio.products.utils.adapters.BoundAdapter;
import com.librelio.products.utils.adapters.CursorViewBinder;
import com.librelio.products.utils.db.ProductsDBHelper;
import com.niveales.wind.R;

public class ProductListFragment extends Fragment {
	public static final String SORT_UP="%S ASC";
	public static final String SORT_DOWN="%S DESC";
	
	ListView mProductListView;
	View rootView;
	protected ProductsDBHelper helper;
	private Cursor cursor;
	private int layoutId;
	private int listViewId;
	private int itemLayoutId;
	String mSortOrder = null;
	private BoundAdapter mAdapter;
	private String table;
	Button3State mMarqueSortButton;
	Button3State mGammeSortButton;
	Button3State mPrixSortButton;
	int [] sortButtonsIds;
	String [] sortColumns;
	private CursorViewBinder binder;
	private ProductSelectedListener listener;
	private String whereClaus;
	
	/**
	 * 
	 * @param table - String with product list table name in DB
	 * @param productListFagmentLayout - id of the layout 
	 * @param productlistview -  id if the ListView instance which will hold the products info
	 * @param productListItemLayout - layout id of the product list item
	 * @param sortButtonIds - must be IDs of Button3State instances in layout @param productListFagmentLayout
	 * @param sortColumns - array of strings with columns to sort by pressing @param sortButtonIds
	 * @return
	 */
	public static ProductListFragment getInstance(String table,
			int productListFagmentLayout, int productlistview, int productListItemLayout,
			int [] sortButtonIds, String [] sortColumns, CursorViewBinder binder) {

		ProductListFragment f = new ProductListFragment();
		f.init(table, productListFagmentLayout, productlistview, productListItemLayout, sortButtonIds, sortColumns, binder);
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
	public ProductListFragment init(String table, int layoutId, int listViewId, int itemLayoutId, int [] sortButtonsIds, String [] sortColumns, 
			CursorViewBinder binder){
		helper = ProductsDBHelper.getDBHelper();
		this.table = table;
		this.layoutId = layoutId;
		this.listViewId = listViewId;
		this.itemLayoutId = itemLayoutId;
		
		this.sortButtonsIds = sortButtonsIds;
		this.sortColumns = sortColumns;
		this.binder = binder;
		this.whereClaus = "";
		Cursor tempCursor = helper.getAllFromTableWithOrder("AdvancedCriteria", "Title");
		boolean isFirst = true;
		while (!tempCursor.isAfterLast()) {
			String title = tempCursor.getString(0);
			String advColName = tempCursor.getString(1);
			String type = tempCursor.getString(2);
			String headerText = tempCursor.getString(3);
			String operation;
			if(type.toLowerCase().equals("numeric"))
				operation = "AND";
			else operation = type;
			Cursor searchInputsCursor = helper
					.rawQuery(
							"select group_concat(querystring, \" " + operation + " \") from UserSearchInputs where colname=?",
							new String[] {
									advColName 
							});
			String tempWhere = searchInputsCursor.getString(0);
			if (tempWhere != null) {
				if (isFirst) {
					whereClaus = "( " + tempWhere + ") ";
					isFirst = false;
				} else {
					whereClaus += " AND " + "( " + tempWhere + ") ";
				}
			}
			tempCursor.moveToNext();
		}
		
		this.cursor = getCursor(table, whereClaus, mSortOrder);
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
		mAdapter = new BoundAdapter(getActivity(), cursor, itemLayoutId, binder);
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
		
		mMarqueSortButton = (Button3State) rootView.findViewById(sortButtonsIds[0]);
		this.mMarqueSortButton.setOnStateChangeListener(new Button3State.OnStateChanged() {
			@Override
			public void onStateChanged(Button3State pView, int state) {
				switch (state) {
				case 0: {
					mSortOrder = null;
					pView.setBackgroundResource(R.drawable.iphone_marque_unselected);
					NotifySortOrderChanged();
					break;
				}
				case 1: {
					pView.setBackgroundResource(R.drawable.iphone_marque_selected);
					mSortOrder = SORT_UP.replace("%S", sortColumns[0]);
					NotifySortOrderChanged();
					break;
				}
				case 2: {
					pView.setBackgroundResource(R.drawable.iphone_marque_selected);
					mSortOrder = SORT_DOWN.replace("%S", sortColumns[0]);
					NotifySortOrderChanged();
					break;
				}
				}
				mGammeSortButton.setState(0);
				mPrixSortButton.setState(0);
			}});
		mGammeSortButton = (Button3State) rootView.findViewById(sortButtonsIds[1]);
		mGammeSortButton.setOnStateChangeListener(new Button3State.OnStateChanged() {
			@Override
			public void onStateChanged(Button3State pView, int state) {
				switch (state) {
				case 0: {
					pView.setBackgroundResource(R.drawable.iphone_gamme_unselected);
					mSortOrder = null;
					NotifySortOrderChanged();
					break;
				}
				case 1: {
					pView.setBackgroundResource(R.drawable.iphone_gamme_selected);
					mSortOrder = SORT_UP.replace("%S", sortColumns[1]);
					NotifySortOrderChanged();
					break;
				}
				case 2: {
					pView.setBackgroundResource(R.drawable.iphone_gamme_selected);
					mSortOrder = SORT_DOWN.replace("%S", sortColumns[1]);
					NotifySortOrderChanged();
					break;
				}
				}
				mMarqueSortButton.setState(0);
				mPrixSortButton.setState(0);
			}});
		
		
		mPrixSortButton = (Button3State) rootView.findViewById(sortButtonsIds[2]);
		mPrixSortButton.setOnStateChangeListener(new Button3State.OnStateChanged() {
			@Override
			public void onStateChanged(Button3State pView, int state) {
				switch (state) {
				case 0: {
					pView.setBackgroundResource(R.drawable.iphone_prix_unselected);
					mSortOrder = null;
					NotifySortOrderChanged();
					break;
				}
				case 1: {
					pView.setBackgroundResource(R.drawable.iphone_prix_selected);
					mSortOrder = SORT_UP.replace("%S", sortColumns[2]);
					NotifySortOrderChanged();
					break;
				}
				case 2: {
					pView.setBackgroundResource(R.drawable.iphone_prix_selected);
					mSortOrder = SORT_DOWN.replace("%S", sortColumns[2]);
					NotifySortOrderChanged();
					break;
				}
				}
				mGammeSortButton.setState(0);
				mMarqueSortButton.setState(0);
			}});
		return rootView;
	}
	
	protected void NotifySortOrderChanged() {
		cursor = getCursor(table, whereClaus, mSortOrder);
		mAdapter.setCursor(cursor);
	}

	protected void onProductSelected(Cursor c) {
		listener.showProductDetails(c);
	}

	public void setOnProductSelectedListener(ProductSelectedListener l) {
		listener = l;
	}
	
	public interface ProductSelectedListener {
		public void showProductDetails(Cursor c);
	}



	
}

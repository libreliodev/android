package com.librelio.products.utils.adapters.search;

import com.librelio.products.ui.CustomizationHelper;
import com.librelio.products.utils.adapters.BoundAdapter;
import com.librelio.products.utils.adapters.CursorViewBinder;
import com.librelio.products.utils.db.ProductsDBHelper;

import android.content.Context;
import android.database.Cursor;
import android.widget.Filter;
import android.widget.Filterable;

public class SearchAdapter extends BoundAdapter implements Filterable {

	protected CursorViewBinder binder;
	private String whereClaus;
	private static String[] searchColumns = CustomizationHelper.ProductSearchConstants.PRODUCT_SEARCH_BINDER_COLUMNS;
	private static int[] searchLayoutIds = CustomizationHelper.ProductSearchConstants.PRODUCT_SEARCH_BINDER_IDS;

	public SearchAdapter(Context context) {
		super(context, ProductsDBHelper.getDBHelper().getAllFromTableWithWhereAndOrder(
				CustomizationHelper.DETAIL_TABLE_NAME, null, null),
				CustomizationHelper.ProductSearchConstants.PRODUCT_SEARCH_LISTVIEW_ITEM_LAYOUT, new CursorViewBinder(
						context, searchColumns, searchLayoutIds));
	}

	/**
	 * @param pActivity
	 * @param pCursor
	 * @param pItemLayoutId
	 * @param pBinder
	 */
	public SearchAdapter(Context context, Cursor pCursor, int pItemLayoutId,
			CursorViewBinder pBinder) {
		super(context, pCursor, pItemLayoutId, pBinder);
	}

	/**
	 * @param pSearch
	 *            - search string
	 */
	public void setSearch(String pSearch) {
		String[] searchWords = pSearch.split(" ");
		whereClaus = "";
		boolean isFirst = true;
		for (int i = 0; i < searchWords.length; i++) {
			String w = "";
			boolean isFirstJ = true;
			for (int j = 0; j < searchColumns.length; j++) {
				if (isFirstJ) {
					w += searchColumns[j] + " LIKE '%" + searchWords[i] + "%'";
					isFirstJ = false;
				} else {
					w += " OR " + searchColumns[j] + " LIKE '%"
							+ searchWords[i] + "%'";
				}
			}
			if (isFirst) {
				whereClaus += "( " + w + " )";
				isFirst = false;
			} else {
				whereClaus += " AND ( " + w + " )";
			}
		}
		setCursor(getCursor(CustomizationHelper.DETAIL_TABLE_NAME,
				whereClaus));
	}

	public Cursor getCursor(String table, String where) {
		return ProductsDBHelper.getDBHelper()
				.getAllFromTableWithWhereAndOrder(table, where, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Filterable#getFilter()
	 */
	@Override
	public Filter getFilter() {
		// TODO Auto-generated method stub
		return productFilter;
	}

	Filter productFilter = new Filter() {
		String lastConstraint;
		public String convertResultToString(Object resultValue) {
			return (lastConstraint == null) ? "" : lastConstraint;
		}

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults res = new FilterResults();
			res.values = constraint;
			res.count = (constraint == null) ? 0 : 1;
			return res;
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			if(constraint != null) {
				lastConstraint = constraint.toString();
				setSearch(lastConstraint);
				notifyDataSetChanged();
			}
//			notifyDataSetInvalidated();
		}
	};

}

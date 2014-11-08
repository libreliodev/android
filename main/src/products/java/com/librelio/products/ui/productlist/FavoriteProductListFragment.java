package com.librelio.products.ui.productlist;

import android.database.Cursor;

import com.librelio.products.utils.adapters.CursorViewBinder;
import com.librelio.products.utils.db.ProductsDBHelper;

public class FavoriteProductListFragment extends ProductListFragment {
	
	
	/**
	 * 
	 * @param helper - instance of DBHelper
	 * @param table - String with product list table name in DB
	 * @param productListFagmentLayout - id of the layout 
	 * @param productlistview -  id if the ListView instance which will hold the products info
	 * @param productListItemLayout - layout id of the product list item
	 * @param sortButtonIds - must be IDs of Button3State instances in layout @param productListFagmentLayout
	 * @param sortColumns - array of strings with columns to sort by pressing @param sortButtonIds
	 * @return
	 */
	public static FavoriteProductListFragment getInstance(ProductsDBHelper helper,
			String table, int productListFagmentLayout, int productlistview,
			int productListItemLayout, int [] sortButtonIds, String [] sortColumns, CursorViewBinder binder) {

		FavoriteProductListFragment f = new FavoriteProductListFragment();
		f.init(table, productListFagmentLayout, productlistview, productListItemLayout, sortButtonIds, sortColumns, binder);
		return f;
	}
	
	@Override
	public Cursor getCursor(String table, String where, String order) {
		return helper.getAllFromTableWithWhereAndOrder(table+" , UserFavorites", "Detail.id_modele = UserFavorites.id", order);
	}
	
}

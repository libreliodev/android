package com.librelio.library.ui.productlist;

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

import com.librelio.library.ui.Button3State;
import com.librelio.library.ui.Button3State.OnStateChanged;
import com.librelio.library.utils.adapters.BoundAdapter;
import com.librelio.library.utils.adapters.CursorViewBinder;
import com.librelio.library.utils.db.DBHelper;

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
	public static FavoriteProductListFragment getInstance(DBHelper helper,
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

package com.librelio.products.ui.lexique;

import com.librelio.products.utils.adapters.lexique.LexiqueAdapter;
import com.librelio.products.utils.db.ProductsDBHelper;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class LexiqueFragment extends Fragment {

	View rootView;
	private int[] viewIds;
	private int layoutId;
	private int listViewId;
	private int listViewItemLayoutId;
/**
 * 
 * @param helper - instance of DBHelper
 * @param layoutId - layout ID of the lexique fragment
 * @param ListViewItemId - ListView view id in @param layoutId layout
 * @param listViewItemLayoutId - layout id of listview item in lexique
 * @param viewIds - array of view ids to bind "Lexique" table columns in the order of "Lexique" table columns
 * @return
 */
	public static LexiqueFragment getInstance(int layoutId,
			int ListViewItemId, 
			int listViewItemLayoutId,
			int[] viewIds) {
		LexiqueFragment f = new LexiqueFragment();
		f.init(layoutId, ListViewItemId, listViewItemLayoutId, viewIds);
		return f;
	}

	private void init(int layoutId, int listViewId, int listViewItemLayoutId,
			int[] viewIds) {
		this.layoutId = layoutId;
		this.listViewId = listViewId;
		this.listViewItemLayoutId = listViewItemLayoutId;
		this.viewIds = viewIds;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(layoutId, container, false);
		ListView mLexiqueListView = (ListView) rootView
				.findViewById(listViewId);
		mLexiqueListView.setAdapter(new LexiqueAdapter(getActivity(), ProductsDBHelper.getDBHelper()
				.getAllLexique(), listViewItemLayoutId, viewIds));
		return rootView;
	}
}

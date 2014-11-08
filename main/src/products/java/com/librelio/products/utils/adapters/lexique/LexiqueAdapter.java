package com.librelio.products.utils.adapters.lexique;

import android.content.Context;
import android.database.Cursor;

import com.librelio.products.utils.adapters.BoundAdapter;
import com.librelio.products.utils.adapters.CursorViewBinder;
import com.librelio.products.utils.db.ProductsDBHelper;
/**
 * 
 * @author valetin
 * @param context - context
 * @param cursor - cursor
 * @param itemLayoutId - layout from R.layout with list item layout
 * @param layoutResourceIds - id of resources to bind 
 */
public class LexiqueAdapter extends BoundAdapter {
	
	/**
	 * 
	 * @param context
	 * @param cursor
	 * @param itemLayout - layout of the ListView item
	 * @param layoutResourceIds - resource ids to map from cursor
	 */
	public LexiqueAdapter(Context context, Cursor cursor, int itemLayout, int [] layoutResourceIds) {
		super(context, cursor, itemLayout, new CursorViewBinder(context, new String[] {
				ProductsDBHelper.LEXIQUE_GAMME_KEY,
				ProductsDBHelper.LEXIQUE_DESCRIPTION_KEY
		}, layoutResourceIds));
	}

}

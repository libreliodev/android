package com.librelio.library.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;

import com.librelio.library.ui.criteraselectors.CheckedCriteriaSelectorFragment;
import com.librelio.library.ui.criteraselectors.CheckedCriteriaSelectorFragment.OnCriteriaChangedListener;
import com.librelio.library.ui.criteraselectors.RangeCriteriaSelectorFragment;
import com.librelio.library.ui.criteraselectors.RangeCriteriaSelectorFragment.OnRangeCriteriaChangedListener;
import com.librelio.library.ui.lexique.LexiqueFragment;
import com.librelio.library.ui.productdetail.ProductDetailFragment;
import com.librelio.library.ui.productdetail.ProductDetailFragment.ShareProductListener;
import com.librelio.library.ui.productlist.FavoriteProductListFragment;
import com.librelio.library.ui.productlist.ProductListFragment;
import com.librelio.library.ui.productsearch.ProductSearchFragment;
import com.librelio.library.utils.adapters.CursorViewBinder;
import com.librelio.library.utils.db.DBHelper;
import com.niveales.testskis.R;

/**
 * @author Dmitry Valetin
 * 
 */

public class NivealesApplication extends Application {

	public static class ProductSearchConstants {

		public static final int[] PRODUCT_SEARCH_BINDER_IDS = new int[] {
				R.id.productListItemGenre, R.id.productListItemModele,
				R.id.productListItemGamme, R.id.productListItemBudget };
		public static final String[] PRODUCT_SEARCH_BINDER_COLUMNS = new String[] {
				// columns to display in search results list
				DBHelper.MODELE_MARQUE_KEY, DBHelper.MODELE_MODELE_KEY,
				"Gamme", "Prix_String", };
		public static final String[] PRODUCT_SEARCH_SEARCH_COLUMNS = new String[] {
				DBHelper.MODELE_MARQUE_KEY, DBHelper.MODELE_MODELE_KEY, "Gamme" };
		public static final int PRODUCT_SEARCH_LISTVIEW_ITEM_LAYOUT = R.layout.product_search_item_layout;
		public static final int PRODUCT_SEARCH_FAGMENT_LAYOUT = R.layout.product_search_fagment_layout;

	}

	public static class CriteriaSelectorConstants {
		public static final int CRITERIA_SELECTOR_RIGHTPANE_TITLE_TEXTVIEW = R.id.RightPaneTitleTextView;
		public static final int CRITERIA_SELECTOR_CRITERIA_CHECKBOX_VIEW_ID = R.id.CriteriaCheckBox;
		public static final int CRITERIA_SELECTOR_CRITERIA_TEXTVIEW_VIEW_ID = R.id.CriteriaTextView;
		public static final int CRETERIA_SELECTOR_LISTVIEW_VIEW_ID = R.id.CreteriaSelectorListView;
		public static final int CHECKED_CRITERIA_SELECTOR_ITEM_LAYOUT_ID = R.layout.checked_criteria_selector_item_layout;
		public static final int CRETERIA_SELECTOR_FRAGMENT_LAYOUT_ID = R.layout.creteria_selector_fragment_layout;
		public static final int CRETERIA_SELECTOR_TITLE_VIEW_ID = R.id.CriteriaTitle;
	}

	public static class ProductDetailConstants {
		public static final int PRODUCT_DETAIL_SHARE_BUTTON_VIEW_ID = R.id.ShareButton;
		public static final int PRODUCT_DETAIL_FAVORITE_CKECKBOX_VIEW_ID = R.id.FavoriteCkeckBox;
		public static String[] PRODUCT_DETAIL_HTML_FILE_KEYS = new String[] {
				"%TAITLE%",
				"%Modele%",
				"%Budget%",
				"%img%", // product image
				"%GAMME%", "%TAILLE TESTEE%", "%TAILLES DISPONIBLES%",
				"%type_de_cambre_text%", "%Test_baseline%",
				"%Description_Test%", "%Test_avantages%",
				"%test_inconvenients%", "%icone_genre%", "%icone_cambres%",
				"%icone_wide%", "%icone_top%", "%img_niveau%",
				"%img_polyvalence%", "%Caractéristiques%",
				"%NIVEAU REQUIS%", "%icone_testerchoice%"
		};

		public static String[] PRODUCT_DETAIL_COLUMN_KEYS = // List of
															// fields in
															// product
															// html file
		new String[] { "Marque", "Modele", "Prix_String", "imgLR", "Gamme",
				"test_Taille_testee", "Tailles", "type_de_cambre_text",
				"Test_baseline", "Description_Test", "Test_avantages",
				"test_inconvenients", "icone_genre", "icone_cambres",
				"icone_wide", "icone_top", "img_niveau", "img_polyvalence",
				"Caractéristiques", "niveau", "icone_testerchoice"
		// List of Details table columns to get data from, used to fill HTML
		// fields above
		};

		public static final int PRODUCT_DETAIL_WEBPAGE_FILE_URI = R.string.ProductDetailWebPage;
		public static final int PRODUCT_DETAIL_WEBVIEW_VIEW_ID = R.id.ProductDetailsWebView;
		public static final int PRODUCT_DETAIL_LAYOUT = R.layout.product_detail_layout;
		// public static final int PRODUCT_DETAIL_SHAREHOLDER_VIEW_ID =
		// R.id.ShareHolder;
		public static final int PRODUCTDETAIL_NEXTBUTTON_VIEW_ID = R.id.NextButton;
		public static final int PRODUCT_DETAIL_PREVBUTTON_VIEW_ID = R.id.PrevButton;
		public static final int PRODUCTDETAIL_PRODUCTIMAGE_VIEW_ID = R.id.ProductImageAnchor;
		// public static final String HTML_TITLE = "%TAITLE%";
		// public static final String HTML_MODELE = "%Modele%";
		// public static final String HTML_BUDGET = "%Budget%";
		// public static final String HTML_GAMME = "%GAMME%";
		// public static final String HTML_CHARACTER = "%CARACTERE%";
		// public static final String HTML_NIVEAU_REQUIS = "%NIVEAU REQUIS%";
		// public static final String HTML_TALLE_TESTEE = "%TAILLE TESTEE%";
		// public static final String HTML_TEST_BASELINE = "%Test_baseline%";
		// public static final String HTML_DESC = "%Description_Test%";
		// public static final String HTML_TEST_ADV = "%Test_avantages%";
		// public static final String HTML_TEST_DISADV = "%test_inconvenients%";
		// public static final String HTML_CHARACTERISTICS =
		// "%Caractéristiques%";
		// public static final String HTML_ICON_TESTCHOICE =
		// "%icone_testerchoice%";
		// public static final String HTML_ICON_SEX = "%icone_genre%";
		// public static final String HTML_PIC = "%img%";
		public static final int PRODUCTDETAIL_PRODUCTIMAGE_POPUP_LAYOUT_ID = R.layout.product_image_popup;
	}

	public static final String DETAIL_TABLE_NAME = "Detail";

	private static class ProductListConstants {
		private static String[] PRODUCT_LIST_DISPLAY_COLUMNS = new String[] {
				DBHelper.MODELE_MARQUE_KEY, DBHelper.MODELE_MODELE_KEY,
				"icone_genre", "icone_cambres", "Gamme", "Prix_String",
				"icone_wide", "icone_testerchoice", "imgLR"
		// DBHelper.MODELE_PRIX_DE_REFERENCE_KEY,
		// DBHelper.MODELE_GENRE_KEY,
		// DBHelper.MODELE_IMG_KEY
		};
		private static int[] PRODUCT_LIST_DISPLAY_VIEW_IDS = new int[] {
				R.id.productListItemGenre, R.id.productListItemModele,
				R.id.productListItemFemale, R.id.productListItemChambre,
				R.id.productListItemGamme, R.id.productListItemBudget,
				R.id.productListItemWide, R.id.productListItemTesterChoice,
				R.id.productListItemPicture };
		private static final int PRODUCT_LIST_FAGMENT_LAYOUT = R.layout.product_list_fagment_layout;
		private static final int PRODUCT_LIST_LISTVIEW_ITEM_LAYOUT = R.layout.product_list_item_layout;
		private static final int PRODUCT_LIST_LISTVIEW_VIEW_ID = R.id.ProductListView;
		/**
		 * list of button ids in product list layout
		 */
		private static final int[] PRODUCT_LIST_SORT_BUTTON_IDS = new int[] {
				R.id.ProductListMarqueSortButton,
				R.id.ProductListGammeSortButton, R.id.ProductListPrixSortButton };
		private static final String[] PRODUCT_LIST_SORT_COLUMNS = new String[] {
				"Marque", "Gamme", "Prix_de_reference" };
	}

	// Bitly constants
	public static final String BITLY_USER = "tedted1";
	public static final String BITLY_API_KEY = "R_d0e2739e13391fc7cc6a7c66966239b4";

	// App constants
	public static final String MAIN_TAB_ID = "tab_id";
	public static final String ASSETS_URI = "file:///android_asset/";
	public static final String ASSETS_PHOTOS_URI = ASSETS_URI + "Photos/";
	public static final String SELECTED_ID = "selectedid";
	public static final String SELECTED_VALUE = "selected_value";
	public static final String SELECTED_CRITERIA_ID = "criteria_id";
	public static final String SELECTED = "Selected.png";
	public static final String UNSELECTED = "NotSelected.png";
	public static final String NUMERIC = "Numeric";

	// UI function helpers to help customize future apps
	public static ProductDetailFragment getProductDetailFragment(Cursor pCursor,
			ShareProductListener pListener) {
		ProductDetailFragment f = new ProductDetailFragment();
		f.setOnShareProductListener(pListener);
		f.setProductCursor(pCursor);
		return f;
	}

	public static ProductListFragment getProductListFragment(Context context) {
		return ProductListFragment.getInstance(DETAIL_TABLE_NAME,
				ProductListConstants.PRODUCT_LIST_FAGMENT_LAYOUT,
				ProductListConstants.PRODUCT_LIST_LISTVIEW_VIEW_ID,
				ProductListConstants.PRODUCT_LIST_LISTVIEW_ITEM_LAYOUT,
				ProductListConstants.PRODUCT_LIST_SORT_BUTTON_IDS,
				ProductListConstants.PRODUCT_LIST_SORT_COLUMNS,
				new CursorViewBinder(context,
						ProductListConstants.PRODUCT_LIST_DISPLAY_COLUMNS,
						ProductListConstants.PRODUCT_LIST_DISPLAY_VIEW_IDS));
	}

	public static FavoriteProductListFragment getFavoriteProductListFragment(Context context) {
		return FavoriteProductListFragment.getInstance(DBHelper.getDBHelper(),
				DETAIL_TABLE_NAME,
				ProductListConstants.PRODUCT_LIST_FAGMENT_LAYOUT,
				ProductListConstants.PRODUCT_LIST_LISTVIEW_VIEW_ID,
				ProductListConstants.PRODUCT_LIST_LISTVIEW_ITEM_LAYOUT,
				ProductListConstants.PRODUCT_LIST_SORT_BUTTON_IDS,
				ProductListConstants.PRODUCT_LIST_SORT_COLUMNS,
				new CursorViewBinder(context,
						ProductListConstants.PRODUCT_LIST_DISPLAY_COLUMNS,
						ProductListConstants.PRODUCT_LIST_DISPLAY_VIEW_IDS));
	}

	public static ProductSearchFragment getProductSearchFragment(Context context, int searchEditTextId) {
		return ProductSearchFragment.getInstance(DBHelper.getDBHelper(),
				DETAIL_TABLE_NAME,
				ProductSearchConstants.PRODUCT_SEARCH_FAGMENT_LAYOUT,
				ProductListConstants.PRODUCT_LIST_LISTVIEW_VIEW_ID,
				ProductSearchConstants.PRODUCT_SEARCH_LISTVIEW_ITEM_LAYOUT,
				searchEditTextId,
				ProductSearchConstants.PRODUCT_SEARCH_SEARCH_COLUMNS,
				new CursorViewBinder(context,
						ProductSearchConstants.PRODUCT_SEARCH_BINDER_COLUMNS,
						ProductSearchConstants.PRODUCT_SEARCH_BINDER_IDS));
	}

	public static LexiqueFragment getLexiqueFragment() {
		return LexiqueFragment.getInstance(R.layout.lexique_fragment_layout, R.id.LexiqueListView,
				R.layout.lexique_list_item_layout, new int[] {
						R.id.LexiqueItemTerm, R.id.LexiqueItemTermDefinition });
	}

	public static RangeCriteriaSelectorFragment getRangeCriteriaSelectorFragment(Context context, String type, String criteria, String colName,
			OnRangeCriteriaChangedListener l) {
		return RangeCriteriaSelectorFragment
				.getInstance(
						type,
						criteria,
						CriteriaSelectorConstants.CRITERIA_SELECTOR_RIGHTPANE_TITLE_TEXTVIEW,
						context, colName,
						R.layout.range_criteria_selector_layout,
						R.id.MinPriceInputField, R.id.MaxPriceInputField, l);
	}

	public static CheckedCriteriaSelectorFragment getCheckedCriteriaSelectorFragment(
			int pPosition, OnCriteriaChangedListener l) {
		return CheckedCriteriaSelectorFragment.getInstance(pPosition, l);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		InputStream is;
		try {
			is = getAssets().open("testmatos/" + "customization.json");
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));
			StringWriter writer = new StringWriter();
			char[] buffer = new char[1024];
			int count = 0;
			while ((count = reader.read(buffer)) > 0) {
				writer.write(buffer, 0, count);
			}
			JSONObject json = new JSONObject(writer.toString());
			JSONArray htmlKeys = json
					.getJSONArray("PRODUCT_DETAIL_HTML_FILE_KEYS");
			ProductDetailConstants.PRODUCT_DETAIL_HTML_FILE_KEYS = new String[htmlKeys
					.length()];
			for (int i = 0; i < htmlKeys.length(); i++) {
				ProductDetailConstants.PRODUCT_DETAIL_HTML_FILE_KEYS[i] = htmlKeys
						.getString(i);
			}
			JSONArray columnKeys = json
					.getJSONArray("PRODUCT_DETAIL_COLUMN_KEYS");
			ProductDetailConstants.PRODUCT_DETAIL_COLUMN_KEYS = new String[columnKeys
					.length()];
			for (int i = 0; i < columnKeys.length(); i++) {
				ProductDetailConstants.PRODUCT_DETAIL_COLUMN_KEYS[i] = columnKeys
						.getString(i);
			}

			JSONArray displayColumns = json
					.getJSONArray("PRODUCT_LIST_DISPLAY_COLUMNS");
			ProductListConstants.PRODUCT_LIST_DISPLAY_COLUMNS = new String[displayColumns
					.length()];
			for (int i = 0; i < displayColumns.length(); i++) {
				ProductListConstants.PRODUCT_LIST_DISPLAY_COLUMNS[i] = displayColumns
						.getString(i);
			}

			JSONArray displayViewIds = json
					.getJSONArray("PRODUCT_LIST_DISPLAY_VIEW_IDS");
			ProductListConstants.PRODUCT_LIST_DISPLAY_VIEW_IDS = new int[displayViewIds
					.length()];
			for (int i = 0; i < displayViewIds.length(); i++) {
				String resourceName = displayViewIds.getString(i);
				ProductListConstants.PRODUCT_LIST_DISPLAY_VIEW_IDS[i] = getResources()
						.getIdentifier(resourceName, "id", getPackageName());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}

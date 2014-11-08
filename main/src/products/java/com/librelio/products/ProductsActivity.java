package com.librelio.products;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

import com.librelio.model.dictitem.ProductsItem;
import com.librelio.products.ui.BaseNivealesFragment;
import com.librelio.products.ui.criteraselectors.RangeCriteriaSelectorFragment;
import com.librelio.products.ui.productdetail.ProductDetailFragment;
import com.librelio.products.ui.productlist.FavoriteProductListFragment;
import com.librelio.utils.StorageUtils;
import com.niveales.wind.R;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.librelio.products.ui.CustomizationHelper;
import com.librelio.products.ui.criteraselectors.CheckedCriteriaSelectorFragment.OnCriteriaChangedListener;

import com.librelio.products.ui.productlist.ProductListFragment;
import com.librelio.products.ui.productlist.ProductListFragment.ProductSelectedListener;
import com.librelio.products.utils.adapters.AdvancedCriteriaMainListAdapter;
import com.librelio.products.utils.adapters.search.SearchAdapter;
import com.librelio.products.utils.db.ProductsDBHelper;

public class ProductsActivity extends FragmentActivity {

	private static final String DIALOG_TAG = null;
	private static final String FILEPATHFROMPLIST = "filename";
	private static final String ITEMFILENAME = "itemfilepath";
    private static final String ASSETSFOLDER = "assetsfolder";
    private static final String TITLE = "title";
	@SuppressWarnings("unused")
	private static final String TAG = ProductsActivity.class.getSimpleName();
    private int mActiveTab;
	private TabHost mMainActivityTabHost;
	private View mRightFrameFragmentHolder;
	private ListView mMainActivityCreteriaSelectionListView;
	private AdvancedCriteriaMainListAdapter mainAdapter;
	// private FrameLayout mSearchResultHolder;
	private AutoCompleteTextView mSearchEditText;
	private ImageButton mMainLayoutSearchButton;
	public ProgressDialog mProgressDialog;
	public String mRecentSearch;
	private TextView mPrevSearchTextView;
	private TextView mNewSearchTextView;
	private int mLastSelectedMainItem = 1;
	
	public static void startActivity(Context context, ProductsItem item) {
		context.startActivity(getIntent(context, item));
	}

	public static Intent getIntent(Context context, ProductsItem item) {
		Intent intent = new Intent(context, ProductsActivity.class);
		intent.putExtra(FILEPATHFROMPLIST, item.getFilePath());
		intent.putExtra(ITEMFILENAME, item.getItemFileName());
        intent.putExtra(ASSETSFOLDER, item.getItemStorageDir());
        intent.putExtra(TITLE, item.getTitle());
		return intent;

	}

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Initialize constants
        CustomizationHelper.initCustomization(this);
        CustomizationHelper.ASSETS_PHOTOS_URI = getIntent().getStringExtra(ASSETSFOLDER);

		if (savedInstanceState != null) {
			mActiveTab = savedInstanceState
					.getInt(CustomizationHelper.MAIN_TAB_ID);
		}

		setContentView(R.layout.main_activity_layout);

        getActionBar().setTitle(getIntent().getStringExtra(TITLE));

		// Init DB
		ProductsDBHelper.setDBHelper(new ProductsDBHelper(this, getIntent().getStringExtra
                (FILEPATHFROMPLIST), getIntent().getStringExtra(ITEMFILENAME)));
		ProductsDBHelper.getDBHelper().open();

		initViews();
		restoreAppState();
	}

	@SuppressLint("NewApi")
	public void initViews() {

		// Setup device screen configuration
		Configuration newConfig = getResources().getConfiguration();
		if ((newConfig.screenLayout & Configuration.SCREENLAYOUT_SIZE_XLARGE) == 0)
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		else
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		// Init tabs

		mMainActivityTabHost = (TabHost) findViewById(R.id.MainLayoutTabHost);
		initTabs(mMainActivityTabHost);

		// Init tab select listener
		mMainActivityTabHost.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String arg0) {
				changeTab(arg0);
			}
		});

		// Restore last selected tab from last run
		mMainActivityTabHost.setCurrentTab(mActiveTab);

		// Init Search button click listener
		mMainLayoutSearchButton = (ImageButton) findViewById(R.id.MainLayoutSearchButton);
		mNewSearchTextView = (TextView) findViewById(R.id.NewSearchTextView);
		this.initSearchButton();

		// Right Frame Holder exists only in xlarge layouts. Other devices with
		// screen size
		// less then 7" should not have this view in a main_activity_layout.xml,
		// this way findViewById return null and
		// we use null later to recognize our device type
		mRightFrameFragmentHolder = findViewById(R.id.ContentHolder);

		mMainActivityCreteriaSelectionListView = (ListView) findViewById(R.id.MainActivityCreteriaSelectionListView);
		mMainActivityCreteriaSelectionListView
				.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int position, long id) {
						showSelectionCategory(position);
					}
				});
		mMainActivityCreteriaSelectionListView
				.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		mainAdapter = new AdvancedCriteriaMainListAdapter(
				ProductsDBHelper.getDBHelper(), this,
				R.layout.creteria_group_selector_item_layout,
				R.id.CreteriaGroupTextView, R.id.CreteriaSelectedListTextView);
		mMainActivityCreteriaSelectionListView.setAdapter(mainAdapter);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		// mSearchResultHolder = (FrameLayout)
		// findViewById(R.id.SearchResultHolder);

		mSearchEditText = (AutoCompleteTextView) findViewById(R.id.SearchEditText);
		mSearchEditText.setAdapter(new SearchAdapter(this));
		mSearchEditText.setThreshold(1);
		mSearchEditText.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> pArg0, View pArg1,
					int pArg2, long pArg3) {
				InputMethodManager imm = (InputMethodManager) ProductsActivity.this
						.getSystemService(Activity.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
				showProductDetail((Cursor) pArg0.getAdapter().getItem(pArg2));
			}
		});
		mSearchEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

		mPrevSearchTextView = (TextView) findViewById(R.id.PrevSearchTextView);
		mPrevSearchTextView.setText(getPrevSearchText());
		mPrevSearchTextView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View pArg0) {
				onPrevSearchClick();
			}
		});
	}

	protected void onPrevSearchClick() {
		ProductsDBHelper helper = ProductsDBHelper.getDBHelper();
		try {
			helper.rawQuery("delete from UserSearchInputs", null);
			helper.rawQuery(
					"insert into UserSearchInputs select * from UserSearchInputsOld",
					null);
			this.mMainActivityCreteriaSelectionListView.invalidateViews();
			this.mMainLayoutSearchButton.setVisibility(View.VISIBLE);
			this.mNewSearchTextView.setVisibility(View.INVISIBLE);
			onSearchButtonClick();
		} catch (Exception e) {
			// table does not exists, do nothing
			e.printStackTrace();
		}
	}

	public String getPrevSearchText() {
		String text = "";
		try {
			Cursor crit = ProductsDBHelper.getDBHelper().getAllFromTable(
					"AdvancedCriteria");
			while (!crit.isAfterLast()) {
				String result = "";
				String title = crit.getString(crit
						.getColumnIndexOrThrow("Title"));
				String critColName = crit.getString(crit
						.getColumnIndexOrThrow("ColName"));
				Cursor c = ProductsDBHelper.getDBHelper()
						.getAllFromTableWithWhereAndOrder(
								"UserSearchInputsOld",
								"ColName LIKE '%" + critColName + "%'", null);
				if (c != null && c.getCount() > 0) {
					while (!c.isAfterLast()) {
						result += c.getString(c.getColumnIndexOrThrow("Title"))
								+ ",";
						c.moveToNext();
					}
				}
				if (!result.equals("")) {
					text += title + ":" + result;
				}
				crit.moveToNext();
			}

			if (!text.equals("")) {
				text = Html.fromHtml("<b>Ma dernière recherche:</b><br>")
						+ text;
				this.mPrevSearchTextView.setVisibility(View.VISIBLE);
			} else {
				this.mPrevSearchTextView.setVisibility(View.GONE);
			}

		} catch (Exception e) {
			// table does not exists, exiting
		}
		return text;
	}

	private void initTabs(TabHost pTabHost) {
		String[] tabNames = this.getResources().getStringArray(
				R.array.TabsNames);
		pTabHost.setup();
		TabHost.TabSpec spec = pTabHost.newTabSpec(tabNames[0]);
		Button b = new Button(this);
		b.setPadding(0, b.getPaddingTop(), 0, b.getPaddingBottom());
		b.setBackgroundResource(R.drawable.tab_button);
		b.setTextColor(getResources().getColorStateList(
				R.drawable.tab_button_textcolor));
		b.setText(tabNames[0]);

		spec.setIndicator(b);
		spec.setContent(R.id.main_list_tab);
		// spec1.setIndicator(tabNames[0]);
		pTabHost.addTab(spec);

		spec = pTabHost.newTabSpec(tabNames[1]);
		spec.setContent(R.id.favorites_tab);
		b = new Button(this);
		b.setBackgroundResource(R.drawable.tab_button);
		b.setTextColor(getResources().getColorStateList(
				R.drawable.tab_button_textcolor));
		b.setText(tabNames[1]);
		spec.setIndicator(b);
		pTabHost.addTab(spec);

		spec = pTabHost.newTabSpec(tabNames[2]);
		spec.setContent(R.id.terms_tab);
		b = new Button(this);
		b.setBackgroundResource(R.drawable.tab_button);
		b.setTextColor(getResources().getColorStateList(
				R.drawable.tab_button_textcolor));
		b.setText(tabNames[2]);
		spec.setIndicator(b);
		pTabHost.addTab(spec);
	}

	private void changeTab(String tabId) {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		prefs.edit().putString("TAB_ID", tabId).commit();
		String[] tabNames = this.getResources().getStringArray(
				R.array.TabsNames);
		if (tabId.equals(tabNames[0])) {
			/**
			 * Search criteria tab. Clean the right pane if selected And show
			 */

		} else if (tabId.equals(tabNames[1])) {
			/**
			 * Favorites
			 */
			FavoriteProductListFragment f = CustomizationHelper
					.getFavoriteProductListFragment(this);
			f.setOnProductSelectedListener(new ProductSelectedListener() {
				@Override
				public void showProductDetails(Cursor c) {

					showProductDetail(c);
				}
			});
			attachFavorites(f, "favorite");

		} else if (tabId.equals(tabNames[2])) {
			/**
			 * Lexique
			 */
			if (this.mRightFrameFragmentHolder != null) {
				// Tablet
				Fragment lexiqueFragment = CustomizationHelper
						.getLexiqueFragment();
				this.getSupportFragmentManager().beginTransaction()
						.replace(R.id.ContentHolder, lexiqueFragment)
						.addToBackStack(null).commit();
			} else {
				// Phone
				Fragment lexiqueFragment = CustomizationHelper
						.getLexiqueFragment();
				this.getSupportFragmentManager().beginTransaction()
						.replace(R.id.terms_tab, lexiqueFragment).commit();
			}
		}
	}

	private void attachFavorites(FavoriteProductListFragment f,
			String fragmentTag) {
		if (this.mRightFrameFragmentHolder != null) {
			this.getSupportFragmentManager().beginTransaction()
					.replace(R.id.ContentHolder, f, fragmentTag)
					.addToBackStack(fragmentTag).commit();
		} else {
			this.getSupportFragmentManager().beginTransaction()
					.replace(R.id.favorites_tab, f, fragmentTag).commit();
		}
	}

	protected void onSearchButtonClick() {
		String whereClaus = null;
		ProductsDBHelper helper = ProductsDBHelper.getDBHelper();
		Cursor tempCursor = helper.getAllFromTableWithOrder("AdvancedCriteria",
				"Title");
		boolean isFirst = true;
		while (!tempCursor.isAfterLast()) {
			String title = tempCursor.getString(0);
			String advColName = tempCursor.getString(1);
			String type = tempCursor.getString(2);
			String headerText = tempCursor.getString(3);
			String operation;
			if (type.toLowerCase().equals("numeric"))
				operation = "AND";
			else
				operation = type;
			Cursor searchInputsCursor = helper.rawQuery(
					"select group_concat(querystring, \" " + operation
							+ " \") from UserSearchInputs where colname=?",
					new String[] { advColName });
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

		Cursor productCursor = helper.getAllFromTableWithWhereAndOrder(
				CustomizationHelper.DETAIL_TABLE_NAME, whereClaus, null);
		if (productCursor.getCount() > 0) {
			ProductListFragment f = CustomizationHelper.getProductListFragment(this);
			f.setOnProductSelectedListener(new ProductSelectedListener() {
				@Override
				public void showProductDetails(Cursor c) {
					showProductDetail(c);
				}
			});
			attachSearchResults(f, "searchresults");
		} else {
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setMessage(R.string.nothing_found_message);
			b.setPositiveButton("Close", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface pDialog, int pWhich) {
					pDialog.dismiss();

				}
			});
			Dialog d = b.create();
			d.requestWindowFeature(Window.FEATURE_NO_TITLE);
			d.show();
		}

	}

	public void onClearSearchClick() {
		ProductsDBHelper.getDBHelper().rawQuery(
				"delete from UserSearchInputsOld", null);
		ProductsDBHelper.getDBHelper()
				.rawQuery(
						"insert into UserSearchInputsOld select * from UserSearchInputs",
						null);
		ProductsDBHelper.getDBHelper().rawQuery(
				"delete from UserSearchInputs", null);
		// TestSnowboardsApplication.getDBHelper().rawQuery(
		// "delete from UserSearchInputs", null);
		mMainActivityCreteriaSelectionListView.invalidateViews();
		mPrevSearchTextView.setText(getPrevSearchText());
		this.initSearchButton();
	}

	// /**
	// * called when user clicks on search input field
	// */
	// protected void onSearchStarted() {
	// final SearchPopup mSearchPopup =
	// getMyApplication().getProductSearchPopup(this.mSearchEditText);
	// mSearchPopup.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
	// LayoutParams.MATCH_PARENT));
	// mSearchPopup.show();
	//
	// }
	// protected void onOldSearchStarted() {
	// final ProductSearchFragment f = getMyApplication()
	// .getProductSearchFragment(
	// R.id.SearchEditText);
	// f.setOnProductSearchSelectedListener(new
	// OnProductSearchSelectedListener() {
	//
	// @Override
	// public void onSearchProductSelected(Cursor c) {
	// InputMethodManager imm = (InputMethodManager)
	// TestSnowboardsMainActivity.this
	// .getSystemService(Activity.INPUT_METHOD_SERVICE);
	// imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
	// getSupportFragmentManager().beginTransaction().remove(f)
	// .commit();
	// showProductDetail(c);
	// }
	// });
	//
	// if (this.mSearchResultHolder != null) {
	// this.getSupportFragmentManager().beginTransaction()
	// .replace(R.id.SearchResultHolder, f)
	// .addToBackStack("search").commit();
	// }
	// }

	protected void showProductDetail(Cursor c) {
		ProductDetailFragment productDetailFragment = CustomizationHelper
				.getProductDetailFragment(c, new ProductDetailFragment.ShareProductListener() {

                    @Override
                    public void onShareProduct(Cursor productId) {
                        shareProduct(productId);
                    }
                });
		attachProductDetailFragment(productDetailFragment, "productdetail");
	}

	/**
	 * @param productDetailFragment
	 * @param fragmentTag
	 */
	private void attachProductDetailFragment(Fragment productDetailFragment,
			String fragmentTag) {
		if (mRightFrameFragmentHolder != null) {

			int orientation = getResources().getConfiguration().orientation;
			int layout = getResources().getConfiguration().screenLayout;
			if (orientation == Configuration.ORIENTATION_PORTRAIT
					&& ((layout & Configuration.SCREENLAYOUT_SIZE_XLARGE) != 0))
				this.getSupportFragmentManager()
						.beginTransaction()
						.replace(R.id.ProductDetailsHolder,
								productDetailFragment, fragmentTag)
						.addToBackStack(fragmentTag).commit();
			else
				this.getSupportFragmentManager()
						.beginTransaction()
						.replace(R.id.ContentHolder, productDetailFragment,
								fragmentTag).addToBackStack(fragmentTag)
						.commit();
		} else {

			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.FragmentHolder, productDetailFragment,
							fragmentTag).addToBackStack(fragmentTag).commit();
		}
	}

	private void attachSearchResults(Fragment pFragment, String fragmentTag) {
		if (this.mRightFrameFragmentHolder != null) {
			this.getSupportFragmentManager().beginTransaction()
					.replace(R.id.ContentHolder, pFragment, fragmentTag)
					.addToBackStack(fragmentTag).commit();
		} else {
			this.getSupportFragmentManager().beginTransaction()
					.replace(R.id.FragmentHolder, pFragment, fragmentTag)
					.addToBackStack(fragmentTag).commit();
		}
	}

	protected void showSelectionCategory(int position) {

		mLastSelectedMainItem = position;

		Cursor cursor = ProductsDBHelper.getDBHelper()
				.getAllAdvancedCriteria();

		cursor.moveToPosition(position);
		String criteria = cursor.getString(0);
		String type = cursor.getString(2);
		String colName = cursor.getString(1);
		Fragment f;
		if (type.equals("Numeric")) {
			f = CustomizationHelper.getRangeCriteriaSelectorFragment(
                    this, type, criteria, colName,
                    new RangeCriteriaChangedListener());

		} else {
			f = CustomizationHelper.getCheckedCriteriaSelectorFragment(position,
                    new CriteriaChangeListener());

		}
		attachCriteriaFragment(f, "criteriacategory");

	}

	private void attachCriteriaFragment(Fragment f, String fragmentTag) {
		if (this.mRightFrameFragmentHolder != null) {
			// Tablet
			this.getSupportFragmentManager().beginTransaction()
					.replace(R.id.ContentHolder, f, fragmentTag).commit();
		} else {
			// phone
			this.getSupportFragmentManager().beginTransaction()
					.replace(R.id.FragmentHolder, f, fragmentTag)
					.addToBackStack(fragmentTag).commit();
		}
	}

	public class RangeCriteriaChangedListener implements
            RangeCriteriaSelectorFragment.OnRangeCriteriaChangedListener {

		@Override
		public void onCriteriaChanged(String colName) {
			initSearchButton();
			mMainActivityCreteriaSelectionListView.invalidateViews();

		}

	}

	public class CriteriaChangeListener implements OnCriteriaChangedListener {
		@Override
		public void onCriteriaChanged(String colName) {
			initSearchButton();
			mMainActivityCreteriaSelectionListView.invalidateViews();
		}
	}

	private FragmentTransaction dismissDialogs() {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		DialogFragment prev = (DialogFragment) getSupportFragmentManager()
				.findFragmentByTag(DIALOG_TAG);
		if (prev != null) {
			prev.dismiss();
		}
		ft.addToBackStack(null);
		return ft;
	}

	/**
	 * 
	 * @param productCursor
	 *            - id of the product to share
	 */
	public void shareProduct(Cursor productCursor) {
		Cursor cursor = productCursor;
		String pic = cursor.getString(cursor.getColumnIndexOrThrow("imgLR"));
		String shareString = "";
		String title = "";
		String message = "";
		String url = "";
		try {
			shareString = cursor.getString(cursor
					.getColumnIndexOrThrow("Lien_Partage"));
			Uri uri = Uri.parse(shareString);
			title = URLDecoder
					.decode(uri.getQueryParameter("watitle"), "utf-8");
			message = URLDecoder.decode(uri.getQueryParameter("watext"),
					"utf-8");
			url = URLDecoder.decode(uri.getQueryParameter("walink"), "utf-8");
		} catch (UnsupportedEncodingException e1) {
			//
			e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
			//
			e1.printStackTrace();
		}
		String newURI = "file://"
				+ StorageUtils.copyFileToExternalDirectory(this, pic,
						getAssets());
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT, title);
		intent.putExtra(Intent.EXTRA_TEXT,
				Html.fromHtml(message) + " " + url.toString());
		intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(newURI));
		startActivity(intent);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// int j = newConfig.screenLayout &
		// Configuration.SCREENLAYOUT_SIZE_XLARGE;
		saveFragments();
		setContentView(R.layout.main_activity_layout);
		initViews();
		restoreAppState();
		// if ((newConfig.screenLayout & Configuration.SCREENLAYOUT_SIZE_XLARGE)
		// == 0)
		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// else
		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	}

	public void initSearchButton() {
		Cursor c = ProductsDBHelper.getDBHelper().getAllFromTable(
				"UserSearchInputs");
		if (c.getCount() > 0) {
			mMainLayoutSearchButton
					.setOnClickListener(new SearchButtonClickListener());
			mMainLayoutSearchButton
					.setBackgroundResource(R.drawable.bout_aff_resultat);
			mMainLayoutSearchButton.setVisibility(View.VISIBLE);
			mNewSearchTextView.setVisibility(View.INVISIBLE);
		} else {
			mMainLayoutSearchButton.setVisibility(View.INVISIBLE);
			mNewSearchTextView.setVisibility(View.VISIBLE);
		}
	}

	public class SearchButtonClickListener implements OnClickListener {
		@Override
		public void onClick(View pView) {
			ImageButton b = (ImageButton) pView;
			b.setBackgroundResource(R.drawable.bout_new_recherc_vert);
			b.setOnClickListener(new ClearSearchButtonClickListener());
			onSearchButtonClick();
		}
	}

	public class ClearSearchButtonClickListener implements OnClickListener {
		@Override
		public void onClick(View pView) {
			onClearSearchClick();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		FragmentManager fm = getSupportFragmentManager();
		Fragment f = fm.findFragmentByTag("productdetail");
		SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);
		Boolean isProcessed = false;
		if (f != null && f instanceof BaseNivealesFragment) {
			BaseNivealesFragment bf = (BaseNivealesFragment) f;
			isProcessed |= bf.onBackPressed();
		}
		f = fm.findFragmentByTag("about");
		if (f != null && f instanceof BaseNivealesFragment) {
			BaseNivealesFragment bf = (BaseNivealesFragment) f;
			isProcessed |= bf.onBackPressed();
		}
		f = fm.findFragmentByTag("facebook");
		if (!isProcessed && f != null && f instanceof BaseNivealesFragment) {
			BaseNivealesFragment bf = (BaseNivealesFragment) f;
			isProcessed |= bf.onBackPressed();
		}
		if (isProcessed)
			return;

		super.onBackPressed();
		fm.executePendingTransactions();
		if (fm.getBackStackEntryCount() > 0) {
			String name = fm.getBackStackEntryAt(
					fm.getBackStackEntryCount() - 1).getName();
			prefs.edit().putString("FRAGMENT_TAG", name).commit();
		} else {
			prefs.edit().remove("FRAGMENT_TAG").commit();
		}

	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		super.onAttachFragment(fragment);
		SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);
		// prefs.edit().putString("FRAGMENT", fragment.getTag()).commit();
		// BackStackEntry e =
		// this.getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount
		// () - 1);
		prefs.edit().putInt("FRAGEMENT_ID", fragment.getId())
				.putString("FRAGMENT_TAG", fragment.getTag()).commit();
	}

	public void saveFragments() {
		SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);
		Editor e = prefs.edit();
		if (this.getSupportFragmentManager().getBackStackEntryCount() > 0) {
			String tag = this
					.getSupportFragmentManager()
					.getBackStackEntryAt(
							this.getSupportFragmentManager()
									.getBackStackEntryCount() - 1).getName();
			e.putString("FRAGMENT_TAG", tag);
		} else {
			e.remove("FRAGMENT_TAG");
		}
		e.putInt("MAIN_POSITION", mLastSelectedMainItem);
		e.commit();
	}

	public void restoreAppState() {
		SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);
		String[] tabNames = this.getResources().getStringArray(
				R.array.TabsNames);
		String tabId = prefs.getString("TAB_ID", tabNames[0]);
		changeTab(tabId);
		if (!tabId.equals(tabNames[0]))
			return;
		mLastSelectedMainItem = prefs.getInt("MAIN_POSITION",
				mLastSelectedMainItem);
		String mFragmentTag = prefs.getString("FRAGMENT_TAG", "");
		if (this.mRightFrameFragmentHolder != null) {
			this.mMainActivityCreteriaSelectionListView.setItemChecked(
					mLastSelectedMainItem, true);
			showSelectionCategory(mLastSelectedMainItem);
		}
		// I give up restoring correct app state with all fragments, thus all
		// state restore has been commented out

		// if (mFragmentTag.equals("facebook")) {
		// showFacebookPage();
		// }
		// if (mFragmentTag.equals("about")) {
		// showAboutPage();
		// }
		// if (mFragmentTag.equals("productdetail")) {
		// FragmentManager fm = getSupportFragmentManager();
		// ProductDetailFragment f = (ProductDetailFragment) fm
		// .findFragmentByTag(mFragmentTag);
		// if (f != null) {
		// Cursor c = f.getCursor();
		// this.showProductDetail(c);
		// fm.beginTransaction().remove(f).commit();
		// fm.executePendingTransactions();
		// // attachProductDetailFragment(f, mFragmentTag);
		// }
		// }
		// if (mFragmentTag.equals("searchresults")) {
		// FragmentManager fm = getSupportFragmentManager();
		// ProductListFragment f = (ProductListFragment) fm
		// .findFragmentByTag(mFragmentTag);
		// if (f != null) {
		// attachSearchResults(f, mFragmentTag);
		// }
		// }
		// if (mFragmentTag.equals("favorites")) {
		// FragmentManager fm = getSupportFragmentManager();
		// ProductListFragment f = (ProductListFragment) fm
		// .findFragmentByTag(mFragmentTag);
		// fm.executePendingTransactions();
		// if (f != null) {
		// fm.beginTransaction().remove(f).commit();
		//
		// }
		// }
	}

	public void attachFragment(Fragment f) {

	}
}

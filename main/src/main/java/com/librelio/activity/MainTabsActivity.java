package com.librelio.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.librelio.base.BaseActivity;
import com.librelio.model.dictitem.DictItem;
import com.librelio.model.interfaces.DisplayableAsTab;
import com.librelio.service.AssetDownloadService;
import com.librelio.utils.StorageUtils;
import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Array;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;
import com.niveales.wind.R;
import com.sbstrm.appirater.Appirater;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainTabsActivity extends BaseActivity {

	public static final String REQUEST_SUBS = "request_subs";

	private static final int DIALOG_CANNOT_CONNECT_ID = 1;
	private static final int DIALOG_BILLING_NOT_SUPPORTED_ID = 2;
	private static final int DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID = 3;

	/**
	 * The Purchase receivers
	 */
	private BroadcastReceiver subscriptionYear;
	private BroadcastReceiver subscriptionMonthly;

	private static final String TAG = "MainTabsActivity";
	ViewPager pager;
	private ArrayList<DictItem> tabs = new ArrayList<DictItem>();
	private TabLayout tabLayout;

	public static Intent getIntent(Context context) {
		Intent intent = new Intent(context, MainTabsActivity.class);
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_tabs);

		parseTabsPlist();

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		pager = (ViewPager) findViewById(R.id.view_pager);
		pager.setAdapter(new MainTabsAdapter(getSupportFragmentManager(), tabs));
		pager.setOffscreenPageLimit(4);

		tabLayout = (TabLayout) findViewById(R.id.tabLayout);
		tabLayout.setupWithViewPager(pager);
		tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

		if (getResources().getBoolean(R.bool.enable_app_rating)) {
			Appirater.appLaunched(this);
		}

		IntentFilter subsFilter = new IntentFilter(REQUEST_SUBS);

		registerReceiver(subscriptionYear, subsFilter);
		registerReceiver(subscriptionMonthly, subsFilter);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		if (subscriptionYear != null) {
			unregisterReceiver(subscriptionYear);
		}
		if (subscriptionMonthly != null) {
			unregisterReceiver(subscriptionMonthly);
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		AssetDownloadService.startAssetDownloadService(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main_tabs, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
		case R.id.options_menu_restore:
			restorePurchases();
			return true;
		case R.id.options_menu_subscribe:
			Intent subscribeIntent = new Intent(getBaseContext(),
					BillingActivity.class);
			startActivity(subscribeIntent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void restorePurchases() {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1001) {
			// int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
			String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
			// String dataSignature =
			// data.getStringExtra("INAPP_DATA_SIGNATURE");

			if (resultCode == RESULT_OK & purchaseData != null) {
				try {
					JSONObject jo = new JSONObject(purchaseData);
					String sku = jo.getString("productId");
					Log.d(TAG, "You have bought the " + sku
							+ ". Excellent choice,adventurer!");
				} catch (JSONException e) {
					Log.e(TAG, "Failed to parse purchase data.", e);
				}
			}
		}
	}

	private boolean parseTabsPlist() {

		// TODO do this off ui thread

		String pList = StorageUtils.getFilePathFromAssetsOrLocalStorage(this, "Tabs.plist");

		try {
			PListXMLHandler handler = new PListXMLHandler();
			PListXMLParser parser = new PListXMLParser();
			parser.setHandler(handler);
			parser.parse(pList);
			PList list = ((PListXMLHandler) parser.getHandler()).getPlist();
			Array arr = (Array) list.getRootElement();
			for (int i = 0; i < arr.size(); i++) {
				Dict dict = (Dict) arr.get(i);
				DictItem item = DictItem.parse(this, dict, "");
				tabs.add(item);
			}
		} catch (Exception e) {
			Log.d(getClass().getSimpleName(), "plist = " + pList);
			e.printStackTrace();
		}
		return true;
	}

	private static class MainTabsAdapter extends FragmentStatePagerAdapter {

		private ArrayList<DictItem> tabs;

		public MainTabsAdapter(FragmentManager fm, ArrayList<DictItem> tabs) {
			super(fm);
			this.tabs = tabs;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return tabs.get(position).getTitle();
		}

		@Override
		public Fragment getItem(int position) {
			DictItem item = tabs.get(position);
			if (item instanceof DisplayableAsTab) {
				return ((DisplayableAsTab) item).getFragment();
			}
			return null;
		}

		@Override
		public int getCount() {
			return tabs.size();
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_CANNOT_CONNECT_ID:
			return createDialog(R.string.cannot_connect_title,
					R.string.cannot_connect_message);
		case DIALOG_BILLING_NOT_SUPPORTED_ID:
			return createDialog(R.string.billing_not_supported_title,
					R.string.billing_not_supported_message);
		case DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID:
			return createDialog(R.string.subscriptions_not_supported_title,
					R.string.subscriptions_not_supported_message);
		default:
			return null;
		}
	}

	public Dialog createDialog(int titleId, int messageId) {
		String helpUrl = replaceLanguageAndRegion(getString(R.string.help_url));
		final Uri helpUri = Uri.parse(helpUrl);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(titleId)
				.setIcon(android.R.drawable.stat_sys_warning)
				.setMessage(messageId)
				.setCancelable(false)
				.setPositiveButton(android.R.string.ok, null)
				.setNegativeButton(R.string.learn_more,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								Intent intent = new Intent(Intent.ACTION_VIEW,
										helpUri);
								startActivity(intent);
							}
						});
		return builder.create();
	}

}

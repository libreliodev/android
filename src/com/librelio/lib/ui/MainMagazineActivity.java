/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.librelio.lib.ui;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.artifex.mupdf.MuPDFActivity;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.librelio.activity.BaseActivity;
import com.librelio.lib.LibrelioApplication;
import com.librelio.lib.adapter.MagazineAdapter;
import com.librelio.lib.model.MagazineModel;
import com.librelio.lib.service.DownloadMagazineListService;
import com.librelio.lib.storage.DataBaseHelper;
import com.librelio.lib.storage.Magazines;
import com.librelio.lib.ui.IssueListAdapter.IssueListEventListener;
import com.librelio.lib.utils.BillingService;
import com.librelio.lib.utils.BillingService.RequestPurchase;
import com.librelio.lib.utils.BillingService.RestoreTransactions;
import com.librelio.lib.utils.Consts;
import com.librelio.lib.utils.Consts.PurchaseState;
import com.librelio.lib.utils.Consts.ResponseCode;
import com.librelio.lib.utils.PurchaseObserver;
import com.librelio.lib.utils.ResponseHandler;
import com.librelio.lib.utils.cloud.CloudHelper;
import com.librelio.lib.utils.cloud.CloudHelper.CloudEventListener;
import com.librelio.lib.utils.cloud.Issue;
import com.librelio.lib.utils.cloud.Magazine;
import com.librelio.lib.utils.db.Ocean;
import com.librelio.lib.utils.db.PurchaseDatabase;
import com.niveales.wind.R;

/**
 * A sample application that demonstrates in-app billing.
 */
public class MainMagazineActivity extends BaseActivity implements IssueListEventListener,
		CloudEventListener {
	private static final String TAG = "OceanActivity";
	public static final String REQUEST_SUBS = "request_subs";
	

	/**
	 * The SharedPreferences key for recording whether we initialized the
	 * database. If false, then we perform a RestoreTransactions request to get
	 * all the purchases for this user.
	 */
	private static final String DB_INITIALIZED = "db_initialized";

	private LibrelioPurchaseObserver mLibrelioPurchaseObserver;
	private Handler mHandler;

	private BillingService mBillingService;
	private PurchaseDatabase mPurchaseDatabase;
	private Cursor mOwnedItemsCursor;
	private Set<String> mOwnedItems = new HashSet<String>();

	/**
	 * The developer payload that is sent with subsequent purchase requests.
	 */
	private String mPayloadContents = null;

	private static final int DIALOG_CANNOT_CONNECT_ID = 1;
	private static final int DIALOG_BILLING_NOT_SUPPORTED_ID = 2;
	private static final int DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID = 3;

	/**
	 * Each product in the catalog can be MANAGED, UNMANAGED, or SUBSCRIPTION.
	 * MANAGED means that the product can be purchased only once per user (such
	 * as a new level in a game). The purchase is remembered by Android Market
	 * and can be restored if this application is uninstalled and then
	 * re-installed. UNMANAGED is used for products that can be used up and
	 * purchased multiple times (such as poker chips). It is up to the
	 * application to keep track of UNMANAGED products for the user.
	 * SUBSCRIPTION is just like MANAGED except that the user gets charged
	 * monthly or yearly.
	 */
	private enum Managed {
		MANAGED, UNMANAGED, SUBSCRIPTION
	}

	/**
	 * A {@link PurchaseObserver} is used to get callbacks when Android Market
	 * sends messages to this application so that we can update the UI.
	 */
	public class LibrelioPurchaseObserver extends PurchaseObserver {
		public LibrelioPurchaseObserver(Handler handler) {
			super(MainMagazineActivity.this, handler);
		}

		@Override
		public void onBillingSupported(boolean supported, String type) {
			if (Consts.DEBUG) {
				Log.i(TAG, "supported: " + supported);
			}
			if (type == null || type.equals(Consts.ITEM_TYPE_INAPP)) {
				if (supported) {
					restoreDatabase();
					// mBuyButton.setEnabled(true);
					// mEditPayloadButton.setEnabled(true);
				} else {
					showDialog(DIALOG_BILLING_NOT_SUPPORTED_ID);
				}
			} else if (!type.equals(Consts.ITEM_TYPE_SUBSCRIPTION)) {
				showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
			}
		}

		@Override
		public void onPurchaseStateChange(PurchaseState purchaseState,
				String itemId, int quantity, long purchaseTime,
				String developerPayload) {
			if (Consts.DEBUG) {
				Log.i(TAG, "onPurchaseStateChange() itemId: " + itemId + " "
						+ purchaseState);
			}

			if (purchaseState == PurchaseState.PURCHASED) {
				mOwnedItems.add(itemId);

				// If this is a subscription, then enable the "Edit
				// Subscriptions" button.
				for (CatalogEntry e : CATALOG) {
					if (e.sku.equals(itemId)
							&& e.managed.equals(Managed.SUBSCRIPTION)) {
						// TODO 
						// update subscription
						MainMagazineActivity.this.onSubscribe(e.sku);
					}
				}
			}
//			mCatalogAdapter.setOwnedItems(mOwnedItems);
			cloud.setIssuePurchiseState(mOwnedItems);
			mOwnedItemsCursor.requery();
			mIssueListGrid.invalidateViews();
		}

		@Override
		public void onRequestPurchaseResponse(RequestPurchase request,
				ResponseCode responseCode) {
			if (Consts.DEBUG) {
				Log.d(TAG, request.mProductId + ": " + responseCode);
			}
			if (responseCode == ResponseCode.RESULT_OK) {
				if (Consts.DEBUG) {
					Log.i(TAG, "purchase was successfully sent to server");
				}
			} else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
				if (Consts.DEBUG) {
					Log.i(TAG, "user canceled purchase");
				}
			} else {
				if (Consts.DEBUG) {
					Log.i(TAG, "purchase failed");
				}
			}
		}

		@Override
		public void onRestoreTransactionsResponse(RestoreTransactions request,
				ResponseCode responseCode) {
			if (responseCode == ResponseCode.RESULT_OK) {
				if (Consts.DEBUG) {
					Log.d(TAG, "completed RestoreTransactions request");
				}
				// Update the shared preferences so that we don't perform
				// a RestoreTransactions again.
				SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
				SharedPreferences.Editor edit = prefs.edit();
				edit.putBoolean(DB_INITIALIZED, true);
				edit.commit();
			} else {
				if (Consts.DEBUG) {
					Log.d(TAG, "RestoreTransactions error: " + responseCode);
				}
			}
		}
	}

	private static class CatalogEntry {
		public String sku;
//		public int nameId = -1;
		public Managed managed;
		public String name;

		public CatalogEntry(Context context, String sku, int nameId, Managed managed) {
			this.sku = sku;
			this.name = context.getString(nameId);
			this.managed = managed;
		}
		
		public CatalogEntry(String sku, String name, Managed managed){
			this.sku = sku;
			this.name = name;
			this.managed = managed;
		}
		
		public String getSku(){
			return sku;
		}
		
		public String getDescription(){
			return name;
		}
		
		public Managed getManagebility(){
			return managed;
		}
	}

	/** An array of product list entries for the products that can be purchased. */
	private static ArrayList<CatalogEntry> CATALOG;

	private String mItemName;
	private String mSku;
	private Managed mManagedType;
//	private CatalogAdapter mCatalogAdapter;

	private CloudHelper cloud;

	private DownloadManager mDownloadManager;

	private HashMap<Long, Long> mDownloadRequestsHashMap;

	private BroadcastReceiver mDownloadBroadcasReciever;

	private GridView mIssueListGrid;
	
	private GoogleAnalyticsTracker tracker;

	ArrayList<CatalogEntry> initCatalog(Context context) {

		if (cloud != null) {
			ArrayList<CatalogEntry> toReturn = new ArrayList<CatalogEntry>();
			ArrayList<Issue> a = cloud.getIssueList();
			ArrayList<Magazine> m = cloud.getMagazineList();
			for (int i = 0; i < m.size(); i++) 
				toReturn.add( new CatalogEntry(context, m.get(i).getSku(), R.string.subscription_yearly, Managed.SUBSCRIPTION));
			for(int i = 0; i < a.size(); i++){
				toReturn.add( new CatalogEntry(a.get(i).getSku(), a.get(i).getName(), Managed.MANAGED));
			}
			return toReturn;
		}
		throw new IllegalStateException("Cloud not initialized!");
	}

	public void onSubscribe(String sku) {
		ArrayList<Magazine> mags = cloud.getMagazineList();
		if(mags.size() <= 0)
			return;
		// handle magazine purchise
	}

	
	private GridView grid;
	private ArrayList<MagazineModel> magazine;
	public static final String BROADCAST_ACTION_IVALIDATE = "com.librelio.lib.service.broadcast.invalidate";
	private MagazineAdapter adapter;
	
	private void reloadMagazineData(ArrayList<MagazineModel> magazine){
		magazine.clear();
		DataBaseHelper dbhelp = new DataBaseHelper(this);
		SQLiteDatabase db = dbhelp.getReadableDatabase();
		Cursor c = db.rawQuery("select * from "+Magazines.TABLE_NAME, null);
		if(c.getCount()>0){
			c.moveToFirst();
			do{
				MagazineModel buf = new MagazineModel(c, this);
				magazine.add(buf);
			}  while(c.moveToNext());
		}
		c.close();
		db.close();
	}
	
	private BroadcastReceiver br;
	private BroadcastReceiver requestSubsAPIv2;
	private Timer update;
	private Intent intent;
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {	
		   if (requestCode == 1001) {    	
		      int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
		      String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
		      String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
		        
		      if (resultCode == RESULT_OK&purchaseData!=null) {
		         try {
		            JSONObject jo = new JSONObject(purchaseData);
		            String sku = jo.getString("productId");
		            Log.d(TAG,"You have bought the " + sku + ". Excellent choice,adventurer!");
		          }
		          catch (JSONException e) {
		             Log.d(TAG,"Failed to parse purchase data.");
		             e.printStackTrace();
		          }
		      }
		   }
		}
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"),
		//		mServiceConn, Context.BIND_AUTO_CREATE);
		
		
		
		/*
		tracker = GoogleAnalyticsTracker.getInstance();

		tracker.startNewSession(getResources().getString(R.string.GoogleAnalyticsCode), 300, this);
		tracker.trackPageView("/mainScreen/");
		
		cloud = new CloudHelper(this, this);
		*/
		setContentView(R.layout.issue_list_layout);
		
		magazine = new ArrayList<MagazineModel>();
		reloadMagazineData(magazine);
		//
		grid = (GridView)findViewById(R.id.issue_list_grid_view);
		adapter = new MagazineAdapter(magazine, this);
		grid.setAdapter(adapter);
		
		br = new BroadcastReceiver() {			
			@Override
			public void onReceive(Context context, Intent intent) {
				if(grid!=null){
					Log.d(TAG, "onReceive: grid was invalidate");
					reloadMagazineData(magazine);
					grid.invalidate();
					grid.invalidateViews();
				}
			}
		};
		IntentFilter filter = new IntentFilter(BROADCAST_ACTION_IVALIDATE);
		registerReceiver(br, filter);
		
		requestSubsAPIv2 = new BroadcastReceiver() {			
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(TAG,"onReceive");
				
				if (!mBillingService.requestPurchase(LibrelioApplication.SUBSCRIPTION_YEAR_KEY, Consts.ITEM_TYPE_SUBSCRIPTION, null)) {
	                // Note: mManagedType == Managed.SUBSCRIPTION
	                //showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
				}
			}
		};
		IntentFilter subsFilter = new IntentFilter(REQUEST_SUBS);
		registerReceiver(requestSubsAPIv2, subsFilter);
		
		startRegularUpdate();
		//
		
	
		mHandler = new Handler();
		mLibrelioPurchaseObserver = new LibrelioPurchaseObserver(mHandler);
		mBillingService = new BillingService();
		mBillingService.setContext(this);
		
		/*CATALOG = initCatalog(this);
		mDownloadManager = (DownloadManager) this
				.getSystemService(Activity.DOWNLOAD_SERVICE);

		mPurchaseDatabase = new PurchaseDatabase(this);
		setupWidgets();*/

		// Check if billing is supported.
		ResponseHandler.register(mLibrelioPurchaseObserver);
		/*if (!mBillingService.checkBillingSupported()) {
			showDialog(DIALOG_CANNOT_CONNECT_ID);
		}

		if (!mBillingService
				.checkBillingSupported(Consts.ITEM_TYPE_SUBSCRIPTION)) {
			showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
		}*/
	}

	private void startRegularUpdate(){
		long period = 1800000;
		update = new Timer();
		intent = new Intent(this, DownloadMagazineListService.class);
		TimerTask updateTask = new TimerTask() {
			@Override
			public void run() {
				startService(intent);
			}
		};
		update.schedule(updateTask, period, period);
	}
	
	private void stopRegularUpdate(){
		update.cancel();
	}
	/**
	 * Called when this activity becomes visible.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		ResponseHandler.register(mLibrelioPurchaseObserver);
		//initializeOwnedItems();
	}

	/**
	 * Called when this activity is no longer visible.
	 */
	@Override
	protected void onStop() {
		super.onStop();
		ResponseHandler.unregister(mLibrelioPurchaseObserver);
	}

	@Override
	protected void onDestroy() {
		/*super.onDestroy();
		mPurchaseDatabase.close();
		mBillingService.unbind();
		this.unregisterReceiver(mDownloadBroadcasReciever);
		for (Iterator<Long> mDownloadRequestsIterator = mDownloadRequestsHashMap
				.keySet().iterator(); mDownloadRequestsIterator.hasNext(); mDownloadManager
				.remove(mDownloadRequestsIterator.next()))
			;
		cloud.recycle();*/
		/*if (mServiceConn != null) {
		      unbindService(mServiceConn);
		   }*/
		stopRegularUpdate();
		unregisterReceiver(br);
		unregisterReceiver(requestSubsAPIv2);
		super.onDestroy();
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

	private Dialog createDialog(int titleId, int messageId) {
		String helpUrl = replaceLanguageAndRegion(getString(R.string.help_url));
		if (Consts.DEBUG) {
			Log.i(TAG, helpUrl);
		}
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

	/**
	 * Replaces the language and/or country of the device into the given string.
	 * The pattern "%lang%" will be replaced by the device's language code and
	 * the pattern "%region%" will be replaced with the device's country code.
	 * 
	 * @param str
	 *            the string to replace the language/country within
	 * @return a string containing the local language and region codes
	 */
	private String replaceLanguageAndRegion(String str) {
		// Substitute language and or region if present in string
		if (str.contains("%lang%") || str.contains("%region%")) {
			Locale locale = Locale.getDefault();
			str = str.replace("%lang%", locale.getLanguage().toLowerCase());
			str = str.replace("%region%", locale.getCountry().toLowerCase());
		}
		return str;
	}

	/**
	 * Sets up the UI.
	 */
	private void setupWidgets() {

		mIssueListGrid = (GridView) findViewById(R.id.issue_list_grid_view);

		if (cloud.getAllIssueCursor().getCount() > 0) {
			setGridViewAdapter(mIssueListGrid);

		} else {
			// Need to download issues for the first time
			onReloadIssues();
		}
		mDownloadRequestsHashMap = new HashMap<Long, Long>();
		mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		mDownloadBroadcasReciever = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
					long downloadId = intent.getLongExtra(
							DownloadManager.EXTRA_DOWNLOAD_ID, 0);
					Query query = new Query();
					query.setFilterById(downloadId);
					Cursor c = mDownloadManager.query(query);
					if (c.moveToFirst()) {
						int columnIndex = c
								.getColumnIndex(DownloadManager.COLUMN_STATUS);
						if (DownloadManager.STATUS_SUCCESSFUL == c
								.getInt(columnIndex)) {
							String uriString = c
									.getString(c
											.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
							Issue issue = cloud
									.getIssue(mDownloadRequestsHashMap
											.get(downloadId));
							issue.setState(issue.getState() | Issue.STATE_LOADED);
							issue.setIssue_path(uriString);
							cloud.updateIssue(issue);
							mIssueListGrid.invalidateViews();
							mDownloadRequestsHashMap.remove(downloadId);
						}
					}
				}
			}
		};
		this.registerReceiver(mDownloadBroadcasReciever, new IntentFilter(
				DownloadManager.ACTION_DOWNLOAD_COMPLETE));

		mOwnedItemsCursor = mPurchaseDatabase.queryAllPurchasedItems();
		startManagingCursor(mOwnedItemsCursor);
		String[] from = new String[] {
				PurchaseDatabase.PURCHASED_PRODUCT_ID_COL,
				PurchaseDatabase.PURCHASED_QUANTITY_COL };
	}

	// private void prependLogEntry(CharSequence cs) {
	// SpannableStringBuilder contents = new SpannableStringBuilder(cs);
	// contents.append('\n');
	// contents.append(mLogTextView.getText());
	// mLogTextView.setText(contents);
	// }
	//
	// private void logProductActivity(String product, String activity) {
	// SpannableStringBuilder contents = new SpannableStringBuilder();
	// contents.append(Html.fromHtml("<b>" + product + "</b>: "));
	// contents.append(activity);
	// prependLogEntry(contents);
	// }

	/**
	 * If the database has not been initialized, we send a RESTORE_TRANSACTIONS
	 * request to Android Market to get the list of purchased items for this
	 * user. This happens if the application has just been installed or the user
	 * wiped data. We do not want to do this on every startup, rather, we want
	 * to do only when the database needs to be initialized.
	 */
	private void restoreDatabase() {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		boolean initialized = prefs.getBoolean(DB_INITIALIZED, false);
		if (!initialized) {
			mBillingService.restoreTransactions();
			Toast.makeText(this, R.string.restoring_transactions,
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Creates a background thread that reads the database and initializes the
	 * set of owned items.
	 */
	private void initializeOwnedItems() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				doInitializeOwnedItems();
			}
		}).start();
	}

	/**
	 * Reads the set of purchased items from the database in a background thread
	 * and then adds those items to the set of owned items in the main UI
	 * thread.
	 */
	private void doInitializeOwnedItems() {
		Cursor cursor = mPurchaseDatabase.queryAllPurchasedItems();
		if (cursor == null) {
			return;
		}

		final Set<String> ownedItems = new HashSet<String>();
		try {
			int productIdCol = cursor
					.getColumnIndexOrThrow(PurchaseDatabase.PURCHASED_PRODUCT_ID_COL);
			while (cursor.moveToNext()) {
				String productId = cursor.getString(productIdCol);
				ownedItems.add(productId);
			}
		} finally {
			cursor.close();
		}

		// We will add the set of owned items in a new Runnable that runs on
		// the UI thread so that we don't need to synchronize access to
		// mOwnedItems.
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mOwnedItems.addAll(ownedItems);
//				mCatalogAdapter.setOwnedItems(mOwnedItems);
				cloud.setIssuePurchiseState(mOwnedItems);
				//TODO: update issueListAdapter to reflect purchised states.
			}
		});
	}

	/**
	 * List subscriptions for this package in Google Play
	 * 
	 * This allows users to unsubscribe from this apps subscriptions.
	 * 
	 * Subscriptions are listed on the Google Play app detail page, so this
	 * should only be called if subscriptions are known to be present.
	 */
	private void editSubscriptions() {
		// Get current package name
		String packageName = getPackageName();
		// Open app detail in Google Play
		Intent i = new Intent(Intent.ACTION_VIEW,
				Uri.parse("market://details?id=" + packageName));
		startActivity(i);
	}

	/**
	 * Displays the dialog used to edit the payload dialog.
	 */
	private void showPayloadEditDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		final View view = View.inflate(this, R.layout.edit_payload, null);
		final TextView payloadText = (TextView) view
				.findViewById(R.id.payload_text);
		if (mPayloadContents != null) {
			payloadText.setText(mPayloadContents);
		}

		dialog.setView(view);
		dialog.setPositiveButton(R.string.edit_payload_accept,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mPayloadContents = payloadText.getText().toString();
					}
				});
		dialog.setNegativeButton(R.string.edit_payload_clear,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (dialog != null) {
							mPayloadContents = null;
							dialog.cancel();
						}
					}
				});
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (dialog != null) {
					dialog.cancel();
				}
			}
		});
		dialog.show();
	}

	/**
	 * An adapter used for displaying a catalog of products. If a product is
	 * managed by Android Market and already purchased, then it will be
	 * "grayed-out" in the list and not selectable.
	 */
	private static class CatalogAdapter extends ArrayAdapter<String> {
		private CatalogEntry[] mCatalog;
		private Set<String> mOwnedItems = new HashSet<String>();
		private boolean mIsSubscriptionsSupported = false;

		public CatalogAdapter(Context context, CatalogEntry[] catalog) {
			super(context, android.R.layout.simple_spinner_item);
			mCatalog = catalog;
			for (CatalogEntry element : catalog) {
				add(element.getDescription());
			}
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}

		public void setOwnedItems(Set<String> ownedItems) {
			mOwnedItems = ownedItems;
			notifyDataSetChanged();
		}

		public void setSubscriptionsSupported(boolean supported) {
			mIsSubscriptionsSupported = supported;
		}

		@Override
		public boolean areAllItemsEnabled() {
			// Return false to have the adapter call isEnabled()
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			// If the item at the given list position is not purchasable,
			// then prevent the list item from being selected.
			CatalogEntry entry = mCatalog[position];
			if (entry.managed == Managed.MANAGED
					&& mOwnedItems.contains(entry.sku)) {
				return false;
			}
			if (entry.managed == Managed.SUBSCRIPTION
					&& !mIsSubscriptionsSupported) {
				return false;
			}
			return true;
		}

		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			// If the item at the given list position is not purchasable, then
			// "gray out" the list item.
			View view = super.getDropDownView(position, convertView, parent);
			view.setEnabled(isEnabled(position));
			return view;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.options_menu_reload:
			//onReloadIssues();
			Intent intent = new Intent(this, DownloadMagazineListService.class);
			startService(intent);
			reloadMagazineData(magazine);
			stopRegularUpdate();
			startRegularUpdate();
			return true;
		case R.id.options_menu_restore:
			restorePurchises();
			return true;
		case R.id.options_menu_subscribe:
			subscribeYear();
			return true;
		case R.id.options_menu_test: {
			Intent testIntent = new Intent(this, MuPDFActivity.class);
			startActivity(testIntent);
			return true;
		}
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void subscribeYear() {
		ArrayList<Magazine> magazines = cloud.getMagazineList();
		if(magazines.size() <= 0)
			return;
		mSku = magazines.get(0).getSku();
		if (!mBillingService.requestPurchase(mSku, Consts.ITEM_TYPE_SUBSCRIPTION, null)) {
	                // Note: mManagedType == Managed.SUBSCRIPTION
	                showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
		}
	}

	private void restorePurchises() {
	}

	public void onReloadIssues() {
		if (this.isOnline()) {
			Toast.makeText(this, R.string.searching_, Toast.LENGTH_SHORT)
					.show();
			try {
				cloud.reloadIssues();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
		}

	}

	@Override
	public void onReadClick(long issueId) {
		Issue issue = cloud.getIssue(issueId);
		if( (issue.getState() & Issue.STATE_LOADED) == 0)
			return;
		Uri uri = Uri.parse(issue.getIssue_path());
		Intent intent = new Intent(this, com.artifex.mupdf.MuPDFActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(uri);
		startActivity(intent);

	}

	@Override
	public void onDownloadClick(long issueId) {
		try {
			long enqueue = cloud.downloadIssue(issueId);
			this.mDownloadRequestsHashMap.put(enqueue, issueId);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mIssueListGrid.invalidateViews();
	}
	
	@Override
	public void onPurchiseClick(long issueId) {
		Issue issue = cloud.getIssue(issueId);
		mSku = issue.getSku();
		if (!mBillingService.requestPurchase(mSku, Consts.ITEM_TYPE_INAPP, null)) {
	                // Note: mManagedType == Managed.SUBSCRIPTION
	                showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
		}
	}
	
	@Override
	public void onDeleteClick(long issueId) {
		cloud.deleteIssue(issueId);
		this.mIssueListGrid.invalidateViews();
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		return cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isConnectedOrConnecting();
	}

	public void setGridViewAdapter(GridView g) {
		String[] from = { Ocean.ISSUE_COVER_PATH_KEY, Ocean.ISSUE_NAME_KEY,
				Ocean.ISSUE_STATE_KEY };
		int[] to = { R.id.issue_cover, R.id.issue_title, R.id.issue_button };
		mIssueListGrid = (GridView) findViewById(R.id.issue_list_grid_view);
		Cursor mAllIssuesCursor = cloud.getAllIssueCursor();
		g.setAdapter(new IssueListAdapter(this, R.layout.issue_item_unloaded,
				mAllIssuesCursor, from, to, cloud, this));
	}

	@Override
	public void onReloadIssuesFinished() {
		IssueListAdapter a = ((IssueListAdapter) mIssueListGrid.getAdapter());
		if (a == null) {
			setGridViewAdapter(mIssueListGrid);
		} else {
			a.notifyDataSetInvalidated();
			this.mIssueListGrid.invalidate();
		}
		// ((IssueListAdapter)mIssueListGrid.getAdapter()).notifyDataSetChanged();
		// mIssueListGrid.invalidateViews();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		MenuItem item = menu.getItem(0);
		item.setIcon(R.drawable.ic_menu_refresh);
		MenuItemCompat.setShowAsAction(menu.getItem(0),
				MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

}

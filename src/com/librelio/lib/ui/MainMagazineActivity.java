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

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.GridView;
import android.widget.Toast;

import com.librelio.base.BaseActivity;
import com.librelio.lib.LibrelioApplication;
import com.librelio.lib.adapter.MagazineAdapter;
import com.librelio.lib.model.MagazineModel;
import com.librelio.lib.service.DownloadMagazineListService;
import com.librelio.lib.storage.DataBaseHelper;
import com.librelio.lib.storage.Magazines;
import com.librelio.lib.utils.BillingService;
import com.librelio.lib.utils.Consts;
import com.librelio.lib.utils.ResponseHandler;
import com.niveales.wind.R;

/**
 * The main point for Librelio application
 */
public class MainMagazineActivity extends BaseActivity {
	/**
	 * The static
	 */
	private static final String TAG = "MainMagazineActivity";
	public static final String REQUEST_SUBS = "request_subs";
	public static final String UPDATE_PROGRESS_STOP = "updeta_pregress_stop";
	public static final String BROADCAST_ACTION_IVALIDATE = "com.librelio.lib.service.broadcast.invalidate";

	private static final int DIALOG_CANNOT_CONNECT_ID = 1;
	private static final int DIALOG_BILLING_NOT_SUPPORTED_ID = 2;
	private static final int DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID = 3;

	/**
	 * Receiver for magazines view refresh
	 */
	private BroadcastReceiver gridInvalidate = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(grid!=null){
				Log.d(TAG, "onReceive: grid was invalidate");
				reloadMagazineData(magazines);
				grid.invalidate();
				grid.invalidateViews();
			}
		}
	};
	/**
	 * The Purchase receivers
	 */
	private BroadcastReceiver subscriptionYear = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG,"onReceive subscription year");
			if (!billingService.requestPurchase(LibrelioApplication.SUBSCRIPTION_YEAR_KEY, Consts.ITEM_TYPE_SUBSCRIPTION, null)) {
				//Note: mManagedType == Managed.SUBSCRIPTION
				showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
			}
		}
	};
	private BroadcastReceiver subscriptionMonthly = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG,"onReceive subscription monthly");
			if (!billingService.requestPurchase(LibrelioApplication.SUBSCRIPTION_MONTHLY_KEY, Consts.ITEM_TYPE_SUBSCRIPTION, null)) {
				showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
			}
		}
	};

	private Timer updateTimer;
	private BillingService billingService;

	private GridView grid;
	private ArrayList<MagazineModel> magazines;
	private MagazineAdapter adapter;
	private LibrelioPurchaseObserver librelioPurchaseObserver;
	private Handler handler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.issue_list_layout);

		grid = (GridView)findViewById(R.id.issue_list_grid_view);

		magazines = new ArrayList<MagazineModel>();
		reloadMagazineData(magazines);

		adapter = new MagazineAdapter(magazines, this);
		grid.setAdapter(adapter);

		IntentFilter filter = new IntentFilter(BROADCAST_ACTION_IVALIDATE);
		IntentFilter subsFilter = new IntentFilter(REQUEST_SUBS);

		registerReceiver(gridInvalidate, filter);
		registerReceiver(subscriptionYear, subsFilter);
		registerReceiver(subscriptionMonthly, subsFilter);

		startRegularUpdate();

		billingService = new BillingService();
		billingService.setContext(this);
		handler = new Handler();
		librelioPurchaseObserver = new LibrelioPurchaseObserver(handler);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1001) {
//			int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
			String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
//			String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

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

	/**
	 * Called when this activity becomes visible.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		setProgressBarIndeterminateVisibility(false);
		IntentFilter filter = new IntentFilter(UPDATE_PROGRESS_STOP);
		registerReceiver(updateProgressStop, filter);
		ResponseHandler.register(librelioPurchaseObserver);
	}

	/**
	 * Called when this activity is no longer visible.
	 */
	@Override
	protected void onStop() {
		unregisterReceiver(updateProgressStop);
		ResponseHandler.unregister(librelioPurchaseObserver);
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		stopRegularUpdate();
		unregisterReceiver(subscriptionYear);
		unregisterReceiver(subscriptionMonthly);
		unregisterReceiver(gridInvalidate);
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


	/**
	 * An adapter used for displaying a catalog of products. If a product is
	 * managed by Android Market and already purchased, then it will be
	 * "grayed-out" in the list and not selectable.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {

		case R.id.options_menu_reload:
			if(LibrelioApplication.thereIsConnection(this)){
				Intent intent = new Intent(this, DownloadMagazineListService.class);
				startService(intent);
				setProgressBarIndeterminateVisibility(true);
				reloadMagazineData(magazines);
				stopRegularUpdate();
				startRegularUpdate();

			} else {
				Toast.makeText(this, getResources().getString(R.string.connection_failed),
						Toast.LENGTH_LONG).show();
			}
			return true;

		case R.id.options_menu_restore:
			restorePurchises();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		MenuItem item = menu.getItem(0);
		item.setIcon(R.drawable.ic_menu_refresh);
		MenuItemCompat.setShowAsAction(menu.getItem(0), MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

	private void reloadMagazineData(ArrayList<MagazineModel> magazine){
		magazine.clear();
		/**
		 * TODO delete after testing
		 */
		magazine.add(new MagazineModel(StartupActivity.TEST_FILE_NAME, "TEST", "test", "", this));
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

	private void startRegularUpdate(){
		long period = getUpdatePeriod();
		updateTimer = new Timer();
		final Intent intent = new Intent(this, DownloadMagazineListService.class);
		TimerTask updateTask = new TimerTask() {
			@Override
			public void run() {
				startService(intent);
			}
		};
		updateTimer.schedule(updateTask, period, period);
	}

	private void stopRegularUpdate(){
		updateTimer.cancel();
	}

	private Dialog createDialog(int titleId, int messageId) {
		String helpUrl = replaceLanguageAndRegion(getString(R.string.help_url));
		if (Consts.DEBUG) {
			Log.d(TAG, helpUrl);
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

	private void restorePurchises() {
	}

}

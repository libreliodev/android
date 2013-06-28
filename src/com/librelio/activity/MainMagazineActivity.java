/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.librelio.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.GridView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.librelio.adapter.MagazineAdapter;
import com.librelio.base.BaseActivity;
import com.librelio.model.Magazine;
import com.librelio.service.DownloadMagazineListService;
import com.librelio.storage.MagazineManager;
import com.niveales.wind.R;

/**
 * The main point for Librelio application
 * 
 * @author Nikolay Moskvin <moskvin@netcook.org>
 */
public class MainMagazineActivity extends BaseActivity {
	/**
	 * The static
	 */
	private static final String TAG = "MainMagazineActivity";
	public static final String REQUEST_SUBS = "request_subs";
	public static final String UPDATE_PROGRESS = "update_progress";
	public static final String BROADCAST_ACTION_IVALIDATE = "com.librelio.lib.service.broadcast.invalidate";
	private static final String START_FIRST_TIME = "START_FIRST_TIME";

	private static final int DIALOG_CANNOT_CONNECT_ID = 1;
	private static final int DIALOG_BILLING_NOT_SUPPORTED_ID = 2;
	private static final int DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID = 3;

	/**
	 * Receiver for magazines view refresh
	 */
	private BroadcastReceiver gridInvalidate;
	/**
	 * The Purchase receivers
	 */
	private BroadcastReceiver subscriptionYear;
	private BroadcastReceiver subscriptionMonthly;

	private Timer updateTimer;

	private GridView grid;
	private ArrayList<Magazine> magazines;
	private MagazineAdapter adapter;
	private Handler handler;
	private MagazineManager magazineManager;
	
	private boolean hasTestMagazine;
    private Timer timer;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.issue_list_layout);
		overridePendingTransition(R.anim.flip_right_in, R.anim.flip_left_out);
		
		hasTestMagazine = hasTestMagazine();

		magazineManager = new MagazineManager(this);

		grid = (GridView)findViewById(R.id.issue_list_grid_view);

		magazines = new ArrayList<Magazine>();
		reloadMagazineData(magazines);

		adapter = new MagazineAdapter(magazines, this, hasTestMagazine);
		grid.setAdapter(adapter);

		gridInvalidate = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (grid != null) {
					Log.d(TAG, "onReceive: grid was invalidate");
					reloadMagazineData(magazines);
					grid.invalidate();
					grid.invalidateViews();
				}
			}
		};

		IntentFilter filter = new IntentFilter(BROADCAST_ACTION_IVALIDATE);
		IntentFilter subsFilter = new IntentFilter(REQUEST_SUBS);

		registerReceiver(gridInvalidate, filter);
		registerReceiver(subscriptionYear, subsFilter);
		registerReceiver(subscriptionMonthly, subsFilter);

		startRegularUpdate();

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
		showProgress(false);
		IntentFilter filter = new IntentFilter(UPDATE_PROGRESS);
		registerReceiver(updateProgress, filter);
	}

	/**
	 * Called when this activity is no longer visible.
	 */
	@Override
	protected void onStop() {
		if (updateProgress != null) {
			unregisterReceiver(updateProgress);
		}
		super.onStop();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		EasyTracker.getTracker().sendView("Library/Magazines");
        int delay = 0; // delay for 1 sec.
        int period = 2000; // repeat every 10 sec.
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask()
        {
            public void run()
            {
                reloadMagazineData(magazines);
            }
        }, delay, period);
    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.cancel();
    }

    @Override
	protected void onDestroy() {
		stopRegularUpdate();
		if (subscriptionYear != null) {
			unregisterReceiver(subscriptionYear);
		}
		if (subscriptionMonthly != null) {
			unregisterReceiver(subscriptionMonthly);
		}
		if (gridInvalidate != null) {
			unregisterReceiver(gridInvalidate);
		}
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
			if (isOnline()) {
				if (!getPreferences().getBoolean(DownloadMagazineListService.ALREADY_RUNNING, false)) {
					Intent intent = new Intent(this, DownloadMagazineListService.class);
					startService(intent);
					showProgress(true);
					reloadMagazineData(magazines);
//					stopRegularUpdate();
//					startRegularUpdate();
				} else {
					Toast.makeText(this, getResources().getString(R.string.download_service_already_running), Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(this, getResources().getString(R.string.connection_failed), Toast.LENGTH_LONG).show();
			}
			return true;

		case R.id.options_menu_restore:
			restorePurchases();
			return true;
		case R.id.options_menu_send_log:
			onSendLog();
			return true;
		case R.id.options_menu_downloaded_magazines:
			Intent intent = new Intent(
					getBaseContext(), DownloadedMagazinesActivity.class);
			startActivity(intent);
			return true;
            case R.id.options_menu_subscribe:
                Intent subscribeIntent = new Intent(
                        getBaseContext(), BillingActivity.class);
                startActivity(subscribeIntent);
                return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
//		MenuItemCompat.setShowAsAction(menu.getItem(0), MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

	private void reloadMagazineData(final ArrayList<Magazine> magazines) {
        //run off main thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<Magazine> newMagazines = magazineManager.getMagazines(hasTestMagazine);
                magazines.clear();
                magazines.addAll(newMagazines);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (grid != null) {
                            grid.invalidate();
                            grid.invalidateViews();
                        }
                    }
                });
            }
        });
        thread.start();
	}

	private void startRegularUpdate(){
		long period = getUpdatePeriod();
		updateTimer = new Timer();
		TimerTask updateTask = new TimerTask() {
			@Override
			public void run() {
				boolean isFirst = getPreferences().getBoolean(START_FIRST_TIME, true);
				Intent intent = new Intent(getBaseContext(), DownloadMagazineListService.class);
				intent.putExtra(DownloadMagazineListService.USE_STATIC_MAGAZINES, isFirst);
				startService(intent);
				getPreferences().edit().putBoolean(START_FIRST_TIME, false).commit();
			}
		};
		long startTime = 0;
		if (magazineManager.getCount(Magazine.TABLE_MAGAZINES) > 0) {
			startTime = period;
		}
		updateTimer.schedule(updateTask, startTime, period);
	}

	private void stopRegularUpdate(){
		updateTimer.cancel();
	}

	private Dialog createDialog(int titleId, int messageId) {
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
							public void onClick(DialogInterface dialog, int which) {
								Intent intent = new Intent(Intent.ACTION_VIEW, helpUri);
								startActivity(intent);
							}
						});
		return builder.create();
	}

	private void restorePurchases() {
	}

	private void showProgress(boolean progress) {
		setProgressBarIndeterminateVisibility(progress);
	}
}

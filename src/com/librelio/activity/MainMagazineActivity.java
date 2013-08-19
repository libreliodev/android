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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.GridView;
import com.google.analytics.tracking.android.EasyTracker;
import com.librelio.adapter.MagazineAdapter;
import com.librelio.base.BaseActivity;
import com.librelio.event.InvalidateGridViewEvent;
import com.librelio.event.LoadPlistEvent;
import com.librelio.event.UpdateMagazinesEvent;
import com.librelio.loader.PlistParserLoader;
import com.librelio.model.DictItem;
import com.librelio.utils.PlistDownloader;
import com.niveales.wind.R;
import de.greenrobot.event.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * The main point for Librelio application
 * 
 * @author Nikolay Moskvin <moskvin@netcook.org>
 */
public class MainMagazineActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<ArrayList<DictItem>> {
	/**
	 * The static
	 */
	private static final String TAG = "MainMagazineActivity";
	public static final String REQUEST_SUBS = "request_subs";
	private static final String START_FIRST_TIME = "START_FIRST_TIME";

	private static final int DIALOG_CANNOT_CONNECT_ID = 1;
	private static final int DIALOG_BILLING_NOT_SUPPORTED_ID = 2;
	private static final int DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID = 3;
    private static final int PLIST_PARSER_LOADER = 0;

    private static final String PLIST_NAME_EXTRA = "plist_name";

    /**
	 * The Purchase receivers
	 */
	private BroadcastReceiver subscriptionYear;
	private BroadcastReceiver subscriptionMonthly;

	private GridView grid;
	private ArrayList<DictItem> magazines;
	private MagazineAdapter adapter;

	private boolean hasTestMagazine;

    private String plistName;

    private Runnable loadPlistTask = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getLoaderManager().restartLoader(PLIST_PARSER_LOADER, null, MainMagazineActivity.this);
                }
            });
        }
    };

    private Handler handler = new Handler();

    public static Intent getIntent(Context context, String plistName) {
        Intent intent = new Intent(context, MainMagazineActivity.class);
        intent.putExtra(PLIST_NAME_EXTRA, plistName);
        return intent;
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.issue_list_layout);
		overridePendingTransition(R.anim.flip_right_in, R.anim.flip_left_out);

        plistName = getIntent().getStringExtra(PLIST_NAME_EXTRA);
        if (!plistName.equals(getString(R.string.root_view))) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

		hasTestMagazine = hasTestMagazine();

		grid = (GridView)findViewById(R.id.issue_list_grid_view);

		magazines = new ArrayList<DictItem>();

		adapter = new MagazineAdapter(magazines, this, hasTestMagazine);
		grid.setAdapter(adapter);

		IntentFilter subsFilter = new IntentFilter(REQUEST_SUBS);

		registerReceiver(subscriptionYear, subsFilter);
		registerReceiver(subscriptionMonthly, subsFilter);

        getLoaderManager().initLoader(PLIST_PARSER_LOADER, null, this);

	}

    public void onEventMainThread(UpdateMagazinesEvent event) {
        if (event.getMagazines() != null) {
            magazines.clear();
            magazines.addAll(event.getMagazines());
        }
        reloadGrid();
    }

    public void onEventMainThread(InvalidateGridViewEvent event) {
            reloadGrid();
    }

    public void onEvent(LoadPlistEvent event) {
        startLoadPlistTask(0);
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
	}

	@Override
	protected void onResume() {
		super.onResume();
		EasyTracker.getTracker().sendView("Library/Magazines");
        startLoadPlistTask(0);
        PlistDownloader.doLoad(this, plistName, false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(loadPlistTask);
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
            case android.R.id.home:
                finish();
		case R.id.options_menu_reload:
            // force a redownload of the plist
            PlistDownloader.doLoad(this, plistName, true);
			return true;
		case R.id.options_menu_restore:
			restorePurchases();
			return true;
		case R.id.options_menu_send_log:
//			onSendLog();
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
		return true;
	}

	private void reloadGrid() {
        grid.invalidate();
        grid.invalidateViews();
	}

//	private void startRegularUpdate(){
//		long period = getUpdatePeriod();
//		updateTimer = new Timer();
//		TimerTask updateTask = new TimerTask() {
//			@Override
//			public void run() {
//				boolean isFirst = getPreferences().getBoolean(START_FIRST_TIME, true);
////				Intent intent = new Intent(getBaseContext(), DownloadMagazineListService.class);
////				intent.putExtra(DownloadMagazineListService.USE_STATIC_MAGAZINES, isFirst);
////				startService(intent);
//				getPreferences().edit().putBoolean(START_FIRST_TIME, false).commit();
//			}
//		};
//		long startTime = 0;
////		if (magazineManager.getCount(Magazine.TABLE_MAGAZINES) > 0) {
////			startTime = period;
////		}
//		updateTimer.schedule(updateTask, startTime, period);
//	}
//
//	private void stopRegularUpdate(){
////		updateTimer.cancel();
//	}

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

    @Override
    public Loader<ArrayList<DictItem>> onCreateLoader(int id, Bundle args) {
//        return new PlistParserLoader(getApplicationContext(), args.getString(PLIST_NAME));

        return new PlistParserLoader(getApplicationContext(), plistName, hasTestMagazine());
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<DictItem>> loader, ArrayList<DictItem> data) {
//        magazines.clear();
//        if (data != null) {
//            magazines.addAll(data);
//        }
        EventBus.getDefault().post(new InvalidateGridViewEvent());
        startLoadPlistTask(2000);
    }

    private void startLoadPlistTask(int delay) {
        handler.removeCallbacks(loadPlistTask);
        handler.postDelayed(loadPlistTask, delay);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<DictItem>> loader) {
        magazines.clear();
        EventBus.getDefault().post(new InvalidateGridViewEvent());
    }
}

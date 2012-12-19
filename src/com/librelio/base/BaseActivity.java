package com.librelio.base;

import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Environment;
import android.os.Handler;

import com.librelio.lib.utils.BillingService.RequestPurchase;
import com.librelio.lib.utils.BillingService.RestoreTransactions;
import com.librelio.lib.utils.Consts.PurchaseState;
import com.librelio.lib.utils.Consts.ResponseCode;
import com.librelio.lib.utils.PurchaseObserver;

abstract public class BaseActivity extends Activity implements IBaseContext{
	public static final String BROADCAST_ACTION = "com.librelio.lib.service.broadcast";

	/**
	 * The receiver for stop progress in action bar
	 */
	protected BroadcastReceiver updateProgressStop = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			setProgressBarIndeterminateVisibility(false);
		}
	};
	
	/**
	 * A {@link PurchaseObserver} is used to get callbacks when Android Market
	 * sends messages to this application so that we can update the UI.
	 */
	public class LibrelioPurchaseObserver extends PurchaseObserver {
		public LibrelioPurchaseObserver(Handler handler) {
			super(BaseActivity.this, handler);
		}

		@Override
		public void onBillingSupported(boolean supported, String type) {
			/*if (Consts.DEBUG) {
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
			}*/
		}

		@Override
		public void onPurchaseStateChange(PurchaseState purchaseState,
				String itemId, int quantity, long purchaseTime,
				String developerPayload) {
			/*if (Consts.DEBUG) {
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
			mIssueListGrid.invalidateViews();*/
		}

		@Override
		public void onRequestPurchaseResponse(RequestPurchase request,
				ResponseCode responseCode) {
			/*if (Consts.DEBUG) {
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
			}*/
		}

		@Override
		public void onRestoreTransactionsResponse(RestoreTransactions request,
				ResponseCode responseCode) {
			/*if (responseCode == ResponseCode.RESULT_OK) {
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
			}*/
		}
	}

	@Override
	public String getInternalPath() {
		return getDir("librelio", MODE_PRIVATE).getAbsolutePath();
	}

	@Override
	public String getExternalPath() {
		return Environment.getExternalStorageDirectory() + "/librelio/";
	}
	@Override
	public String getStoragePath(){
		return getInternalPath();
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
	protected String replaceLanguageAndRegion(String str) {
		// Substitute language and or region if present in string
		if (str.contains("%lang%") || str.contains("%region%")) {
			Locale locale = Locale.getDefault();
			str = str.replace("%lang%", locale.getLanguage().toLowerCase());
			str = str.replace("%region%", locale.getCountry().toLowerCase());
		}
		return str;
	}
	
	protected int getUpdatePeriod() {
		return 1800000;
	}
}

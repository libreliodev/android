package com.librelio.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import com.librelio.base.BaseActivity;
import com.niveales.wind.R;

abstract public class GooglePlayActivity extends BaseActivity {
	/**
	 * The developer payload that is sent with subsequent purchase requests.
	 */
	private String payloadContents = null;

	/**
	 * List subscriptions for this package in Google Play
	 * 
	 * This allows users to unsubscribe from this apps subscriptions.
	 * 
	 * Subscriptions are listed on the Google Play app detail page, so this
	 * should only be called if subscriptions are known to be present.
	 */
	protected void editSubscriptions() {
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
	protected void showPayloadEditDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		final View view = View.inflate(this, R.layout.edit_payload, null);
		final TextView payloadText = (TextView) view
				.findViewById(R.id.payload_text);
		if (payloadContents != null) {
			payloadText.setText(payloadContents);
		}

		dialog.setView(view);
		dialog.setPositiveButton(R.string.edit_payload_accept,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						payloadContents = payloadText.getText().toString();
					}
				});
		dialog.setNegativeButton(R.string.edit_payload_clear,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (dialog != null) {
							payloadContents = null;
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

}

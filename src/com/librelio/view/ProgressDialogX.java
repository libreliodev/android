package com.librelio.view;

import android.app.ProgressDialog;
import android.content.Context;

public class ProgressDialogX extends ProgressDialog {
	private boolean cancelled = false;

	public ProgressDialogX(Context context) {
		super(context);
	}

	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void cancel() {
		cancelled = true;
		super.cancel();
	}
}
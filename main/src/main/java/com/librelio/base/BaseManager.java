package com.librelio.base;

import android.content.Context;

public class BaseManager {
	private Context context;

	public BaseManager(Context context) {
		this.context = context;
	}

	public Context getContext() {
		return context;
	}

}

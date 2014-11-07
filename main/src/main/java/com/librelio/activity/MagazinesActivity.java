package com.librelio.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.librelio.base.BaseActivity;
import com.librelio.fragments.PlistGridFragment;

public class MagazinesActivity extends BaseActivity {

	private static final String PLIST_NAME_EXTRA = "plist_name";

	public static Intent getIntent(Context context, String plistName) {
		Intent intent = new Intent(context, MagazinesActivity.class);
		intent.putExtra(PLIST_NAME_EXTRA, plistName);
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState != null) {
			return;
		}

		PlistGridFragment f = PlistGridFragment.newInstance(getIntent()
				.getStringExtra(PLIST_NAME_EXTRA));

		getSupportFragmentManager().beginTransaction()
				.add(android.R.id.content, f).commit();
	}

}

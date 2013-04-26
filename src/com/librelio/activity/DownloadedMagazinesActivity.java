package com.librelio.activity;

import java.util.List;

import android.os.Bundle;
import android.view.MenuItem;

import com.librelio.base.BaseActivity;
import com.librelio.model.Magazine;
import com.librelio.storage.MagazineManager;
import com.librelio.view.DownloadedMagazinesListView;
import com.niveales.wind.R;

public class DownloadedMagazinesActivity extends BaseActivity {

	private MagazineManager magazineManager;

	private DownloadedMagazinesListView downloadsList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		setContentView(R.layout.activity_downloaded_magazines);

		magazineManager = new MagazineManager(this);

		downloadsList = (DownloadedMagazinesListView) findViewById(R.id.activity_downloaded_magazines_list);

		List<Magazine> downloads = magazineManager.getMagazines(false,
				Magazine.TABLE_DOWNLOADED_MAGAZINES);

		downloadsList.setMagazines(downloads);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
}

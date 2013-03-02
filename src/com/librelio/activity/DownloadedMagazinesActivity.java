package com.librelio.activity;

import java.util.List;

import android.os.Bundle;

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
		
		setContentView(R.layout.activity_downloaded_magazines);
		
		magazineManager = new MagazineManager(this);

		downloadsList = (DownloadedMagazinesListView) findViewById(R.id.activity_downloaded_magazines_list);
		
		List<Magazine> downloads = magazineManager.getMagazines(false, Magazine.TABLE_DOWNLOADED_MAGAZINES);
		
		downloadsList.setMagazines(downloads);
	}
	
	
	
}

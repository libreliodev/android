/**
 * 
 */
package com.librelio.activity;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.artifex.mupdf.MuPDFPageView;
import com.librelio.adapter.SlideShowAdapter;
import com.librelio.base.BaseActivity;
import com.librelio.view.SimpleGallery;
import com.niveales.wind.R;

/**
 * @author Dmitry Valetin 
 * TODO: @moskvin Could you replace this class to another class with better way (like as ImagePager)
 */
public class SlideShowActivity extends BaseActivity {
	private SimpleGallery slideshowGallery;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.sideshow_activity_layout);
		slideshowGallery = (SimpleGallery) findViewById(R.id.slide_show_gallery);

		String path = getIntent().getExtras().getString(MuPDFPageView.LINK_URI_KEY);
		String base = getIntent().getExtras().getString(MuPDFPageView.PATH_KEY);
		String fullPath = base + Uri.parse(path).getPath();
		SlideShowAdapter adapter = new SlideShowAdapter(this, fullPath);

		slideshowGallery.setAdapter(adapter);
		slideshowGallery.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pId) {
				SlideShowActivity.this.finish();
			}
		});
	}
}

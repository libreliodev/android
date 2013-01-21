/**
 * 
 */
package com.librelio.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.artifex.mupdf.LinkInfo;
import com.artifex.mupdf.MediaHolder;
import com.artifex.mupdf.MuPDFPageView;
import com.librelio.base.BaseActivity;
import com.librelio.view.SimpleGallery;
import com.niveales.wind.R;


/**
 * @author Mike Osipov
 */
public class SlideShowActivity extends BaseActivity {
	private SimpleGallery slideshowGallery;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.sideshow_activity_layout);
		LinearLayout frame = (LinearLayout)findViewById(R.id.slide_show_full);
		
		String path = getIntent().getExtras().getString(MuPDFPageView.PATH_KEY);
		String uri = getIntent().getExtras().getString(MuPDFPageView.LINK_URI_KEY);
		LinkInfo link = new LinkInfo(0, 0, 100, 100, 0);
		link.uri = uri;
		MediaHolder mh = new MediaHolder(this, link, path,true);
		mh.setVisibility(View.VISIBLE);
		frame.addView(mh);
	}
}

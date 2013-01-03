/**
 * 
 */
package com.librelio.lib.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.artifex.mupdf.LinkInfo;
import com.artifex.mupdf.MediaHolder;
import com.artifex.mupdf.MuPDFPageView;
import com.artifex.mupdf.SimpleGallery;
import com.librelio.base.BaseActivity;
import com.niveales.wind.R;

/**
 * @author Dmitry Valetin
 * TODO: @Mike Is it used? Please delete if not!
 */
public class SlideShowActivity extends BaseActivity {
	private SimpleGallery mSlideshowGallery;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sideshow_activity_layout);
		LinearLayout frame = (LinearLayout)findViewById(R.id.slide_show_full);
		
		String path = getIntent().getExtras().getString(MuPDFPageView.PATH_KEY);
		String uri = getIntent().getExtras().getString(MuPDFPageView.LINK_URI_KEY);
		LinkInfo link = new LinkInfo(0, 0, 100, 100, 0);
		link.uri = uri;
		MediaHolder mh = new MediaHolder(this, link, path,false);
		mh.setVisibility(View.VISIBLE);
		frame.addView(mh);
		
		/*setContentView(R.layout.sideshow_activity_layout);
		mSlideshowGallery = (SimpleGallery) findViewById(R.id.SlideshowGallery);

		String path = getIntent().getExtras().getString("path");


		SlideshowAdapter adapter = new SlideshowAdapter(this, path);

		mSlideshowGallery.setAdapter(adapter);
		mSlideshowGallery.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> pParent, View pView,
					int pPosition, long pId) {
				// TODO Auto-generated method stub
				SlideShowActivity.this.finish();
			}
		});*/
	}
}

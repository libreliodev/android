/**
 * 
 */
package com.librelio.activity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;

import com.artifex.mupdf.MediaHolder;
import com.librelio.base.BaseActivity;
import com.librelio.view.ImagePager;
import com.niveales.wind.R;


/**
 * @author Mike Osipov
 */
public class SlideShowActivity extends BaseActivity {
	private static final String TAG = "SlideShowActivity";
	
	private ImagePager imagePager;
	private Handler autoPlayHandler;
	
	private int autoPlayDelay;
	private boolean transition = true;
	private boolean autoPlay;
	private int bgColor;
	private String fullPath;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.sideshow_activity_layout);
		LinearLayout frame = (LinearLayout)findViewById(R.id.slide_show_full);
		
		autoPlayDelay = getIntent().getExtras().getInt(MediaHolder.PLAY_DELAY_KEY);
		transition = getIntent().getExtras().getBoolean(MediaHolder.TRANSITION_KEY);
		autoPlay = getIntent().getExtras().getBoolean(MediaHolder.AUTO_PLAY_KEY);
		bgColor = getIntent().getExtras().getInt(MediaHolder.BG_COLOR_KEY);
		fullPath = getIntent().getExtras().getString(MediaHolder.FULL_PATH_KEY);

		imagePager = new ImagePager(this, fullPath, transition, 100);
		imagePager.post(new Runnable() {
			@Override
			public void run() {
				imagePager.setViewWidth(imagePager.getWidth());
			}
		});
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.gravity = Gravity.CENTER;
		imagePager.setLayoutParams(lp);
		
		if(autoPlay) {
			autoPlayHandler = new Handler();
			autoPlayHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					Log.d(TAG, "autoPlayHandler start");
					imagePager.setCurrentPosition(imagePager.getCurrentPosition() + 1, transition);
					autoPlayHandler.postDelayed(this, autoPlayDelay);
				}}, autoPlayDelay);
		}
		
		imagePager.setBackgroundColor(bgColor);
		
		frame.addView(imagePager);
	}
}

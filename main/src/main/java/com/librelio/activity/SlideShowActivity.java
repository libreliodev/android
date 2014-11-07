package com.librelio.activity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;

import com.artifex.mupdfdemo.MediaHolder;
import com.librelio.base.BaseActivity;
import com.librelio.view.ImageLayout;
import com.niveales.wind.R;

/**
 * @author Mike Osipov
 */
public class SlideShowActivity extends BaseActivity {
	private static final String TAG = "SlideShowActivity";
	
	private ImageLayout imageLayout;
	private Handler autoPlayHandler;
	
	private String fullPath;
	private int autoPlayDelay;
	private int bgColor;
	private int initialSlidePosition = 0;
	private boolean transition = true;
	private boolean autoPlay;
	
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
		initialSlidePosition = getIntent().getExtras().getInt(MediaHolder.INITIAL_SLIDE_POSITION);

		imageLayout = new ImageLayout(this, fullPath, transition);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.gravity = Gravity.CENTER;
		imageLayout.setLayoutParams(lp);

		imageLayout.post(new Runnable() {
			@Override
			public void run() {
				imageLayout.setCurrentPosition(initialSlidePosition, false);
			}
		});
		
		if(autoPlay) {
			autoPlayHandler = new Handler();
			autoPlayHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					Log.d(TAG, "autoPlayHandler start");
					imageLayout.setCurrentPosition(imageLayout.getCurrentPosition() + 1, transition);
					autoPlayHandler.postDelayed(this, autoPlayDelay);
				}}, autoPlayDelay);
		}
		
		imageLayout.setBackgroundColor(bgColor);
		
		frame.addView(imageLayout);
	}
}

/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.librelio.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import com.artifex.mupdf.MediaHolder;
import com.librelio.base.BaseActivity;
import com.librelio.task.CreateTempVideoTask;
import com.niveales.wind.R;

/**
 * The video player screen
 * 
 * @author Nikolay Moskvin <moskvin@netcook.org>
 * 
 */
public class VideoActivity extends BaseActivity {
	private static final String TAG = "VideoActivity";

	private MediaController mc;
	private VideoView video;
	private String path;
	private int position;
	private boolean rotationWasDisabled = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.video_activity_layout);
		path = getIntent().getExtras().getString(MediaHolder.VIDEO_PATH_KEY);
		if(getIntent().getExtras().containsKey(MediaHolder.PLAYBACK_POSITION_KEY)){
			position = getIntent().getExtras().getInt(MediaHolder.PLAYBACK_POSITION_KEY);
		} else {
			position = 0;
		}
		video = (VideoView)findViewById(R.id.video_frame);
		video.setVideoPath(path);
		mc = new MediaController(getContext());
		video.setMediaController(mc);
		mc.setAnchorView(video);
		video.requestFocus();
		video.post(new Runnable() {			
			@Override
			public void run() {
				video.start();
				video.seekTo(position);
				mc.postDelayed(new Runnable() {
					@Override
					public void run() {
						mc.show(4000);
					}
				}, 500);
			}
		});
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onBackPressed() {
		if(video!=null){
			video.stopPlayback();
		}
		finish();
		super.onBackPressed();
	}

	private Context getContext(){
		return this;
	}

	@Override
	protected void onResume() {
		int rotationEnable = android.provider.Settings.System.getInt(
				getContentResolver(), android.provider.Settings.System.ACCELEROMETER_ROTATION, 1);
		if(rotationEnable == 0){
			rotationWasDisabled = true;
		} else {
			enableRotation(false);
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		if(!rotationWasDisabled){
			enableRotation(true);
		}
		super.onPause();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	}
}

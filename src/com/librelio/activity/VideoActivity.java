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

	private VideoView video;
	private String uriString;
	private String basePath;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG,"onCreate");
		uriString = getIntent().getExtras().getString(MediaHolder.URI_STRING_KEY);
		basePath = getIntent().getExtras().getString(MediaHolder.BASE_PATH_KEY);

		new CreateTempVideoTask(getVideoTempPath(), basePath) {
			@Override
			protected void onPreExecute() {
				setContentView(R.layout.wait_bar);
			};

			@Override
			protected void onPostExecute(String videoPath) {
				if (isCancelled()) {
					return;
				}
				setContentView(R.layout.video_activity_layout);
				video = (VideoView)findViewById(R.id.video_frame);
				video.setVideoPath(videoPath);
				video.post(new Runnable() {
					@Override
					public void run() {
						PlayStopController mc = new PlayStopController(getContext(),video);
						
						mc.setAnchorView(video);
						mc.setMediaPlayer(video);
						
						video.setMediaController(mc);
						mc.show(4000);
					}
				});
				video.requestFocus();
				video.start();
			}
		}.execute(uriString);
		
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

	private class PlayStopController extends MediaController {

		public PlayStopController(Context context, View anchor) {
			super(context);
			super.setAnchorView(anchor);
		}

		@Override
		public void setAnchorView(View view) {
			// TODO Auto-generated method stub
			//super.setAnchorView(view);
		}
	}
}

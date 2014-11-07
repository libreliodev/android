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

import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager.BadTokenException;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;

import com.artifex.mupdfdemo.MediaHolder;
import com.niveales.wind.R;

import java.io.File;
import java.io.FileInputStream;

/**
 * The video player screen
 * 
 * @author Nikolay Moskvin <moskvin@netcook.org>
 * @author Mike Osipov
 * 
 */
public class VideoActivity extends AbstractLockRotationActivity implements MediaPlayerControl{
	private static final String TAG = "VideoActivity";

	private SurfaceView video;
	private MediaPlayer mMediaPlayer;
	private MediaController mc;
	private SurfaceHolder holder;
	
	private String path;
	private int position;
	private ProgressDialog dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_video);
		onWait();
		path = getIntent().getExtras().getString(MediaHolder.VIDEO_PATH_KEY);
		if(getIntent().getExtras().containsKey(MediaHolder.PLAYBACK_POSITION_KEY)){
			position = getIntent().getExtras().getInt(MediaHolder.PLAYBACK_POSITION_KEY);
		} else {
			position = 0;
		}

		mMediaPlayer = new MediaPlayer();
		mc = new MediaController(this);
		video = (SurfaceView)findViewById(R.id.surface_frame);
		video.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mc.show();
				return false;
			}
		});
		holder = video.getHolder();
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		holder.setKeepScreenOn(true);
		holder.addCallback(new SurfaceHolder.Callback() {
	        @Override
	        public void surfaceCreated(SurfaceHolder mHolder) {
	            initMediaPlayer(mHolder);
	        }
	        @Override
	        public void surfaceDestroyed(SurfaceHolder holder) {}
	        @Override
	        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }
	    });
		
		super.onCreate(savedInstanceState);
	}
	
	private void initMediaPlayer(SurfaceHolder sHolder){
		Log.d(TAG, "Play video on full, path: " + path);
		File videoFile = new File(path);
        FileInputStream fis;
		try {
			fis = new FileInputStream(videoFile);
	        mMediaPlayer.setDataSource(fis.getFD());
	        mMediaPlayer.setDisplay(sHolder);
	        
	        mMediaPlayer.prepareAsync();
	        
	        mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mp) {
					onCancel();
					mMediaPlayer.start();
					setSurfaceViewScale();
					mMediaPlayer.seekTo(position);
					mc.setMediaPlayer(getMediaPlayerControl());
					mc.setAnchorView(video);
					
					Handler handler = new Handler();
			        handler.post(new Runnable() {
			            public void run() {
			                mc.setEnabled(true);
			                try{
			                	mc.show();
			                } catch (BadTokenException e) {
								Log.w(TAG,"Can't show media controller, activity may not running yet.",e);
							}
			            }
			        });
				}
			});
	        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		} catch (Exception e) {
			Log.e(TAG,"Problem with input stream!",e);
		}
	}
	
	private void setSurfaceViewScale(){
		
		int width = video.getWidth();
		int height = video.getHeight();
		float boxWidth = width;
		float boxHeight = height;

		float videoWidth = mMediaPlayer.getVideoWidth();
		float videoHeight = mMediaPlayer.getVideoHeight();

		float wr = boxWidth / videoWidth;
		float hr = boxHeight / videoHeight;
		float ar = videoWidth / videoHeight;

		if (wr > hr)
		    width = (int) (boxHeight * ar);
		else
		    height = (int) (boxWidth / ar);
		
		holder.setFixedSize(width, height);
	}
	
	@Override
	public void onBackPressed() {
		if(mMediaPlayer!=null){
			mMediaPlayer.release();
		}
		finish();
		super.onBackPressed();
	}

	private Context getContext(){
		return this;
	}

	private MediaPlayerControl getMediaPlayerControl(){
		return this;
	}
	
	public void onWait() {
		dialog = new ProgressDialog(this);
        dialog.setMessage(getResources().getString(R.string.loading));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();
	}

	public void onCancel() {
		if(dialog!=null){
			dialog.cancel();
			dialog = null;
		}
	}

	@Override
	protected void onDestroy() {
		if(mMediaPlayer!=null){
			mMediaPlayer.release();
		}
		super.onDestroy();
	}
	
	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return false;
	}

	@Override
	public boolean canSeekForward() {
		return false;
	}

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
	public int getBufferPercentage() {
		return 0;
	}

	@Override
	public int getCurrentPosition() {
		return mMediaPlayer.getCurrentPosition();
	}

	@Override
	public int getDuration() {
		return mMediaPlayer.getDuration();
	}

	@Override
	public boolean isPlaying() {
		return mMediaPlayer.isPlaying();
	}

	@Override
	public void pause() {
		mMediaPlayer.pause();
	}		

	@Override
	public void seekTo(int pos) {
		mMediaPlayer.seekTo(pos);
	}

	@Override
	public void start() {
		mMediaPlayer.start();
	}
}

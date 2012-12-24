package com.librelio.lib.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;

import com.artifex.mupdf.MediaHolder;
import com.librelio.base.BaseActivity;
import com.niveales.wind.R;

public class VideoActivity extends BaseActivity{
	private static final String TAG = "VideoActivity";
	
	
	private VideoView video;
	private String temp;
	private String uriString;
	private String basePath;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG,"onCreate");
		uriString = getIntent().getExtras().getString(MediaHolder.URI_STRING_KEY);
		basePath = getIntent().getExtras().getString(MediaHolder.BASE_PATH_KEY);
		
		
		String local = "http://localhost/";
		int startIdx = local.length();
		int finIdx = uriString.length();
		if(uriString.contains("?")){
			finIdx = uriString.indexOf("?");
		}
		String assetsFile = uriString.substring(startIdx, finIdx);
		String videoPath = basePath+"/"+assetsFile;
				
		setContentView(R.layout.video_activity_layout);
		video = (VideoView)findViewById(R.id.video_frame);
		video.setVideoPath(videoPath);
		video.setMediaController(new MediaController(getContext()));
		video.requestFocus();
		video.start();
		
		new AsyncTask<Void, Void, Void>(){
			String temp;
			protected void onPreExecute() {
				setContentView(R.layout.wait_bar);
			};
			@Override
			protected Void doInBackground(Void... params) {
				createTempVideoFile(uriString,basePath, getTempPath());
				return null;
			}
			@Override
			protected void onPostExecute(Void result) {
				setContentView(R.layout.video_activity_layout);
				video = (VideoView)findViewById(R.id.video_frame);
				video.setVideoPath(getTempPath());
				video.setMediaController(new MediaController(getContext()));
				video.requestFocus();
				video.start();
			}
		}.execute();
		
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
	
	public static void createTempVideoFile(String uriString,String basePath,String temp){
		String local = "http://localhost/";
		int startIdx = local.length();
		int finIdx = uriString.length();
		if(uriString.contains("?")){
			finIdx = uriString.indexOf("?");
		}
		String assetsFile = uriString.substring(startIdx, finIdx);
		String videoPath = basePath+"/"+assetsFile;
		File video = new File(videoPath);
		File tmp = new File(temp);
		//
		try {
			InputStream in  = new FileInputStream(video);
			OutputStream out = new FileOutputStream(tmp);
			byte[] buf = new byte[1024];
			int len;
			  while ((len = in.read(buf)) > 0){
				  out.write(buf, 0, len);
			  }
			  in.close();
			  out.close();
			  System.out.println("File copied.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public static String getTempPath(){
		return "/mnt/sdcard/librelio/tmp.mp4";
	}
}

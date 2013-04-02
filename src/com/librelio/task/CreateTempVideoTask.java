package com.librelio.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.AsyncTask;
import android.util.Log;

public class CreateTempVideoTask extends AsyncTask<String, Void, String> {
	private static final String TAG = "CreateTempVideoTask";

	public static final String IOEXEPTION_CODE = "io_exeption_code"; 
	
	private final String videoTempPath;
	private final String basePath;
	
	

	public CreateTempVideoTask(String videoTempPath, String basePath) {
		this.videoTempPath = videoTempPath;
		this.basePath = basePath;
	}
		
//	public CreateTempVideoTask(String videoTempPath) {
//		this(videoTempPath, null);
//	}

	@Override
	protected String doInBackground(String... videoPaths) {
		if (null == basePath) {
			return createTempVideoFile(videoPaths[0]);
		} else {
			return createTempVideoFile(getUrlFromLocalhost(basePath, videoPaths[0]));
		}
	}

	@Override
	protected void onPostExecute(String path) {
		super.onPostExecute(path);
		File tmp = new File(path);
		tmp.delete();
		Log.d(TAG, "Deleted temp video file " + path);
	}

	/**
	 * Creates temp video file from internal path
	 * 
	 * @param videoPath
	 *            the internal path
	 * @return video temp url
	 */
	protected String createTempVideoFile(String videoPath){
		final String temPath = videoTempPath;
		File video = new File(videoPath);
		File tmp = new File(temPath);
		//
		try {
			InputStream in = new FileInputStream(video);
			OutputStream out = new FileOutputStream(tmp);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
			Log.d(TAG, "Created temp video file " + temPath + " => " + tmp.length());
			return temPath;
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Create temp video file failed", e);
			return IOEXEPTION_CODE;
		} catch (IOException e) {
			Log.e(TAG, "Create temp video file failed", e);
			return IOEXEPTION_CODE;
		}
	}

	
	private String getUrlFromLocalhost(String basePath, String uriString) {
		String local = "http://localhost/";
		int startIdx = local.length();
		int finIdx = uriString.length();
		if(uriString.contains("?")){
			finIdx = uriString.indexOf("?");
		}
		String assetsFile = uriString.substring(startIdx, finIdx);
		return basePath + "/" + assetsFile;
	}

}
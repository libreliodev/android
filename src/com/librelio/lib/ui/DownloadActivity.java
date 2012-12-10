package com.librelio.lib.ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.librelio.lib.LibrelioApplication;
import com.niveales.wind.R;

public class DownloadActivity extends Activity {
	private static final String TAG = "DownloadActivity";
	private static final String STOP = "stop_modificator";
	
	public static final String FILE_URL_KEY = "file_url_key";
	public static final String FILE_PATH_KEY = "file_path_key";
	public static final String PNG_PATH_KEY = "png_path_key";
	
	
	private String fileUrl;
	private String filePath;
	private ImageView preview;
	private TextView text;
	private ProgressBar progress;
	private DownloadTask download; 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download);
		preview = (ImageView)findViewById(R.id.download_preview_image);
		text = (TextView)findViewById(R.id.download_progress_text);
		progress = (ProgressBar)findViewById(R.id.download_progress);
		progress.setProgress(0);
		fileUrl = getIntent().getExtras().getString(FILE_URL_KEY);
		filePath = getIntent().getExtras().getString(FILE_PATH_KEY);
		//
		String imagePath = getIntent().getExtras().getString(PNG_PATH_KEY);
		preview.setImageBitmap(BitmapFactory.decodeFile(imagePath));
		text.setText("Downloading");
		
		Log.d(TAG, "fileUrl: "+fileUrl+"\nfilePath: "+filePath);
		download = new DownloadTask();
		download.execute();
	}
	
	private void closeDownloadScreen(){
		finish();
	}
	
	private Context getContext(){
		return this;
	}
	
	class DownloadTask extends AsyncTask<String, String, String>{

		@Override
		protected String doInBackground(String... params) {
			int count;
			//DownloadMagazineListService.downloadFromUrl(fileUrl, filePath);
			try {

			URL url = new URL(fileUrl);
			URLConnection conexion = url.openConnection();
			conexion.connect();

			int lenghtOfFile = conexion.getContentLength();
			Log.d("ANDRO_ASYNC", "Lenght of file: " + lenghtOfFile);

			InputStream input = new BufferedInputStream(url.openStream());
			OutputStream output = new FileOutputStream(filePath);

			byte data[] = new byte[1024];

			long total = 0;

				while ((count = input.read(data)) != -1) {
					total += count;
					publishProgress(""+(int)((total*100)/lenghtOfFile));
					if(isCancelled()){
						Log.d(TAG, "Task was stop");
						return STOP;
					}
					output.write(data, 0, count);
				}

				output.flush();
				output.close();
				input.close();
			} catch (Exception e) {
				// If download was interrupted then file delete
				File f = new File(filePath);
				f.delete();
				Log.e(TAG, "Problem with download!",e);
			}
			return filePath;
		}
		@Override
		protected void onProgressUpdate(String... p) {
			int curProgress = Integer.parseInt(p[0]);
			text.setText("Downloading "+curProgress+"%");
			progress.setProgress(curProgress);
		}
		@Override
		protected void onPostExecute(String result) {
			if(result.equals(STOP)){
				return;
			}
			Intent intentInvalidate = new Intent(MainMagazineActivity.BROADCAST_ACTION_IVALIDATE);
			sendBroadcast(intentInvalidate);
			LibrelioApplication.startPDFActivity(getContext(),result);
			closeDownloadScreen();
			super.onPostExecute(result);
		}
	}
	
	@Override
	public void onBackPressed() {
		download.cancel(true);
		finish();
		super.onBackPressed();
	}
}

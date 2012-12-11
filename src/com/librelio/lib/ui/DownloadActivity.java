package com.librelio.lib.ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.artifex.mupdf.LinkInfo;
import com.librelio.lib.LibrelioApplication;
import com.librelio.lib.model.MagazineModel;
import com.librelio.lib.service.DownloadMagazineListService;
import com.librelio.lib.utils.PDFParser;
import com.niveales.wind.R;

public class DownloadActivity extends Activity {
	private static final String TAG = "DownloadActivity";
	private static final String STOP = "stop_modificator";
	
	public static final String FILE_NAME_KEY = "file_name_key";
	public static final String FILE_URL_KEY = "file_url_key";
	public static final String FILE_PATH_KEY = "file_path_key";
	public static final String PNG_PATH_KEY = "png_path_key";
	public static final String ORIENTATION_KEY = "orientation";
	
	private String fileName;
	private String fileUrl;
	private String filePath;
	private ImageView preview;
	private TextView text;
	private ProgressBar progress;
	private DownloadTask download;
	private DownloadLinksTask downloadLinks;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG,getIntent().getExtras().getInt(ORIENTATION_KEY)+"");
		switch (getIntent().getExtras().getInt(ORIENTATION_KEY)) {
		case Configuration.ORIENTATION_PORTRAIT:
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		case Configuration.ORIENTATION_LANDSCAPE:
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		default:
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		}
		super.onCreate(savedInstanceState);

		setContentView(R.layout.download);
		preview = (ImageView)findViewById(R.id.download_preview_image);
		text = (TextView)findViewById(R.id.download_progress_text);
		progress = (ProgressBar)findViewById(R.id.download_progress);
		progress.setProgress(0);
		fileName = getIntent().getExtras().getString(FILE_NAME_KEY);
		fileUrl = getIntent().getExtras().getString(FILE_URL_KEY);
		filePath = getIntent().getExtras().getString(FILE_PATH_KEY);
		//
		String imagePath = getIntent().getExtras().getString(PNG_PATH_KEY);
		preview.setImageBitmap(BitmapFactory.decodeFile(imagePath));
		text.setText("Downloading");
		
		Log.d(TAG, "fileUrl: "+fileUrl+"\nfilePath: "+filePath);
		download = new DownloadTask();
		try{
			download.execute();
		} catch (Exception e) {
			Log.e(TAG,"File download failed ("+fileUrl+")",e);
			download.cancel(true);
			finish();
		}
	}
	
	private InputStream input;
	private OutputStream output;
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
				Log.d(TAG, "Lenght of file: " + lenghtOfFile);
				input = new BufferedInputStream(url.openStream());
				output = new FileOutputStream(filePath);
				byte data[] = new byte[1024];
				long total = 0;
				while ((count = input.read(data)) != -1) {
					total += count;
					publishProgress(""+(int)((total*100)/lenghtOfFile));
					if(isCancelled()){
						output.flush();
						output.close();
						input.close();
						Log.d(TAG, "DownloadTask was stop");
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
				return STOP;
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
			downloadLinks = new DownloadLinksTask();
			downloadLinks.execute();
			super.onPostExecute(result);
		}
	}
	
	class DownloadLinksTask extends AsyncTask<String, String, String>{
		private ArrayList<String> links;
		private ArrayList<String> assetsNames;
		@Override
		protected void onPreExecute() {
			MagazineModel.makeAssetsDir(fileName);
			text.setText("Getting assets...");
			Log.d(TAG,"Start DownloadLinksTask");
			links = new ArrayList<String>();
			assetsNames = new ArrayList<String>();
			//
			PDFParser linkGetter = new PDFParser(filePath);
			SparseArray<LinkInfo[]> linkBuf = linkGetter.getLinkInfo();
			if(linkBuf==null){
				Log.d(TAG,"There is no links");
				return;
			}
			for(int i=0;i<linkBuf.size();i++){
				Log.d(TAG,"--- i = "+i);
				if(linkBuf.get(i)!=null){
					for(int j=0;j<linkBuf.get(i).length;j++){
						String link = linkBuf.get(i)[j].uri;
						Log.d(TAG,"link[" + j + "] = "+link);
						String local = "http://localhost";
						if(link.startsWith(local)){
							int startIdx = local.length()+1;
							int finIdx = link.length();
							if(link.contains("?")){
								finIdx = link.indexOf("?");
							}
							String assetsFile = link.substring(startIdx, finIdx);
							links.add(MagazineModel.getAssetsBaseURL(fileName)+assetsFile);
							assetsNames.add(assetsFile);
							Log.d(TAG,"   link: "+MagazineModel.getAssetsBaseURL(fileName)+assetsFile);
							Log.d(TAG,"   file: "+assetsFile);
						}
					}
				}
			}
			text.setText("Download assets 0/"+links.size());
			progress.setProgress(0);
			progress.setMax(links.size());
			super.onPreExecute();
		}
		private int count = 0;
		@Override
		protected String doInBackground(String... params) {
			count = 0;
			for(int i=0;i<links.size();i++){
				String assetUrl = links.get(i);
				String assetPath = MagazineModel.getAssetsDir(fileName)+assetsNames.get(i);
				DownloadMagazineListService.downloadFromUrl(assetUrl, assetPath);
				publishProgress("");
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			count++;
			text.setText("Download assets "+count+"/"+links.size());
			progress.setProgress(count);
			super.onProgressUpdate(values);
		}
		@Override
		protected void onPostExecute(String result) {
			Intent intentInvalidate = new Intent(MainMagazineActivity.BROADCAST_ACTION_IVALIDATE);
			sendBroadcast(intentInvalidate);
			LibrelioApplication.startPDFActivity(getContext(),filePath);
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
	
	private void closeDownloadScreen(){
		finish();
	}
	
	private Context getContext(){
		return this;
	}

}

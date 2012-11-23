package com.librelio.lib.utils.cloud;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.librelio.lib.utils.db.DbHelper;
import com.librelio.lib.utils.db.Ocean;
import com.librelio.lib.utils.db.PurchaseDatabase;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class CloudHelper {
	public static final String TAG = CloudHelper.class.getSimpleName();
	Ocean db;
	PurchaseDatabase purchases;
	Context context;
	CloudEventListener listener;
	
	public CloudHelper(Context context, CloudEventListener listener){
		this.context = context;
		this.listener = listener;
		db = new Ocean(context);
		db.open();
		purchases = new PurchaseDatabase(context);
	}
	
	
	public void deleteIssue(long issueId){
		Issue issue = this.getIssue(issueId);
		Uri path = Uri.parse(issue.getIssue_path());
		File pdf = new File(path.getEncodedSchemeSpecificPart());
		if(pdf.exists()){
			pdf.delete();
		}
		issue.setState(issue.getState() & ~Issue.STATE_LOADED);
		this.updateIssue(issue);
	}
	
	public long downloadIssue(long issueId) throws IOException{
		DownloadManager dm = (DownloadManager) context.getSystemService(Activity.DOWNLOAD_SERVICE);
		Issue issue = this.getIssue(issueId);
		Uri from = Uri.parse(issue.getPdf_url());
		String filename = from.getLastPathSegment();
		Request request = new Request(Uri.parse(issue.getPdf_url()));
		Magazine mag = getMagazine(issue.getMagazineId());
		request.setDestinationInExternalFilesDir(context, null, mag.getSku()+ "/" + filename);
//		request.setShowRunningNotification(false);
		request.setTitle("Скачиваю " + issue.getName());
		return dm.enqueue(request);
	}
	
	private Magazine getMagazine(long magazineId) {
		Magazine mag = new Magazine();
		Cursor c = db.getMagazine(magazineId);
		if(c.getCount() > 0) {
			mag.setName(c.getString(c.getColumnIndexOrThrow(Ocean.MAGAZINE_NAME_KEY)));
			mag.setSku(c.getString(c.getColumnIndexOrThrow(Ocean.MAGAZINE_SKU_KEY)));
			return mag;
		}
		return null;
	}


	public ArrayList<Issue> getIssueList(){
		ArrayList<Issue> mIssueArrayList = new ArrayList<Issue>();
		Cursor cursor = db.getAllIssue();
		cursor.moveToFirst();
		while(!cursor.isAfterLast()){
			try{
				mIssueArrayList.add(getIssueFromCursor(cursor));
			} catch (Exception e){
				Log.d(TAG, e.getMessage());
			} finally {
				
			}
			cursor.moveToNext();
		}
		
		return mIssueArrayList;
	}
	
	
	public void updateIssueList(){
		
	}
	
	public Issue getIssue(long id){
		Cursor cursor = db.getIssue(id);
		return getIssueFromCursor(cursor);
	}
	
	public void recycle(){
		db.close();
		purchases.close();
	}
	// Private class methods
	
	
	private Issue getIssueFromCursor(Cursor cursor) throws IllegalArgumentException {
		Issue issue = new Issue();
		issue.setId(cursor.getLong(0));
		issue.setName(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.ISSUE_NAME_KEY)));
		issue.setCover_path(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.ISSUE_COVER_PATH_KEY)));
		issue.setDate(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.ISSUE_DATE_KEY)));
		issue.setIssue_path(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.ISSUE_ISSUE_PATH_KEY)));
		issue.setMagazineId(cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.ISSUE_MAGAZINE_ID_KEY)));
		issue.setPreview_path(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.ISSUE_PREVIEW_PATH_KEY)));
		issue.setPrice(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.ISSUE_PRICE_KEY)));
		issue.setState(cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.ISSUE_STATE_KEY)));
		issue.setPdf_url(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.ISSUE_PDF_URL_KEY)));
		issue.setSku(cursor.getString(cursor.getColumnIndexOrThrow(Ocean.ISSUE_SKU_KEY)));
		return issue;
	}
	
	private Magazine getMagazineFromCursor(Cursor cursor) throws IllegalArgumentException {
		Magazine mag = new Magazine();
		mag.setName(cursor.getString(cursor.getColumnIndex(Ocean.MAGAZINE_NAME_KEY)));
		mag.setVersion(cursor.getString(cursor.getColumnIndex(Ocean.MAGAZINE_VERSION_KEY)));
		mag.setBaseURL(cursor.getString(cursor.getColumnIndex(Ocean.MAGAZINE_BASE_URL_KEY)));
		mag.setSku(cursor.getString(cursor.getColumnIndex(Ocean.MAGAZINE_SKU_KEY)));
		return mag;
	}
	
	public long updateIssue(Issue issue){
		Cursor cursor = db.getIssue(issue.getId());
		if(cursor.getCount() > 0){
			// Issue exists in DB, need to update it
			return db.updateIssue(issue.getId(), Integer.valueOf((int)issue.getMagazineId()), Integer.valueOf(issue.getState()), issue.getName(), issue.getDate(), issue.getPrice(), issue.getPdf_url(), issue.getPreview_path(),
					issue.getCover_path(), issue.getIssue_path(), issue.getSku());
		} else {
			return db.addIssue(Integer.valueOf((int)issue.getMagazineId()), Integer.valueOf(issue.getState()), issue.getName(), issue.getDate(), issue.getPrice(), issue.getPdf_url(), issue.getPreview_path(),
					issue.getCover_path(), issue.getIssue_path(), issue.getSku());
		}
	}

	public interface CloudEventListener {
		public void onReloadIssuesFinished();
	}

	public void reloadIssues() throws IOException {
		
		// reload list of issues from cloud storage
		BufferedInputStream is = new BufferedInputStream(context.getAssets().open("xml/ocean.json"));
		String mIssuesString = new String();
		StringWriter writer = new StringWriter();
		Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		
		char [] buffer = new char[1024];
		int count = 0;
		
		while ( (count = reader.read(buffer, 0, buffer.length)) > 0){
			writer.write(buffer, 0, count);
		}
		
		mIssuesString = writer.toString();
		
		JSONObject json;
		
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		if( !mExternalStorageAvailable ) {
			throw new IOException("External storage not available");
		}
		
		if( !mExternalStorageWriteable ) {
			throw new IOException("External storage not writable");
		}
		try {
			// Read JSON data from input stream
			json = new JSONObject(mIssuesString);
			
			String mMagazineTitle = json.getJSONObject("magazine").getString("name");
			String mMagazineSKU = json.getJSONObject("magazine").getString("sku");
			String mMagazineVersion = json.getJSONObject("magazine").getString("version");
			int mMagazineSerial = json.getJSONObject("magazine").getInt("serial");
			long mMagazineId = db.addMagazine(mMagazineTitle, mMagazineSerial, mMagazineVersion, mMagazineSKU);
			final File mExtStorageFilesDir = context.getExternalFilesDir(null);
			// Get list of issues
			JSONArray mIssuesJSONArray = json.getJSONObject("magazine").getJSONArray("issue");
			for(int i = 0; i < mIssuesJSONArray.length(); i++){
				JSONObject mjo = mIssuesJSONArray.getJSONObject(i);
				Issue issue = new Issue();
				issue.cover_path = mjo.getString("cover");
				issue.name = mjo.getString("name");
				issue.date = mjo.getString("date");
				issue.pdf_url = mjo.getString("url");
				issue.sku = mjo.getString("sku");
				issue.setMagazineId(mMagazineId);
				
				Cursor c = db.getIssueBySKU(issue.sku);
				Issue dbIssue = null;
				if(c.getCount() > 0){
					dbIssue = this.getIssue(c.getLong(0));
				} 
				if(dbIssue == null || !(new File(dbIssue.getCover_path())).exists()){
//					File f = new File(context.getExternalFilesDir(null), "cover.jpg");
					downloadIssueAssets(issue, mExtStorageFilesDir.getPath() + "/" + mMagazineSKU + "/" + issue.sku);
				}
			}
			listener.onReloadIssuesFinished();
		} catch (JSONException e) {
			e.printStackTrace();
			throw new IOException("JSON Error");
		}
	}
	
	private void downloadIssueAssets(Issue issue, String toDir){
		String mCoverFileName = toDir+"/cover.jpg";
		DownloadFileTask coverTask = new DownloadFileTask(issue.cover_path, mCoverFileName, listener);
		SimpleDownloadManager mSimpleDownloadManager = new SimpleDownloadManager();
		mSimpleDownloadManager.execute(coverTask);
		issue.setCover_path(mCoverFileName);
		updateIssue(issue);
	}
	
//	private void downloadObjectToDir(String url, File to) throws IOException {
//
////		if( ! to.canWrite() ){
////			if( ! to.mkdirs() )
////				throw new IOException("File not writable");
////		}
//		Log.d(TAG, "Downloading asset from "+ url);
//		BufferedInputStream bis = new BufferedInputStream(getInternetObjectInputStream(url));
//		FileOutputStream os = new FileOutputStream(to);
//		byte [] buffer = new byte[1024];
//		int count = 0;
//		while( true ) {
//			count = bis.read(buffer, 0, buffer.length);
//			if(count <= 0)
//				break;
//			os.write(buffer, 0, count);
//		}
//		os.close();
//		bis.close();
//	}
	
	private InputStream getInternetObjectInputStream(String pURLString) throws IOException{
		URL mUrl = new URL(pURLString);
		return mUrl.openConnection().getInputStream();
	}
	
	
	private class SimpleDownloadManager extends AsyncTask<DownloadFileTask, Void, Void>{

		@Override
		protected Void doInBackground(DownloadFileTask... params) {
			// TODO Auto-generated method stub
			if(params.length <= 0)
				return null;
			File mTargetFile = new File(params[0].path);
			try {
				downloadObjectToDir(params[0].url, mTargetFile);
				return null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		
		private void downloadObjectToDir(String url, File to) throws IOException {

			if( ! to.canWrite() ){
				if( ! to.getParentFile().mkdirs() )
					throw new IOException("File not writable "+to.getAbsolutePath());
			}
			Log.d(TAG, "Downloading asset from "+ url);
			BufferedInputStream bis = new BufferedInputStream(getInternetObjectInputStream(url));
			FileOutputStream os = new FileOutputStream(to);
			byte [] buffer = new byte[1024];
			int count = 0;
			while( true ) {
				count = bis.read(buffer, 0, buffer.length);
				if(count <= 0)
					break;
				os.write(buffer, 0, count);
			}
			os.close();
			bis.close();
		}
		
		
		@Override
		protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            listener.onReloadIssuesFinished();
        }
	}
	
	private class DownloadFileTask{
		private String path;
		private String url;
		private CloudEventListener listener;
		public DownloadFileTask(String pRemoteURL, String pLocalPath, CloudEventListener listener){
			this.url = pRemoteURL;
			this.path = pLocalPath;
			this.listener = listener;
		}
	}

	public Cursor getAllIssueCursor() {
		// TODO Auto-generated method stub
		return db.getAllIssue();
	}


	public void reopenDB() {
		db.open();
	}

	
	public Cursor getAllMagazineCursor(){
		return db.getAllMagazine();
	}

	public ArrayList<Magazine> getMagazineList() {
		
		ArrayList<Magazine> mMagazineArrayList = new ArrayList<Magazine>();
		Cursor cursor = db.getAllMagazine();
		cursor.moveToFirst();
		while(!cursor.isAfterLast()){
			try{
				mMagazineArrayList.add(getMagazineFromCursor(cursor));
			} catch (Exception e){
				Log.d(TAG, e.getMessage());
			} finally {
				
			}
			cursor.moveToNext();
		}
		
		return mMagazineArrayList;
	}


	public void setIssuePurchiseState(Set<String> mOwnedItems) {
		Cursor cursor = this.getAllIssueCursor();
		while(!cursor.isAfterLast()){
			try{
				Issue issue = getIssueFromCursor(cursor);
				if(mOwnedItems.contains(issue.getSku())){
					issue.setState(issue.getState() | Issue.STATE_PURCHASED );
					this.updateIssue(issue);
				}
			} catch (Exception e){
				Log.d(TAG, e.getMessage());
			} finally {
				
			}
			cursor.moveToNext();
		}
	}


	public boolean isSubscribtionActive() {
		// TODO Auto-generated method stub
		ArrayList<Magazine> mags = getMagazineList();
		if(mags.size() <= 0)
			return false;
		Cursor c = this.purchases.queryAllPurchaseHistory();
		if(c.getCount() <= 0 )
			return false;
		c.moveToFirst();
		while(!c.isAfterLast()){
			
			String sku = c.getString(c.getColumnIndex(PurchaseDatabase.HISTORY_PRODUCT_ID_COL));
			long count = purchases.queryPurchasedItemCount(sku);
			if(sku.equals(mags.get(0).getSku()) && count > 0){
				long purchaseTime = c.getLong(c.getColumnIndex(PurchaseDatabase.HISTORY_PURCHASE_TIME_COL));
				if(purchaseTime + 365*24*60*60*1000 >= System.currentTimeMillis()){
					c.close();
					return true;
				} else 
					return false;
			}
			
			c.moveToNext();
		}
		return false;
	}
}

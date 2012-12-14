package com.librelio.lib.model;

import java.io.File;
import java.io.IOException;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.librelio.lib.LibrelioApplication;
import com.librelio.lib.storage.DataBaseHelper;
import com.librelio.lib.storage.Magazines;
import com.librelio.lib.ui.MainMagazineActivity;

public class MagazineModel {
	private static final String TAG = "MagazineModel";
	private static final String COMPLETE_FILE = ".complete";
	private static final String COMPLETE_SAMPLE_FILE = ".sample_complete";
	
	private Context context;
	private String title;
	private String subtitle;
	private String fileName;
	private String pdfPath;
	private String pngPath;
	private String samplePath;
	private String pdfUrl;
	private String pngUrl;
	private String sampleUrl;
	private boolean isPaid;
	private boolean isDowloaded;
	private boolean isSampleDowloaded = false;
	private String assetsDir;
	private String downloadDate;

	public MagazineModel(String fileName, String title, String subtitle,
			String downloadDate, Context context) {
		this.fileName = fileName;
		this.title = title;
		this.subtitle = subtitle;
		this.downloadDate = downloadDate;
		this.context = context;

		valuesInit(fileName);
	}

	public MagazineModel(Cursor cursor, Context context) {
		int titleColumnId = cursor.getColumnIndex(Magazines.FIELD_TITLE);
		int subitleColumnId = cursor.getColumnIndex(Magazines.FIELD_SUBTITLE);
		int fileNameColumnId = cursor.getColumnIndex(Magazines.FIELD_FILE_NAME);
		int dateColumnId = cursor.getColumnIndex(Magazines.FIELD_DOWNLOAD_DATE);
		
		this.fileName = cursor.getString(fileNameColumnId);
		this.title = cursor.getString(titleColumnId);
		this.subtitle = cursor.getString(subitleColumnId);
		this.downloadDate = cursor.getString(dateColumnId);
		this.context = context;

		valuesInit(fileName);
	}
	
	public static String getMagazineDir(String fileName){
		int finishNameIndex = fileName.indexOf("/");
		return LibrelioApplication.APP_DIRECTORY
					+fileName.substring(0,finishNameIndex)+"/";
	}
	
	public static String getAssetsBaseURL(String fileName){
		int finishNameIndex = fileName.indexOf("/");
		return LibrelioApplication.BASE_URL+fileName.substring(0,finishNameIndex)+"/";
	}

	public static void makeMagazineDir(String fileName){
		File assets = new File(getMagazineDir(fileName));
		if(!assets.exists()){
			assets.mkdirs();
		}
	}
	
	public static void clearMagazineDir(String fileName){
		File dir = new File(getMagazineDir(fileName));
		if (dir.exists()) {
			if (dir.isDirectory()) {
				for (File c : dir.listFiles()) c.delete();
			} else {
				dir.delete();
			}
		}
	}
	
	public void delete(){
		Log.d(TAG,"Deleting magazine has been initiated");
		clearMagazineDir(fileName);
		new File(getMagazineDir(fileName)).delete();
		
		Intent intentInvalidate = new Intent(MainMagazineActivity.BROADCAST_ACTION_IVALIDATE);
		context.sendBroadcast(intentInvalidate);
	}
	
	public synchronized void saveInBase() {
		SQLiteDatabase db;
		DataBaseHelper dbhelp = new DataBaseHelper(context);
		db = dbhelp.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(Magazines.FIELD_FILE_NAME, fileName);
		cv.put(Magazines.FIELD_DOWNLOAD_DATE, downloadDate);
		cv.put(Magazines.FIELD_TITLE, title);
		cv.put(Magazines.FIELD_SUBTITLE, subtitle);
		db.insert(Magazines.TABLE_NAME, null, cv);
		db.close();
	}

	private void valuesInit(String fileName) {
		isPaid = fileName.contains("_.");
		int startNameIndex = fileName.indexOf("/")+1;
		String png = LibrelioApplication.APP_DIRECTORY+fileName.substring(startNameIndex, fileName.length()); 
		pdfUrl = LibrelioApplication.BASE_URL + fileName;
		pdfPath = getMagazineDir(fileName)+fileName.substring(startNameIndex, fileName.length());
		if(isPaid){
			pngUrl = pdfUrl.replace("_.pdf", ".png");
			pngPath = png.replace("_.pdf", ".png");
			sampleUrl = pdfUrl.replace("_.", ".");
			samplePath = pdfPath.replace("_.", ".");
			File sample = new File(getMagazineDir(fileName)+COMPLETE_SAMPLE_FILE);
			isSampleDowloaded = sample.exists();
		} else {
			pngUrl = pdfUrl.replace(".pdf", ".png");
			pngPath = png.replace(".pdf", ".png");
		}
		File complete = new File(getMagazineDir(fileName)+COMPLETE_FILE);
		isDowloaded = complete.exists();
		
		assetsDir = getMagazineDir(fileName);
	}

	public void makeCompleteFile(boolean isSample){
		String completeModificator = COMPLETE_FILE;
		if(isSample){
			completeModificator = COMPLETE_SAMPLE_FILE;
		}
		File file = new File(getMagazineDir(fileName)+completeModificator);
		boolean create = false;
		try {
			create = file.createNewFile();
		} catch (IOException e) {
			Log.d(TAG,"Problem with create .complete, createNewFile() return "+create,e);
		}
	}
	
	public String getAssetsDir(){
		return this.assetsDir;
	}
	
	public boolean isPaid() {
		return this.isPaid;
	}

	public boolean isDownloaded(){
		return this.isDowloaded;
	}
	public boolean isSampleDownloaded(){
		return this.isSampleDowloaded;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getSamplePath() {
		return samplePath;
	}

	public void setSamplePath(String samplePath) {
		this.samplePath = samplePath;
	}

	public String getSampleUrl() {
		return sampleUrl;
	}

	public void setSampleUrl(String sampleUrl) {
		this.sampleUrl = sampleUrl;
	}

	public String getPdfPath() {
		return pdfPath;
	}

	public void setPdfPath(String pdfName) {
		this.pdfPath = pdfName;
	}

	public String getPngPath() {
		return pngPath;
	}

	public void setPngPath(String pngName) {
		this.pngPath = pngName;
	}

	public String getPdfUrl() {
		return pdfUrl;
	}

	public void setPdfUrl(String pdfUrl) {
		this.pdfUrl = pdfUrl;
	}

	public String getPngUrl() {
		return pngUrl;
	}

	public void setPngUrl(String pngUrl) {
		this.pngUrl = pngUrl;
	}

	public String getDownloadDate() {
		return downloadDate;
	}

	public void setDownloadDate(String downloadDate) {
		this.downloadDate = downloadDate;
	}

}

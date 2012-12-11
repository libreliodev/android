package com.librelio.lib.model;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

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
	
	public static String getAssetsDir(String fileName){
		int startNameIndex = fileName.indexOf("/")+1;
		int finishNameIndex = fileName.indexOf(".");
		return LibrelioApplication.appDirectory
					+fileName.substring(startNameIndex,finishNameIndex)+"/";
	}
	
	public static String getAssetsBaseURL(String fileName){
		int finishNameIndex = fileName.indexOf("/");
		return LibrelioApplication.BASE_URL+fileName.substring(0,finishNameIndex)+"/";
	}

	public static void makeAssetsDir(String fileName){
		File assets = new File(getAssetsDir(fileName));
		if(!assets.exists()){
			assets.mkdirs();
		}
	}
	
	public void delete(){
		Log.d(TAG,"Deleting magazine has been initiated");
		ArrayList<File> files = new ArrayList<File>();
		files.add( new File(pdfPath));
		files.add( new File(getAssetsDir(fileName)));
		if(samplePath!=null){
			files.add( new File(samplePath));
		}
		for (File file : files) {
			if (file.exists()) {
				if (file.isDirectory()) {
					for (File c : file.listFiles()) c.delete();
				} else {
					file.delete();
				}
			}
		}

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
		pdfUrl = LibrelioApplication.BASE_URL + fileName;
		pdfPath = LibrelioApplication.appDirectory+fileName.substring(startNameIndex, fileName.length());
		if(isPaid){
			pngUrl = pdfUrl.replace("_.pdf", ".png");
			pngPath = pdfPath.replace("_.pdf", ".png");
			sampleUrl = pdfUrl.replace("_.", ".");
			samplePath = pdfPath.replace("_.", ".");
			File sample = new File(getSamplePath());
			isSampleDowloaded = sample.exists();
		} else {
			pngUrl = pdfUrl.replace(".pdf", ".png");
			pngPath = pdfPath.replace(".pdf", ".png");
		}
		File pdf = new File(getPdfPath());
		isDowloaded = pdf.exists();
		
		assetsDir = getAssetsDir(fileName);
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

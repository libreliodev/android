package com.librelio.lib.model;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.librelio.lib.LibrelioApplication;
import com.librelio.lib.storage.DataBaseHelper;
import com.librelio.lib.storage.Magazines;

public class MagazineModel {
	private Context context;
	private String title;
	private String subtitle;
	private String fileName;
	private String pdfName;
	private String pngName;
	private String pdfUrl;
	private String pngUrl;
	private boolean isPaid;
	private ArrayList<String> assets_references;
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
		int startIndex = fileName.indexOf("/")+1;
		pdfUrl = LibrelioApplication.BASE_URL + fileName;
		pdfName = LibrelioApplication.appDirectory+fileName.substring(startIndex, fileName.length());
		if(isPaid){
			pngUrl = pdfUrl.replace("_.pdf", ".png");
			pngName = pdfName.replace("_.pdf", ".png");
		} else {
			pngUrl = pdfUrl.replace(".pdf", ".png");
			pngName = pdfName.replace(".pdf", ".png");
		}
	}

	public boolean isPaid() {
		return this.isPaid;
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

	public String getPdfName() {
		return pdfName;
	}

	public void setPdfName(String pdfName) {
		this.pdfName = pdfName;
	}

	public String getPngName() {
		return pngName;
	}

	public void setPngName(String pngName) {
		this.pngName = pngName;
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

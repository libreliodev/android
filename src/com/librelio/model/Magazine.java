package com.librelio.model;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import com.librelio.LibrelioApplication;
import com.librelio.event.LoadPlistEvent;
import com.librelio.storage.DataBaseHelper;
import com.librelio.storage.MagazineManager;
import com.librelio.utils.StorageUtils;
import de.greenrobot.event.EventBus;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class Magazine extends DictItem {
	protected static final String TAG = Magazine.class.getSimpleName();
	private static final String COMPLETE_FILE = ".complete";
	private static final String COMPLETE_SAMPLE_FILE = ".sample_complete";

	private Context context;
	private long id;
	private String title;
	private String subtitle;
	private String pdfPath;
	private String samplePdfPath;
	private String samplePdfUrl;
	private boolean isPaid;
	private boolean isDownloaded;
	private boolean isSampleDownloaded = false;
	private String downloadDate;
	private boolean isSample;
    private int downloadStatus = DownloadStatus.NOT_DOWNLOADED;

    public Magazine(String fileName, String title, String subtitle, String downloadDate, Context context) {
		this.fileName = fileName;
		this.title = title;
		this.subtitle = subtitle;
		this.downloadDate = downloadDate;
		this.context = context;

		initValues(fileName);
	}

	public Magazine(Cursor cursor, Context context) {
		int idColumnId = cursor.getColumnIndex(DataBaseHelper.FIELD_ID);
		int titleColumnId = cursor.getColumnIndex(DataBaseHelper.FIELD_TITLE);
		int subitleColumnId = cursor.getColumnIndex(DataBaseHelper.FIELD_SUBTITLE);
		int fileNameColumnId = cursor.getColumnIndex(DataBaseHelper.FIELD_FILE_NAME);
		int dateColumnId = cursor.getColumnIndex(DataBaseHelper.FIELD_DOWNLOAD_DATE);
		int isSampleColumnId = cursor.getColumnIndex(DataBaseHelper.FIELD_IS_SAMPLE);
        int downloadStatus = cursor.getColumnIndex(DataBaseHelper.FIELD_DOWNLOAD_STATUS);
		
		this.id = cursor.getInt(idColumnId);
		this.fileName = cursor.getString(fileNameColumnId);
		this.title = cursor.getString(titleColumnId);
		this.subtitle = cursor.getString(subitleColumnId);
		this.downloadDate = cursor.getString(dateColumnId);
		this.downloadStatus = cursor.getInt(downloadStatus);
		if (isSampleColumnId > -1){
			this.isSample = cursor.getInt(isSampleColumnId) == 0 ? false : true;
		}
		this.context = context;

		initValues(fileName);
	}

	public String getMagazineDir(){
		int finishNameIndex = fileName.lastIndexOf("/");
		return StorageUtils.getStoragePath(context) + fileName.substring(0,finishNameIndex)+"/";
	}
	
	public static String getServerBaseURL(String fileName){
		int finishNameIndex = fileName.lastIndexOf("/");
		return LibrelioApplication.getAmazonServerUrl() + fileName.substring(0,finishNameIndex) + "/";
	}

	public void makeMagazineDir(){
		File magazineDir = new File(getMagazineDir());
		if(!magazineDir.exists()){
			magazineDir.mkdirs();
		}
	}
	
	public void clearMagazineDir(){
        try {
            FileUtils.deleteDirectory(new File(getMagazineDir()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        EventBus.getDefault().post(new LoadPlistEvent());
	}

	private void initValues(String fileName) {
		isPaid = fileName.contains("_.");
		int startNameIndex = fileName.lastIndexOf("/")+1;
	    pdfUrl = LibrelioApplication.getAmazonServerUrl() + fileName;
		pdfPath = getMagazineDir()+fileName.substring(startNameIndex, fileName.length());
		if(isPaid){
			pngUrl = pdfUrl.replace("_.pdf", ".png");
			samplePdfUrl = pdfUrl.replace("_.", ".");
			samplePdfPath = pdfPath.replace("_.", ".");
			File sample = new File(samplePdfPath);
			isSampleDownloaded = sample.exists();
		} else {
			pngUrl = pdfUrl.replace(".pdf", ".png");
		}
		File complete = new File(pdfPath);
		isDownloaded = complete.exists();
		
        makeMagazineDir();
	}

//	public void makeCompleteFile(boolean isSample){
//		String completeModificator = COMPLETE_FILE;
//		if (isSample) {
//			completeModificator = COMPLETE_SAMPLE_FILE;
//		}
//		File file = new File(getMagazineDir() + completeModificator);
//		boolean create = false;
//		try {
//			create = file.createNewFile();
//		} catch (IOException e) {
//			Log.d(TAG,"Problem creating "+completeModificator+", createNewFile() return "+create,e);
//		}
//	}
	
	public long getId(){
		return this.id;
	}

    public void setId(long id){
        this.id = id;
    }
	
	public boolean isPaid() {
		return this.isPaid;
	}

	public boolean isDownloaded(){
		return this.isDownloaded;
	}
	public boolean isSampleDownloaded(){
		return this.isSampleDownloaded;
	}
	
	public String getTitle() {
		return title;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public String getSamplePdfPath() {
		return samplePdfPath;
	}

	public String getSamplePdfUrl() {
		return samplePdfUrl;
	}

	public String getFilename() {
		return pdfPath;
	}

	public void setSample(boolean isSample) {
		this.isSample = isSample;
	}

	public String getItemUrl() {
		return pdfUrl;
	}

	public String getPngUrl() {
		return pngUrl;
	}

	public String getDownloadDate() {
		return downloadDate;
	}

	public void setDownloadDate(String downloadDate) {
		this.downloadDate = downloadDate;
	}

	public boolean isFake() {
		return getFileName().equals(MagazineManager.TEST_FILE_NAME);
	}
	
	public boolean isSample() {
		return isSample;
	}
	
	public int isSampleForBase() {
		return isSample ? 1 : 0;
	}

    public int getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(int downloadStatus) {
        this.downloadStatus = downloadStatus;
    }
}

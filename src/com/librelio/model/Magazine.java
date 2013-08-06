package com.librelio.model;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.librelio.LibrelioApplication;
import com.librelio.event.LoadPlistEvent;
import com.librelio.storage.MagazineManager;
import com.librelio.utils.StorageUtils;
import de.greenrobot.event.EventBus;

public class Magazine {
	protected static final String TAG = Magazine.class.getSimpleName();
	private static final String COMPLETE_FILE = ".complete";
	private static final String COMPLETE_SAMPLE_FILE = ".sample_complete";
	private static final String PAID_FILE = ".payed";

	public static final String TABLE_DOWNLOADED_MAGAZINES = "DownloadedMagazines";
    public static final String TABLE_ASSETS = "Assets";
	public static final String FIELD_ID = "_id";
	public static final String FIELD_TITLE = "title";
	public static final String FIELD_SUBTITLE = "subtitle";
	public static final String FIELD_FILE_NAME = "filename";
    public static final String FIELD_ASSET_FILE_NAME = "assetfilename";
    public static final String FIELD_ASSET_IS_DOWNLOADED = "assetisdownloaded";
	public static final String FIELD_DOWNLOAD_DATE = "downloaddate";
	public static final String FIELD_IS_SAMPLE = "sample";
    public static final String FIELD_DOWNLOAD_MANAGER_ID = "downloadmanagerid";

    private Context context;
	private long id;
	private String title;
	private String subtitle;
	private String fileName;
	private String pdfPath;
	private String pngPath;
	private String samplePdfPath;
	private String pdfUrl;
	private String pngUrl;
	private String samplePdfUrl;
	private boolean isPaid;
	private boolean isDownloaded;
	private boolean isSampleDownloaded = false;
	private String assetsDir;
	private String downloadDate;
	private boolean isSample;
    private int downloadStatus = -1;
    private long downloadManagerId;
    private int downloadProgress;
    private int totalAssetCount;
    private int totalDownloadedCount;

    public Magazine(String fileName, String title, String subtitle, String downloadDate, Context context) {
		this.fileName = fileName;
		this.title = title;
		this.subtitle = subtitle;
		this.downloadDate = downloadDate;
		this.context = context;

		valuesInit(fileName);
	}

	public Magazine(Cursor cursor, Context context) {
		int idColumnId = cursor.getColumnIndex(FIELD_ID);
		int titleColumnId = cursor.getColumnIndex(FIELD_TITLE);
		int subitleColumnId = cursor.getColumnIndex(FIELD_SUBTITLE);
		int fileNameColumnId = cursor.getColumnIndex(FIELD_FILE_NAME);
		int dateColumnId = cursor.getColumnIndex(FIELD_DOWNLOAD_DATE);
		int isSampleColumnId = cursor.getColumnIndex(FIELD_IS_SAMPLE);
        int downloadManagerId = cursor.getColumnIndex(FIELD_DOWNLOAD_MANAGER_ID);
		
		this.id = cursor.getInt(idColumnId);
		this.fileName = cursor.getString(fileNameColumnId);
		this.title = cursor.getString(titleColumnId);
		this.subtitle = cursor.getString(subitleColumnId);
		this.downloadDate = cursor.getString(dateColumnId);
		if (isSampleColumnId > -1){
			this.isSample = cursor.getInt(isSampleColumnId) == 0 ? false : true;
            this.downloadManagerId = cursor.getLong(downloadManagerId);
		}
		this.context = context;

		valuesInit(fileName);
	}
	
	public String getMagazineDir(){
		int finishNameIndex = fileName.indexOf("/");
		return StorageUtils.getStoragePath(context) + fileName.substring(0,finishNameIndex)+"/";
	}
	
	public static String getAssetsBaseURL(String fileName){
		int finishNameIndex = fileName.indexOf("/");
		return LibrelioApplication.getAmazonServerUrl() + fileName.substring(0,finishNameIndex) + "/";
	}

	public void makeMagazineDir(){
		File assets = new File(getMagazineDir());
		if(!assets.exists()){
			assets.mkdirs();
		}
	}
	
	public void clearMagazineDir(){
		File dir = new File(getMagazineDir());
		if (dir.exists()) {
			if (dir.isDirectory()) {
				for (File c : dir.listFiles()) c.delete();
			}
			dir.delete();
		}
	}
	
	public void delete(){
		Log.d(TAG,"Deleting magazine has been initiated");
		clearMagazineDir();
		EventBus.getDefault().post(new LoadPlistEvent());
	}

	private void valuesInit(String fileName) {
		isPaid = fileName.contains("_.");
		int startNameIndex = fileName.indexOf("/")+1;
		String png = StorageUtils.getStoragePath(context)+fileName.substring(startNameIndex, fileName.length());
		pdfUrl = LibrelioApplication.getAmazonServerUrl() + fileName;
		pdfPath = getMagazineDir()+fileName.substring(startNameIndex, fileName.length());
		if(isPaid){
			pngUrl = pdfUrl.replace("_.pdf", ".png");
			pngPath = png.replace("_.pdf", ".png");
			samplePdfUrl = pdfUrl.replace("_.", ".");
			samplePdfPath = pdfPath.replace("_.", ".");
			File sample = new File(getMagazineDir()+COMPLETE_SAMPLE_FILE);
			isSampleDownloaded = sample.exists();
		} else {
			pngUrl = pdfUrl.replace(".pdf", ".png");
			pngPath = png.replace(".pdf", ".png");
		}
		File complete = new File(getMagazineDir()+COMPLETE_FILE);
		isDownloaded = complete.exists();
		
		assetsDir = getMagazineDir();
	}

	public void makeCompleteFile(boolean isSample){
		String completeModificator = COMPLETE_FILE;
		if(isSample){
			completeModificator = COMPLETE_SAMPLE_FILE;
		}
		File file = new File(getMagazineDir()+completeModificator);
		boolean create = false;
		try {
			create = file.createNewFile();
		} catch (IOException e) {
			Log.d(TAG,"Problem with create "+completeModificator+", createNewFile() return "+create,e);
		}
	}
	public void makePaidFile(){
		File file = new File(getMagazineDir()+ PAID_FILE);
		boolean create = false;
		if(file.exists()){
			return;
		}
		try {
			create = file.createNewFile();
		} catch (IOException e) {
			Log.d(TAG,"Problem with create "+ PAID_FILE +", createNewFile() return "+create,e);
		}
	}
	
	public long getId(){
		return this.id;
	}

    public void setId(long id){
        this.id = id;
    }
	
	public String getAssetsDir(){
		return this.assetsDir;
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

	public String getFileName() {
		return fileName;
	}

	public String getSamplePdfPath() {
		return samplePdfPath;
	}

	public String getSamplePdfUrl() {
		return samplePdfUrl;
	}

	public String getPdfPath() {
		return pdfPath;
	}

	public String getPngPath() {
		return pngPath;
	}

	public void setSample(boolean isSample) {
		this.isSample = isSample;
	}

	public String getPdfUrl() {
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

    public long getDownloadManagerId() {
        return downloadManagerId;
    }

    public void setDownloadManagerId(long downloadManagerId) {
        this.downloadManagerId = downloadManagerId;
    }

    public void setDownloadProgress(int downloadProgress) {
        this.downloadProgress = downloadProgress;
    }

    public int getDownloadProgress() {
        return downloadProgress;
    }

    public void setDownloadedAssetCount(int totalDownloadedCount) {
        this.totalDownloadedCount = totalDownloadedCount;
    }

    public int getDownloadedAssetCount() {
        return totalDownloadedCount;
    }

    public void setTotalAssetCount(int totalAssetCount) {
        this.totalAssetCount = totalAssetCount;
    }

    public int getTotalAssetCount() {
        return totalAssetCount;
    }
}

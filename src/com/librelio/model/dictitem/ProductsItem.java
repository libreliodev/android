package com.librelio.model.dictitem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;

import android.content.Context;

import com.librelio.LibrelioApplication;
import com.librelio.library.ui.ProductsActivity;
import com.librelio.model.DownloadStatusCode;
import com.librelio.model.interfaces.DisplayableAsGridItem;
import com.librelio.model.interfaces.Downloadable;
import com.librelio.service.ProductsDownloadService;
import com.librelio.utils.StorageUtils;

public class ProductsItem extends DictItem implements DisplayableAsGridItem, Downloadable {
	
	private String subtitle;

	private int downloadStatus = DownloadStatusCode.NOT_DOWNLOADED;
	private Context context;

	public ProductsItem(Context context, String title, String subtitle, String fileName) {
		this.context = context;
		this.subtitle = subtitle;
		this.filePath = fileName;
		this.title = title;
		initValues();
	}
	
	private void initValues() {
//        String actualFileName;
//        Pattern actualFileNamePattern = Pattern.compile("(?=.*\\?)[^\\?]+");
//        Matcher actualFileNameMatcher = actualFileNamePattern.matcher(filePathFromPlist);
//        if (actualFileNameMatcher.find()) {
//            actualFileName = actualFileNameMatcher.group();
//        } else {
//            actualFileName = filePathFromPlist;
//        }
        this.itemFilename = FilenameUtils.getBaseName(filePath);
	}
	
	public String getItemStorageDir() {
		return StorageUtils.getStoragePath(context)
				+ FilenameUtils.getPath(filePath);
	}

	@Override
	protected void initOtherValues() {
		super.initOtherValues();
	}
	
	public boolean isPaid() {
		return filePath.contains("_.");
	}

	public String getSubtitle() {
		return subtitle;
	}
	
	public boolean isDownloaded() {
		return getLocalPathIfAvailable() == null ? false : true;
	}
	
	public String getLocalPathIfAvailable() {
		// check in assets
		String pngPath = filePath;
		try {
			InputStream file = context.getAssets().open(pngPath + ".zip");
			file.close();
			return "file:///android_asset/" + pngPath;
		} catch (IOException e) {
//			e.printStackTrace();
		}

		// check in local file
		String localPngPath = getItemStorageDir() + FilenameUtils.getBaseName(filePath) + ".png";
		File localPngFile = new File(localPngPath);
		if (localPngFile.exists()) {
			return localPngPath;
		}
		return null;
	}

	@Override
	public String getPngUri() {

		// TODO deal with _. in paid items
		
		// check in assets
		String pngPath = getFilePath().replace("sqlite", "png");
		try {
			InputStream file = context.getAssets().open(pngPath);
			file.close();
			return "file:///android_asset/" + pngPath;
		} catch (IOException e) {
//			e.printStackTrace();
		}

		// check in local file
		String localPngPath = getItemStorageDir() + FilenameUtils.getBaseName(filePath) + ".png";
		File localPngFile = new File(localPngPath);
		if (localPngFile.exists()) {
			return localPngPath;
		}

		// else return server url
		if (isPaid()) {
			return getItemUrl().replace("_.sqlite", ".png");
		} else {
			return getItemUrl().replace(".sqlite", ".png");
		}
	}

	@Override
	public void onThumbnailClick(Context context) {
		if (isDownloaded()) {
			ProductsActivity.startActivity(context, this);
		}
	}

	@Override
	public String getItemUrl() {
		return LibrelioApplication.getAmazonServerUrl() + filePath;
	}

	@Override
	public void onDownloadButtonClick(Context context) {
		ProductsDownloadService.startProductsDownload(context, this, false);
	}

	public void onReadButtonClicked(Context context) {
		ProductsActivity.startActivity(context, this);
	}
}
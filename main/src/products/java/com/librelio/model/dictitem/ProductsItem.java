package com.librelio.model.dictitem;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.format.DateUtils;

import com.librelio.LibrelioApplication;
import com.librelio.event.LoadPlistEvent;
import com.librelio.model.DownloadStatusCode;
import com.librelio.model.dictitem.DownloadableDictItem;
import com.librelio.model.interfaces.DisplayableAsGridItem;
import com.librelio.products.ProductsActivity;
import com.librelio.products.ProductsDownloadService;
import com.librelio.products.ui.ProductsBillingActivity;
import com.librelio.storage.DataBaseHelper;
import com.librelio.storage.DownloadsManager;
import com.librelio.utils.StorageUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;

public class ProductsItem extends DownloadableDictItem implements DisplayableAsGridItem {
	
	private String subtitle;

	private int downloadStatus = DownloadStatusCode.NOT_DOWNLOADED;
	private Context context;

	public ProductsItem(Context context, String title, String subtitle, String fileName) {
		this.context = context;
		this.subtitle = subtitle;
		this.title = title;
		initValues(fileName);
	}

    public ProductsItem(Context context, Cursor cursor) {
        int titleColumnId = cursor.getColumnIndex(DataBaseHelper.FIELD_TITLE);
        int subitleColumnId = cursor
                .getColumnIndex(DataBaseHelper.FIELD_SUBTITLE);
        int fileNameColumnId = cursor
                .getColumnIndex(DataBaseHelper.FIELD_FILE_PATH);
        this.title = cursor.getString(titleColumnId);
        this.subtitle = cursor.getString(subitleColumnId);
//		this.downloadStatus = cursor.getInt(cursor
//				.getColumnIndex(DataBaseHelper.FIELD_DOWNLOAD_STATUS));
        this.context = context;

        initValues(cursor.getString(fileNameColumnId));
    }
	
	private void initValues(String filePath) {
        String filePathWithoutQueriesAtEnd;
        Pattern actualFileNamePattern = Pattern.compile("(?=.*\\?)[^\\?]+");
        Matcher actualFileNameMatcher = actualFileNamePattern.matcher(filePath);
        if (actualFileNameMatcher.find()) {
            filePathWithoutQueriesAtEnd = actualFileNameMatcher.group();
        } else {
            filePathWithoutQueriesAtEnd = filePath;
        }
        this.filePath = filePathWithoutQueriesAtEnd;
        this.itemFilename = FilenameUtils.getName(filePathWithoutQueriesAtEnd);

        // may need to check for stuff like this - ?wabbar=yes
//        Pattern updateFrequencyPattern = Pattern.compile("waupdate=([0-9]+)");
//        Matcher updateFrequencyMatcher = updateFrequencyPattern.matcher(filePathFromPlist);
//        if (updateFrequencyMatcher.find()) {
//            parsedItem.setUpdateFrequency(Integer.parseInt(updateFrequencyMatcher.group(1)));
//        }

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
		try {
			InputStream file = context.getAssets().open(filePath + ".zip");
			file.close();
			return "file:///android_asset/" + filePath;
		} catch (IOException e) {
//			e.printStackTrace();
		}

		// check in local database folder
		File localDatabaseFile = new File(getDatabaseStoragePath());
		if (localDatabaseFile.exists()) {
			return getDatabaseStoragePath();
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
			return "file:///" + localPngPath;
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

    public String getItemFilePath() {
        return getItemStorageDir(context) + FilenameUtils.getName(filePath);
    }

	@Override
	public String getItemUrl() {
		return LibrelioApplication.getAmazonServerUrl() + filePath;
	}

    public String getDatabaseStoragePath() {
        return "/data/data/" + context.getPackageName()
                + "/databases/" + itemFilename;
    }

    @Override
    public void makeLocalStorageDir(Context context) {
        File magazineDir = new File(getItemStorageDir(context) + "/Photos");
        if (!magazineDir.exists()) {
            magazineDir.mkdirs();
        }
    }

    public void clearMagazineDir(Context context) {
        try {
            FileUtils.deleteDirectory(new File(getItemStorageDir(context)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        EventBus.getDefault().post(new LoadPlistEvent());
    }

    @Override
    public String getDownloadDate() {
        File file = new File(getDatabaseStoragePath());
        return DateUtils.formatDateTime(context, file.lastModified(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR);
    }

    @Override
    public void deleteItem() {
        clearMagazineDir(context);
        DownloadsManager.removeDownload(context, this);
        new File(getDatabaseStoragePath()).delete();
    }

	@Override
	public void onDownloadButtonClick(Context context) {
        if (isPaid()) {
            Intent intent = new Intent(context,
                    ProductsBillingActivity.class);
            intent.putExtra(ProductsBillingActivity.FILE_NAME_KEY,
                    getFilePath());
            intent.putExtra(ProductsBillingActivity.TITLE_KEY,
                    getTitle());
            intent.putExtra(ProductsBillingActivity.SUBTITLE_KEY,
                    getSubtitle());
            context.startActivity(intent);
        } else {
            ProductsDownloadService.startProductsDownload(context, this, false);
        }
	}

	public void onReadButtonClicked(Context context) {
		ProductsActivity.startActivity(context, this);
	}
}
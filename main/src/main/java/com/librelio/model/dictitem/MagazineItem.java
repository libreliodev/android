package com.librelio.model.dictitem;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;

import com.librelio.LibrelioApplication;
import com.librelio.event.ReloadPlistEvent;
import com.librelio.model.interfaces.DisplayableAsGridItem;
import com.librelio.storage.DataBaseHelper;
import com.librelio.storage.DownloadsManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import de.greenrobot.event.EventBus;

public class MagazineItem extends DownloadableDictItem implements DisplayableAsGridItem {
	protected static final String TAG = MagazineItem.class.getSimpleName();

	private Context context;

	// ****
	// From plist
	// title, filePathFromPlist
	private String subtitle;
	// ****

//	private boolean isSample;

	public MagazineItem(Context context, String title, String subtitle,
			String filePath) {
		this.title = title;
		this.subtitle = subtitle;
		this.filePath = filePath;
		this.context = context;

		initValues();
	}

	public MagazineItem(Context context, Cursor cursor) {
		int titleColumnId = cursor.getColumnIndex(DataBaseHelper.FIELD_TITLE);
		int subitleColumnId = cursor
				.getColumnIndex(DataBaseHelper.FIELD_SUBTITLE);
		int fileNameColumnId = cursor
				.getColumnIndex(DataBaseHelper.FIELD_FILE_PATH);
		this.filePath = cursor.getString(fileNameColumnId);
		this.title = cursor.getString(titleColumnId);
		this.subtitle = cursor.getString(subitleColumnId);
//		this.downloadStatus = cursor.getInt(cursor
//				.getColumnIndex(DataBaseHelper.FIELD_DOWNLOAD_STATUS));
		this.context = context;

		initValues();
	}

	public String getAssetUrl(String assetFileName) {
		return LibrelioApplication.getAmazonServerUrl()
				+ FilenameUtils.getPath(filePath) + assetFileName;
	}

	private void initValues() {
		makeLocalStorageDir(context);
	}

	public boolean isPaid() {
		return filePath.contains("_.");
	}

	public boolean isDownloaded() {
		return new File(getItemFilePath()).exists();
	}

	public boolean isSampleDownloaded() {
		return new File(getSamplePdfPath()).exists();
	}

	public String getSubtitle() {
		return subtitle;
	}

	public String getSamplePdfPath() {
		return getItemStorageDir(context)
				+ filePath.substring(filePath.lastIndexOf("/") + 1,
						filePath.length()).replace("_.", ".");
	}

	public String getSampleItemUrl() {
		return getItemUrl().replace("_.", ".");
	}

	public String getItemFilePath() {
		return getItemStorageDir(context) + FilenameUtils.getName(filePath);
	}

	public String getSampleFilePath() {
		return filePath.replace("_.", ".");
	}

	@Override
	public String getItemUrl() {
		return LibrelioApplication.getAmazonServerUrl() + filePath;
	}

    @Override
	public String getDownloadDate() {
		File file;
		if (isSampleDownloaded()) {
			file = new File(getSamplePdfPath());
		} else {
			file = new File(getItemFilePath());
		}
		return DateUtils.formatDateTime(context, file.lastModified(),
				DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR);
	}

	public boolean isSample() {
		if (isPaid()) {
			return isSampleDownloaded();
		} else {
			return false;
		}
	}

	@Override
	public String getPngUri() {

//		// check in assets
//		String pngName = getPngPath();
//		try {
//			if (Arrays.asList(context.getResources().getAssets().list(""))
//					.contains(pngName)) {
//				return "file:///android_asset/" + pngName;
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		// check in local file
//		String localPngPath = getItemStorageDir() + FilenameUtils.getBaseName(filePath) + ".png";
//		File localPngFile = new File(localPngPath);
//		if (localPngFile.exists()) {
//			return "file:///" + localPngPath;
//		}

		// else return server url
		if (isPaid()) {
			return getItemUrl().replace("_.pdf", ".png");
		} else {
			return getItemUrl().replace(".pdf", ".png");
		}
	}

	public String getNewsstandPngUri() {
		if (isPaid()) {
			return getItemUrl().replace("_.pdf", "_newsstand.png");
		} else {
			return getItemUrl().replace(".pdf", "_newsstand.png");
		}
	}

    public void clearMagazineDir(Context context) {
        try {
            FileUtils.deleteDirectory(new File(getItemStorageDir(context)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        EventBus.getDefault().post(new ReloadPlistEvent());
    }

    @Override
	public void deleteItem() {
		DownloadsManager.removeDownload(context, this);
        clearMagazineDir(context);
	}

	public String getInAppBillingProductId() {
		String productId = FilenameUtils.getName(getFilePath()).toLowerCase(
				Locale.US);
		productId = productId.substring(0, productId.indexOf("_.pdf"));
		return productId;
	}
}

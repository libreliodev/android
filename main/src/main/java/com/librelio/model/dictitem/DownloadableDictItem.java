package com.librelio.model.dictitem;

import android.support.v4.util.Pair;

import com.librelio.model.interfaces.DisplayableAsGridItem;
import com.librelio.storage.DownloadsManager;

public abstract class DownloadableDictItem extends DictItem implements DisplayableAsGridItem {

//    protected int downloadStatus = DownloadStatusCode.NOT_DOWNLOADED;

    public Pair<Integer, Boolean> getDownloadStatus() {
        return new DownloadsManager(context).getDownloadStatus(getFilePath());
    }

//    public void setDownloadStatus(int downloadStatus) {
//        this.downloadStatus = downloadStatus;
//    }

    public abstract String getDownloadDate();

    public abstract void deleteItem();
}

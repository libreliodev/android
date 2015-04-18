package com.librelio.model.dictitem;

import android.content.Context;
import android.database.Cursor;


///////////////////////////////////////////////////////////////////////////
// *******************************************
// STUB! Overriden in Products flavorDimension
// *******************************************
///////////////////////////////////////////////////////////////////////////
public class ProductsItem extends DownloadableDictItem {
    public ProductsItem(Context context, Cursor c) {

    }

    public ProductsItem(Context context, String title, String subtitle, String filePathFromPlist) {

    }

    @Override
    public String getItemUrl() {
        return null;
    }

    public boolean isDownloaded() {
        return false;
    }

    public void clearMagazineDir(Context context) {
    }

    public void onReadButtonClicked(Context context) {
    }

    public boolean isPaid() {
        return false;
    }

    @Override
    public String getDownloadDate() {
        return null;
    }

    @Override
    public void deleteItem() {

    }

    @Override
    public String getPngUri() {
        return null;
    }
}

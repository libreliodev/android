package com.librelio.model.dictitem;

import android.content.Context;

public class UpdatesPlistItem extends PlistItem {
    public UpdatesPlistItem(Context context, String title, String fullFilePath) {
        super(context, title, fullFilePath);

        // FIXME valuesInit being run twice
        this.filePath = fullFilePath.replace(".pdf", "_updates.plist");
        valuesInit();
    }
}

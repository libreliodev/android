package com.librelio.model.dictitem;

import android.content.Context;

public class UpdatesPlistItem extends PlistItem {
    public UpdatesPlistItem(Context context, String title, String filePath) {
        super(context, title, filePath);

        // FIXME valuesInit being run twice
        this.filePath = filePath.replace(".pdf", "_updates.plist");
        valuesInit();
    }
}

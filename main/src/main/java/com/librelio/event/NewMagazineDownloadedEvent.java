package com.librelio.event;

import com.librelio.model.dictitem.MagazineItem;

public class NewMagazineDownloadedEvent {

    private MagazineItem magazine;

    public NewMagazineDownloadedEvent(MagazineItem magazine) {
        this.magazine = magazine;
    }

    public MagazineItem getMagazine() {
        return magazine;
    }
}

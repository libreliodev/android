package com.librelio.event;

import com.librelio.model.dictitem.MagazineItem;

public class MagazineDownloadedEvent {

    private MagazineItem magazine;

    public MagazineDownloadedEvent(MagazineItem magazine) {
        this.magazine = magazine;
    }

    public MagazineItem getMagazine() {
        return magazine;
    }
}

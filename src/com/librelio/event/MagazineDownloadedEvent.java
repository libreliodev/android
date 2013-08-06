package com.librelio.event;

import com.librelio.model.Magazine;

public class MagazineDownloadedEvent {

    private Magazine magazine;

    public MagazineDownloadedEvent(Magazine magazine) {
        this.magazine = magazine;
    }

    public Magazine getMagazine() {
        return magazine;
    }
}

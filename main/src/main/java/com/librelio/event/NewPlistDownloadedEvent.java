package com.librelio.event;

public class NewPlistDownloadedEvent {

    private String plistName;

    public NewPlistDownloadedEvent(String plistName) {

        this.plistName = plistName;
    }

    public String getPlistName() {
        return plistName;
    }
}

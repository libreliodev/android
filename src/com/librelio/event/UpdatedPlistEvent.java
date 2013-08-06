package com.librelio.event;

public class UpdatedPlistEvent {

    private String plistName;

    public UpdatedPlistEvent(String plistName) {
        this.plistName = plistName;
    }

    public String getPlistName() {
        return plistName;
    }
}

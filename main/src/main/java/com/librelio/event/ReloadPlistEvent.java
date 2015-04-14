package com.librelio.event;

public class ReloadPlistEvent {

    private String plistName;

    public ReloadPlistEvent() {

    }

    public ReloadPlistEvent(String plistName) {
        this.plistName = plistName;
    }

    public String getPlistName() {
        return plistName;
    }
}

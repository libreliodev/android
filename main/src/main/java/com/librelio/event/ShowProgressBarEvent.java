package com.librelio.event;

public class ShowProgressBarEvent {

    private String plistName;
    private boolean showProgress;

    public ShowProgressBarEvent(String plistName, boolean showProgress) {
        this.plistName = plistName;
        this.showProgress = showProgress;
    }

    public String getPlistName() {
        return plistName;
    }

    public boolean isShowProgress() {
        return showProgress;
    }
}

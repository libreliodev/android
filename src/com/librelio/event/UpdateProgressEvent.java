package com.librelio.event;

public class UpdateProgressEvent {

    private boolean showProgress;

    public UpdateProgressEvent(boolean showProgress) {
        this.showProgress = showProgress;
    }

    public boolean isShowProgress() {
        return showProgress;
    }
}

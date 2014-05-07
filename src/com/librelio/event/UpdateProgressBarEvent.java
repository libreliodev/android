package com.librelio.event;

public class UpdateProgressBarEvent {

    private boolean showProgress;

    public UpdateProgressBarEvent(boolean showProgress) {
        this.showProgress = showProgress;
    }

    public boolean isShowProgress() {
        return showProgress;
    }
}

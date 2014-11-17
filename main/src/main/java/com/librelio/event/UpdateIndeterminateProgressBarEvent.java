package com.librelio.event;

public class UpdateIndeterminateProgressBarEvent {

    private boolean showProgress;

    public UpdateIndeterminateProgressBarEvent(boolean showProgress) {
        this.showProgress = showProgress;
    }

    public boolean isShowProgress() {
        return showProgress;
    }
}

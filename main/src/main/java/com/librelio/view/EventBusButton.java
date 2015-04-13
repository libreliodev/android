package com.librelio.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Button;

import com.librelio.event.EventBusButtonEvent;

import de.greenrobot.event.EventBus;

public class EventBusButton extends Button {

    OnEventListener listener;

    String eventTag;

    public EventBusButton(Context context) {
        super(context);
    }

    public EventBusButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EventBusButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EventBusButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setOnEventListener(OnEventListener listener) {
        this.listener = listener;
    }

    public void onEventMainThread(EventBusButtonEvent event) {
        if (eventTag != null && eventTag.equals(event.getTag())) {
            if (listener != null) {
                listener.onEventListener();
            }
        }
    }

    public String getEventTag() {
        return eventTag;
    }

    public void setEventTag(String eventTag) {
        this.eventTag = eventTag;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    public interface OnEventListener {
        public abstract void onEventListener();
    }
}
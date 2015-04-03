package com.librelio.event;

public class EventBusButtonEvent {

    private String tag;
    private String text;

    public EventBusButtonEvent(String tag, String text) {
        this.tag = tag;
        this.text = text;
    }

    public String getTag() {
        return tag;
    }

    public String getText() {
        return text;
    }
}

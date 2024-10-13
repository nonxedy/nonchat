package com.nonxedy.utils;

public class BroadcastMessage {
    private boolean enabled;
    private String message;
    private int interval;

    public BroadcastMessage(boolean enabled, String message, int interval) {
        this.enabled = enabled;
        this.message = message;
        this.interval = interval;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getMessage() {
        return message;
    }

    public int getInterval() {
        return interval;
    }
}
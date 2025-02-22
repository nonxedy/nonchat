package com.nonxedy.nonchat.utils;

/**
 * Manages automated broadcast messages with timing control
 * Handles configuration and delivery of scheduled announcements
 */
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
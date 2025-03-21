package com.nonxedy.nonchat.util;

/**
 * Represents a chat type configuration with formatting and behavior settings
 * Handles different chat modes like global and local radius-based chat
 */
public class ChatTypeUtil {
    private final boolean enabled;
    private final String format;
    private final int radius;
    private final char chatChar;
    private final String permission; // Can be null or empty

    public ChatTypeUtil(boolean enabled, String format, int radius, char chatChar, String permission) {
        this.enabled = enabled;
        this.format = format;
        this.radius = radius;
        this.chatChar = chatChar;
        this.permission = permission;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getFormat() {
        return format;
    }

    public int getRadius() {
        return radius;
    }

    public char getChatChar() {
        return chatChar;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
    }

    public boolean isGlobal() {
        return radius == -1;
    }
}

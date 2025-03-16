package com.nonxedy.nonchat.util;

/**
 * Represents a chat type configuration with formatting and behavior settings
 * Handles different chat modes like global and local radius-based chat
 */
public class ChatTypeUtil {
    private final String name;
    private final boolean enabled;
    private final String format;
    private final int radius;
    private final char chatChar;

    public ChatTypeUtil(String name, boolean enabled, String format, int radius, char chatChar) {
        this.name = name;
        this.enabled = enabled;
        this.format = format;
        this.radius = radius;
        this.chatChar = chatChar;
    }

    public String getName() {
        return name;
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

    public boolean isGlobal() {
        return radius == -1;
    }
}


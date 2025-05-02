package com.nonxedy.nonchat.util;

/**
 * Represents a chat type configuration with formatting and behavior settings
 * Handles different chat modes like global and local radius-based chat
 */
public class ChatTypeUtil {
    private final boolean enabled;
    private final String displayName;
    private final String format;
    private final int radius;
    private final char chatChar;
    private final String sendPermission; // Can be null or empty
    private final String receivePermission; // Can be null or empty
    private final int cooldown;
    private final int minLength;
    private final int maxLength;

    // Legacy constructor for backward compatibility
    public ChatTypeUtil(boolean enabled, String format, int radius, char chatChar, String permission) {
        this(enabled, null, format, radius, chatChar, permission, null, 0, 0, -1);
    }

    // Full constructor with all properties
    public ChatTypeUtil(boolean enabled, String displayName, String format, int radius, char chatChar, 
                        String sendPermission, String receivePermission,
                        int cooldown, int minLength, int maxLength) {
        this.enabled = enabled;
        this.displayName = displayName != null ? displayName : "Channel";
        this.format = format;
        this.radius = radius;
        this.chatChar = chatChar;
        this.sendPermission = sendPermission;
        this.receivePermission = receivePermission;
        this.cooldown = cooldown;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDisplayName() {
        return displayName;
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
    
    /**
     * @deprecated Use getSendPermission() instead
     */
    @Deprecated
    public String getPermission() {
        return sendPermission;
    }
    
    public String getSendPermission() {
        return sendPermission;
    }
    
    public String getReceivePermission() {
        return receivePermission;
    }
    
    public int getCooldown() {
        return cooldown;
    }
    
    public int getMinLength() {
        return minLength;
    }
    
    public int getMaxLength() {
        return maxLength;
    }
    
    /**
     * @deprecated Use hasSendPermission() instead
     */
    @Deprecated
    public boolean hasPermission() {
        return sendPermission != null && !sendPermission.isEmpty();
    }
    
    public boolean hasSendPermission() {
        return sendPermission != null && !sendPermission.isEmpty();
    }
    
    public boolean hasReceivePermission() {
        return receivePermission != null && !receivePermission.isEmpty();
    }
    
    public boolean hasCooldown() {
        return cooldown > 0;
    }
    
    public boolean hasMinLength() {
        return minLength > 0;
    }
    
    public boolean hasMaxLength() {
        return maxLength >= 0;
    }

    public boolean isGlobal() {
        return radius == -1;
    }
}

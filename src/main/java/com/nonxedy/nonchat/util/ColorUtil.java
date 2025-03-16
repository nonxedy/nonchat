package com.nonxedy.nonchat.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;

/**
 * Provides color code processing and text formatting for chat messages
 * Supports both legacy color codes and hex colors
 */
public class ColorUtil {
    // Pattern for matching hex color codes in &#RRGGBB format
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Converts color codes in text to actual colored output
     * Processes both & color codes and hex colors
     * @param message The text containing color codes
     * @return Processed string with color codes converted
     */
    public static String parseColor(String message) {
        // Return empty string if message is null
        if (message == null) return "";
        
        // Create a matcher to find hex color patterns in the message
        Matcher matcher = HEX_PATTERN.matcher(message);
        // Create a string buffer with extra capacity for color code conversions
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);

        // Process each hex color code found in the message
        while (matcher.find()) {
            // Extract the hex color value without the &# prefix
            String group = matcher.group(1);
            // Replace the &#RRGGBB with the actual ChatColor format
            matcher.appendReplacement(buffer, ChatColor.of("#" + group).toString());
        }
        // Add the remaining text after the last match
        matcher.appendTail(buffer);

        // Convert standard & color codes and return the fully colored string
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Converts color-coded text into Adventure API Component
     * Used for modern Minecraft text rendering
     * @param message The text to convert to Component
     * @return Adventure Component with processed colors
     */
    public static Component parseComponent(String message) {
        // First convert the color codes to legacy format
        String legacyMessage = parseColor(message);
        // Convert the legacy formatted string to a Component object
        return LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
    }
}

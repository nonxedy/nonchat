package com.nonxedy.nonchat.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;

// Utility class for handling color codes and text formatting in chat messages
public class ColorUtil {
    // Regex pattern to match hex color codes in format &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    // Converts color codes in a string to actual colored text
    // Supports both & color codes and hex colors (&#RRGGBB format)
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

    // Converts a color-coded string into a Component object for Adventure API
    // Used for modern text rendering in Minecraft
    public static Component parseComponent(String message) {
        // First convert the color codes to legacy format
        String legacyMessage = parseColor(message);
        // Convert the legacy formatted string to a Component object
        return LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
    }
}

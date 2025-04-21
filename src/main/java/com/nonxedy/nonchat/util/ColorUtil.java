package com.nonxedy.nonchat.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;

/**
 * Provides color code processing and text formatting for chat messages
 * Supports legacy color codes, hex colors, and MiniMessage format
 */
public class ColorUtil {
    // Pattern for matching hex color codes in &#RRGGBB format
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    // MiniMessage instance for parsing MiniMessage format
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

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
        // Check if the message contains any MiniMessage format tags
        if (message != null && (
                message.contains("<#") || 
                message.contains("<gradient") || 
                message.contains("<rainbow") ||
                message.contains("<bold") ||
                message.contains("<italic") ||
                message.contains("<underlined") ||
                message.contains("<strikethrough") ||
                message.contains("<obfuscated") ||
                message.contains("<reset") ||
                message.contains("<color")
            )) {
            // Parse with MiniMessage if it contains MiniMessage format tags
            return parseMiniMessageComponent(message);
        } else {
            // Otherwise use legacy format parsing
            String legacyMessage = parseColor(message);
            // Convert the legacy formatted string to a Component object
            return LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
        }
    }
    
    /**
     * Parses a string with MiniMessage format into an Adventure Component
     * Supports both MiniMessage format and legacy format
     * @param message The text with MiniMessage formatting
     * @return Parsed Adventure Component
     */
    public static Component parseMiniMessageComponent(String message) {
        if (message == null) return Component.empty();
        
        // First convert any legacy format to equivalent MiniMessage format
        String preparedMessage = prepareMixedFormatMessage(message);
        
        // Parse with MiniMessage
        return MINI_MESSAGE.deserialize(preparedMessage);
    }
    
    /**
     * Prepares a message that might contain both legacy and MiniMessage formats
     * Converts legacy color codes to their equivalent MiniMessage format
     * @param message The mixed format message
     * @return A message with all color codes in MiniMessage format
     */
    private static String prepareMixedFormatMessage(String message) {
        if (message == null) return "";
        
        // First convert hex format from &#RRGGBB to <#RRGGBB>
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            // Replace &#RRGGBB with <#RRGGBB>
            matcher.appendReplacement(buffer, "<#" + hexColor + ">");
        }
        matcher.appendTail(buffer);
        
        String result = buffer.toString();
        
        // Convert legacy color codes to MiniMessage format
        // &0-&f and §0-§f -> <black>, <dark_blue>, etc.
        
        // Convert & codes
        result = result.replace("&0", "<black>");
        result = result.replace("&1", "<dark_blue>");
        result = result.replace("&2", "<dark_green>");
        result = result.replace("&3", "<dark_aqua>");
        result = result.replace("&4", "<dark_red>");
        result = result.replace("&5", "<dark_purple>");
        result = result.replace("&6", "<gold>");
        result = result.replace("&7", "<gray>");
        result = result.replace("&8", "<dark_gray>");
        result = result.replace("&9", "<blue>");
        result = result.replace("&a", "<green>");
        result = result.replace("&b", "<aqua>");
        result = result.replace("&c", "<red>");
        result = result.replace("&d", "<light_purple>");
        result = result.replace("&e", "<yellow>");
        result = result.replace("&f", "<white>");
        
        // Convert § codes (section symbol)
        result = result.replace("§0", "<black>");
        result = result.replace("§1", "<dark_blue>");
        result = result.replace("§2", "<dark_green>");
        result = result.replace("§3", "<dark_aqua>");
        result = result.replace("§4", "<dark_red>");
        result = result.replace("§5", "<dark_purple>");
        result = result.replace("§6", "<gold>");
        result = result.replace("§7", "<gray>");
        result = result.replace("§8", "<dark_gray>");
        result = result.replace("§9", "<blue>");
        result = result.replace("§a", "<green>");
        result = result.replace("§b", "<aqua>");
        result = result.replace("§c", "<red>");
        result = result.replace("§d", "<light_purple>");
        result = result.replace("§e", "<yellow>");
        result = result.replace("§f", "<white>");
        
        // Format codes with & prefix
        result = result.replace("&l", "<bold>");
        result = result.replace("&m", "<strikethrough>");
        result = result.replace("&n", "<underlined>");
        result = result.replace("&o", "<italic>");
        result = result.replace("&k", "<obfuscated>");
        result = result.replace("&r", "<reset>");
        
        // Format codes with § prefix
        result = result.replace("§l", "<bold>");
        result = result.replace("§m", "<strikethrough>");
        result = result.replace("§n", "<underlined>");
        result = result.replace("§o", "<italic>");
        result = result.replace("§k", "<obfuscated>");
        result = result.replace("§r", "<reset>");
        
        return result;
    }
}

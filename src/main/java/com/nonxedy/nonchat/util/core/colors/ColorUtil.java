package com.nonxedy.nonchat.util.core.colors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

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
    
    // Pattern for matching legacy color codes (&0-&f, &k-&r)
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("&[0-9a-fklmnor]");
    
    // Pattern for matching section symbol color codes (§0-§f, §k-§r)
    private static final Pattern SECTION_COLOR_PATTERN = Pattern.compile("§[0-9a-fklmnor]");
    
    // Pattern for matching MiniMessage format tags
    private static final Pattern MINIMESSAGE_PATTERN = Pattern.compile("<[^>]+>");
    
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
                message.contains("<color") ||
                // legacy color codes in MiniMessage
                message.contains("<black") ||
                message.contains("<dark_blue") ||
                message.contains("<dark_green") ||
                message.contains("<dark_aqua") ||
                message.contains("<dark_red") ||
                message.contains("<dark_purple") ||
                message.contains("<gold") ||
                message.contains("<gray") ||
                message.contains("<dark_gray") ||
                message.contains("<blue") ||
                message.contains("<aqua") ||
                message.contains("<red") ||
                message.contains("<light_purple") ||
                message.contains("<yellow") ||
                message.contains("<white")
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
     * Converts color-coded text into Adventure API Component with permission check
     * @param message The text to convert to Component
     * @param player The player to check permissions for (null to skip permission check)
     * @return Adventure Component with processed colors (or plain text if no permission)
     */
    public static Component parseComponent(String message, Player player) {
        if (player != null && !player.hasPermission("nonchat.color")) {
            // Strip all color codes if player doesn't have permission
            return Component.text(stripAllColors(message));
        }
        return parseComponent(message);
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
     * Parses a string with MiniMessage format into an Adventure Component with permission check
     * @param message The text with MiniMessage formatting
     * @param player The player to check permissions for (null to skip permission check)
     * @return Parsed Adventure Component (or plain text if no permission)
     */
    public static Component parseMiniMessageComponent(String message, Player player) {
        if (player != null && !player.hasPermission("nonchat.color")) {
            // Strip all color codes if player doesn't have permission
            return Component.text(stripAllColors(message));
        }
        return parseMiniMessageComponent(message);
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
    
    /**
     * Strips all color codes and formatting from a message
     * @param message The message to strip colors from
     * @return Plain text without any color codes or formatting
     */
    public static String stripAllColors(String message) {
        if (message == null) return "";
        
        String result = message;
        
        // Remove hex color codes (&#RRGGBB)
        result = HEX_PATTERN.matcher(result).replaceAll("");
        
        // Remove legacy color codes (&0-&f, &k-&r)
        result = LEGACY_COLOR_PATTERN.matcher(result).replaceAll("");
        
        // Remove section symbol color codes (§0-§f, §k-§r)
        result = SECTION_COLOR_PATTERN.matcher(result).replaceAll("");
        
        // Remove MiniMessage format tags
        result = MINIMESSAGE_PATTERN.matcher(result).replaceAll("");
        
        return result;
    }
    
    /**
     * Checks if a message contains any color codes or formatting
     * @param message The message to check
     * @return true if the message contains color codes, false otherwise
     */
    public static boolean hasColorCodes(String message) {
        if (message == null) return false;
        
        return HEX_PATTERN.matcher(message).find() ||
               LEGACY_COLOR_PATTERN.matcher(message).find() ||
               SECTION_COLOR_PATTERN.matcher(message).find() ||
               MINIMESSAGE_PATTERN.matcher(message).find();
    }
    
    /**
     * Processes a message with permission-based color filtering
     * @param message The message to process
     * @param player The player to check permissions for
     * @return Processed message (colored if has permission, plain if not)
     */
    public static String processMessageWithPermission(String message, Player player) {
        if (player != null && !player.hasPermission("nonchat.color")) {
            return stripAllColors(message);
        }
        return parseColor(message);
    }
}

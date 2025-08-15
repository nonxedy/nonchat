package com.nonxedy.nonchat.util.core.colors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;

/**
 * Simple LRU cache implementation
 */
class LRUCache<K,V> {
    private final Map<K,V> cache;
    private final int maxSize;
    
    public LRUCache(int maxSize) {
        this.maxSize = maxSize;
        this.cache = Collections.synchronizedMap(
            new LinkedHashMap<K,V>(maxSize, 0.75f, true) {
                protected boolean removeEldestEntry(Entry<K,V> eldest) {
                    return cache.size() > maxSize;
                }
            });
    }
    
    public V get(K key) {
        return cache.get(key);
    }
    
    public void put(K key, V value) {
        cache.put(key, value);
    }
}

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
    
    // Pattern for matching MiniMessage format tags (more comprehensive)
    private static final Pattern MINIMESSAGE_PATTERN = Pattern.compile("<[^>]+>");
    
    // Pattern for detecting MiniMessage tags more accurately
    private static final Pattern MINIMESSAGE_TAG_PATTERN = Pattern.compile("<(?:[/#]?(?:color|c|gradient|rainbow|bold|b|italic|i|underlined|u|strikethrough|st|obfuscated|obf|reset|r|shadow|hover|click|insertion|font|transition|selector|keybind|translatable|score|nbt|newline|br|lang|key|translate|#[0-9a-fA-F]{6}|[a-z_]+)(?::[^>]*)?|/)>");
    
    // MiniMessage instance for parsing MiniMessage format
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Converts color codes in text to actual colored output
     * Processes both & color codes and hex colors
     * @param message The text containing color codes
     * @return Processed string with color codes converted
     */
    private static final LRUCache<String, String> COLOR_CACHE = new LRUCache<>(1000);
    private static final Map<String, Component> COMPONENT_CACHE = 
        Collections.synchronizedMap(new WeakHashMap<>());

    public static String parseColor(String message) {
        if (message == null) return "";
        
        // Check cache first
        String cached = COLOR_CACHE.get(message);
        if (cached != null) return cached;
        
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 32);

        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + group).toString());
        }
        matcher.appendTail(buffer);
        
        String result = ChatColor.translateAlternateColorCodes('&', buffer.toString());
        COLOR_CACHE.put(message, result);
        return result;
    }

    public static Component parseComponentCached(String message) {
        if (message == null || message.isEmpty()) return Component.empty();
        
        return COMPONENT_CACHE.computeIfAbsent(message, m -> {
            if (containsMiniMessageTags(m)) {
                // Parse with MiniMessage if it contains MiniMessage format tags
                return parseMiniMessageComponent(m);
            } else {
                // Otherwise use legacy format parsing
                String legacyMessage = parseColor(m);
                return LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
            }
        });
    }

    /**
     * Converts color-coded text into Adventure API Component
     * Used for modern Minecraft text rendering
     * @param message The text to convert to Component
     * @return Adventure Component with processed colors
     */
    public static Component parseComponent(String message) {
        if (message == null || message.isEmpty()) return Component.empty();
        
        // Check if the message contains any MiniMessage format tags
        if (containsMiniMessageTags(message)) {
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
        if (message == null || message.isEmpty()) return Component.empty();
        
        try {
            // Check if message contains legacy codes that need conversion
            if (containsLegacyCodes(message)) {
                // Convert legacy codes to MiniMessage format
                String preparedMessage = prepareMixedFormatMessage(message);
                return MINI_MESSAGE.deserialize(preparedMessage);
            } else {
                // Parse directly with MiniMessage
                return MINI_MESSAGE.deserialize(message);
            }
        } catch (Exception e) {
            // Fallback to legacy parsing if MiniMessage parsing fails
            String legacyMessage = parseColor(message);
            return LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
        }
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
        if (message == null || message.isEmpty()) return "";
        
        String result = message;
        
        // Only convert legacy codes if they don't conflict with existing MiniMessage tags
        // First convert hex format from &#RRGGBB to <#RRGGBB> only if not already in MiniMessage format
        if (!result.contains("<#")) {
            Matcher matcher = HEX_PATTERN.matcher(result);
            StringBuffer buffer = new StringBuffer(result.length() + 4 * 8);
            
            while (matcher.find()) {
                String hexColor = matcher.group(1);
                // Replace &#RRGGBB with <#RRGGBB>
                matcher.appendReplacement(buffer, "<#" + hexColor + ">");
            }
            matcher.appendTail(buffer);
            result = buffer.toString();
        }
        
        // Convert legacy color codes to MiniMessage format only if they're not part of existing tags
        // Use more careful replacement to avoid conflicts
        result = replaceLegacyCode(result, "&0", "<black>");
        result = replaceLegacyCode(result, "&1", "<dark_blue>");
        result = replaceLegacyCode(result, "&2", "<dark_green>");
        result = replaceLegacyCode(result, "&3", "<dark_aqua>");
        result = replaceLegacyCode(result, "&4", "<dark_red>");
        result = replaceLegacyCode(result, "&5", "<dark_purple>");
        result = replaceLegacyCode(result, "&6", "<gold>");
        result = replaceLegacyCode(result, "&7", "<gray>");
        result = replaceLegacyCode(result, "&8", "<dark_gray>");
        result = replaceLegacyCode(result, "&9", "<blue>");
        result = replaceLegacyCode(result, "&a", "<green>");
        result = replaceLegacyCode(result, "&b", "<aqua>");
        result = replaceLegacyCode(result, "&c", "<red>");
        result = replaceLegacyCode(result, "&d", "<light_purple>");
        result = replaceLegacyCode(result, "&e", "<yellow>");
        result = replaceLegacyCode(result, "&f", "<white>");
        
        // Convert § codes (section symbol)
        result = replaceLegacyCode(result, "§0", "<black>");
        result = replaceLegacyCode(result, "§1", "<dark_blue>");
        result = replaceLegacyCode(result, "§2", "<dark_green>");
        result = replaceLegacyCode(result, "§3", "<dark_aqua>");
        result = replaceLegacyCode(result, "§4", "<dark_red>");
        result = replaceLegacyCode(result, "§5", "<dark_purple>");
        result = replaceLegacyCode(result, "§6", "<gold>");
        result = replaceLegacyCode(result, "§7", "<gray>");
        result = replaceLegacyCode(result, "§8", "<dark_gray>");
        result = replaceLegacyCode(result, "§9", "<blue>");
        result = replaceLegacyCode(result, "§a", "<green>");
        result = replaceLegacyCode(result, "§b", "<aqua>");
        result = replaceLegacyCode(result, "§c", "<red>");
        result = replaceLegacyCode(result, "§d", "<light_purple>");
        result = replaceLegacyCode(result, "§e", "<yellow>");
        result = replaceLegacyCode(result, "§f", "<white>");
        
        // Format codes with & prefix
        result = replaceLegacyCode(result, "&l", "<bold>");
        result = replaceLegacyCode(result, "&m", "<strikethrough>");
        result = replaceLegacyCode(result, "&n", "<underlined>");
        result = replaceLegacyCode(result, "&o", "<italic>");
        result = replaceLegacyCode(result, "&k", "<obfuscated>");
        result = replaceLegacyCode(result, "&r", "<reset>");
        
        // Format codes with § prefix
        result = replaceLegacyCode(result, "§l", "<bold>");
        result = replaceLegacyCode(result, "§m", "<strikethrough>");
        result = replaceLegacyCode(result, "§n", "<underlined>");
        result = replaceLegacyCode(result, "§o", "<italic>");
        result = replaceLegacyCode(result, "§k", "<obfuscated>");
        result = replaceLegacyCode(result, "§r", "<reset>");
        
        return result;
    }
    
    /**
     * Safely replaces legacy codes without interfering with existing MiniMessage tags
     * @param text The text to process
     * @param legacyCode The legacy code to replace
     * @param miniMessageTag The MiniMessage tag to replace with
     * @return Processed text
     */
    private static String replaceLegacyCode(String text, String legacyCode, String miniMessageTag) {
        if (!text.contains(legacyCode)) return text;
        
        // Simple replacement - in a more sophisticated implementation,
        // we could check if the legacy code is inside existing MiniMessage tags
        return text.replace(legacyCode, miniMessageTag);
    }
    
    /**
     * Checks if a message contains MiniMessage tags using pattern matching
     * @param message The message to check
     * @return true if the message contains MiniMessage tags
     */
    private static boolean containsMiniMessageTags(String message) {
        if (message == null || message.isEmpty()) return false;
        return MINIMESSAGE_TAG_PATTERN.matcher(message).find();
    }
    
    /**
     * Checks if a message contains legacy color codes
     * @param message The message to check
     * @return true if the message contains legacy codes
     */
    private static boolean containsLegacyCodes(String message) {
        if (message == null || message.isEmpty()) return false;
        return HEX_PATTERN.matcher(message).find() ||
               LEGACY_COLOR_PATTERN.matcher(message).find() ||
               SECTION_COLOR_PATTERN.matcher(message).find();
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

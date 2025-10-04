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
                @Override
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
    
    // Pattern for detecting gradient tags specifically
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:[^>]+>");
    
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
                // Use a smarter approach for mixed formats
                // First, try to parse as MiniMessage directly to see if it works
                try {
                    return MINI_MESSAGE.deserialize(message);
                } catch (Exception miniMessageException) {
                    // If MiniMessage parsing fails, it means we have legacy codes that need conversion
                    // Convert legacy codes to MiniMessage format
                    String preparedMessage = prepareMixedFormatMessage(message);
                    return MINI_MESSAGE.deserialize(preparedMessage);
                }
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
        
        // Check if the message already has valid MiniMessage hex colors
        boolean hasMiniMessageHex = result.matches(".*<#[0-9a-fA-F]{6}>.*");
        
        // Only convert legacy hex codes if there are no MiniMessage hex colors present
        // This prevents conflicts between &#RRGGBB and <#RRGGBB> formats
        if (!hasMiniMessageHex) {
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
        
        // For legacy color codes, only convert them if they're not part of existing MiniMessage tags
        // We need to be more careful to avoid conflicts
        result = safelyConvertLegacyColors(result);
        
        return result;
    }
    
    /**
     * Safely converts legacy color codes to MiniMessage format, avoiding conflicts
     * @param message The message to process
     * @return Message with legacy colors converted to MiniMessage format
     */
    private static String safelyConvertLegacyColors(String message) {
        if (message == null || message.isEmpty()) return message;
        
        // Split the message by MiniMessage tags to process text between tags
        String[] parts = message.split("(?=<)|(?<=>)");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            if (part.startsWith("<") && part.endsWith(">")) {
                // This is a MiniMessage tag, keep it as is
                result.append(part);
            } else {
                // This is text between tags, convert legacy codes
                String converted = convertLegacyCodesInText(part);
                result.append(converted);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Converts legacy color codes in plain text (not inside MiniMessage tags)
     * @param text The text to convert
     * @return Text with legacy codes converted to MiniMessage format
     */
    private static String convertLegacyCodesInText(String text) {
        String result = text;
        
        // Convert legacy color codes to MiniMessage format
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
    
    /**
     * Checks if a string contains MiniMessage gradient tags
     * @param message The message to check
     * @return true if contains gradient tags, false otherwise
     */
    public static boolean containsGradient(String message) {
        if (message == null || message.isEmpty()) return false;
        return GRADIENT_PATTERN.matcher(message).find();
    }
    
    /**
     * Enhanced component parsing that prioritizes MiniMessage format for config strings
     * This method is specifically designed for processing config format strings that may contain gradients
     * @param message The text to convert to Component
     * @return Adventure Component with processed colors
     */
    public static Component parseConfigComponent(String message) {
        if (message == null || message.isEmpty()) return Component.empty();
        
        // For config strings, always try MiniMessage first if it contains any MiniMessage-like tags
        // This ensures gradients and other MiniMessage features work properly in config
        if (containsMiniMessageTags(message) || containsGradient(message)) {
            try {
                // Try to parse as MiniMessage first
                return parseMiniMessageComponent(message);
            } catch (Exception e) {
                // Fallback to legacy parsing if MiniMessage parsing fails
                String legacyMessage = parseColor(message);
                return LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
            }
        } else {
            // Use legacy format parsing for traditional color codes
            String legacyMessage = parseColor(message);
            return LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
        }
    }
}

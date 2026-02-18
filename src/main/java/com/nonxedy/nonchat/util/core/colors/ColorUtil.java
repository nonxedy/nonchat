package com.nonxedy.nonchat.util.core.colors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Color;
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
    
    public LRUCache(int maxSize) {
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
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("&[0-9a-fklmnor]");
    private static final Pattern SECTION_COLOR_PATTERN = Pattern.compile("§[0-9a-fklmnor]");
    private static final Pattern MINIMESSAGE_PATTERN = Pattern.compile("<[/#]?[a-zA-Z0-9_:#]+>");
    private static final Pattern MINIMESSAGE_TAG_PATTERN = Pattern.compile("<[/#]?(?:[a-zA-Z_]+|#[0-9a-fA-F]{6})(?::[^>]*)?>");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:[^>]+>");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final LRUCache<String, String> COLOR_CACHE = new LRUCache<>(1000);
    private static final Map<String, Component> COMPONENT_CACHE = 
        Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Converts color codes in text to actual colored output
     * Processes both & color codes and hex colors
     * @param message The text containing color codes
     * @return Processed string with color codes converted
     */
    public static String parseColor(String message) {
        if (message == null) return "";
        
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
        
        return COMPONENT_CACHE.computeIfAbsent(message, m -> parseComponent(m));
    }

    /**
     * Converts color-coded text into Adventure API Component
     * Used for modern Minecraft text rendering
     * @param message The text to convert to Component
     * @return Adventure Component with processed colors
     */
    public static Component parseComponent(String message) {
        if (message == null || message.isEmpty()) return Component.empty();
        
        if (containsMiniMessageTags(message)) {
            return parseMiniMessageComponent(message);
        } else {
            String legacyMessage = parseColor(message);
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
            if (containsLegacyCodes(message)) {
                message = prepareMixedFormatMessage(message);
            }
            return MINI_MESSAGE.deserialize(message);
        } catch (Exception e) {
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
        
        Matcher miniMessageHexMatcher = Pattern.compile("<#[0-9a-fA-F]{6}>").matcher(result);
        boolean hasMiniMessageHex = miniMessageHexMatcher.find();
        
        if (!hasMiniMessageHex) {
            Matcher matcher = HEX_PATTERN.matcher(result);
            StringBuffer buffer = new StringBuffer(result.length() + 4 * 8);
            
            while (matcher.find()) {
                String hexColor = matcher.group(1);
                matcher.appendReplacement(buffer, "<#" + hexColor + ">");
            }
            matcher.appendTail(buffer);
            result = buffer.toString();
        }
        
        result = safelyConvertLegacyColors(result);
        
        return result;
    }
    
    private static String safelyConvertLegacyColors(String message) {
        if (message == null || message.isEmpty()) return message;
        
        StringBuilder result = new StringBuilder();
        int i = 0;
        int len = message.length();
        
        while (i < len) {
            if (message.charAt(i) == '<') {
                int endTag = message.indexOf('>', i);
                if (endTag != -1) {
                    result.append(message, i, endTag + 1);
                    i = endTag + 1;
                    continue;
                }
            }
            
            if (i < len - 1 && message.charAt(i) == '&') {
                char code = message.charAt(i + 1);
                String miniMessageTag = convertLegacyCodeToMiniMessage(code);
                if (miniMessageTag != null) {
                    result.append(miniMessageTag);
                    i += 2;
                    continue;
                }
            }
            
            result.append(message.charAt(i));
            i++;
        }
        
        return result.toString();
    }
    
    private static String convertLegacyCodeToMiniMessage(char code) {
        switch (code) {
            case '0': return "<black>";
            case '1': return "<dark_blue>";
            case '2': return "<dark_green>";
            case '3': return "<dark_aqua>";
            case '4': return "<dark_red>";
            case '5': return "<dark_purple>";
            case '6': return "<gold>";
            case '7': return "<gray>";
            case '8': return "<dark_gray>";
            case '9': return "<blue>";
            case 'a': case 'A': return "<green>";
            case 'b': case 'B': return "<aqua>";
            case 'c': case 'C': return "<red>";
            case 'd': case 'D': return "<light_purple>";
            case 'e': case 'E': return "<yellow>";
            case 'f': case 'F': return "<white>";
            case 'k': case 'K': return "<obfuscated>";
            case 'l': case 'L': return "<bold>";
            case 'm': case 'M': return "<strikethrough>";
            case 'n': case 'N': return "<underlined>";
            case 'o': case 'O': return "<italic>";
            case 'r': case 'R': return "<reset>";
            default: return null;
        }
    }
    
    /**
     * Checks if a message contains MiniMessage tags using pattern matching
     * @param message The message to check
     * @return true if the message contains MiniMessage tags
     */
    public static boolean containsMiniMessageTags(String message) {
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
        
        result = HEX_PATTERN.matcher(result).replaceAll("");
        result = LEGACY_COLOR_PATTERN.matcher(result).replaceAll("");
        result = SECTION_COLOR_PATTERN.matcher(result).replaceAll("");
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

        if (containsMiniMessageTags(message) || containsGradient(message)) {
            try {
                return parseMiniMessageComponent(message);
            } catch (Exception e) {
                String legacyMessage = parseColor(message);
                return LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
            }
        } else {
            String legacyMessage = parseColor(message);
            return LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
        }
    }

    /**
     * Converts a hex color string to a Bukkit Color object
     * Supports formats like "#RRGGBB", "#RGB", or "RRGGBB"
     * @param hexColor The hex color string
     * @return Bukkit Color object, or Color.BLACK if parsing fails
     */
    public static Color parseHexColor(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return Color.BLACK;
        }

        try {
            String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;

            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0) +
                      hex.charAt(1) + hex.charAt(1) +
                      hex.charAt(2) + hex.charAt(2);
            }

            if (hex.length() != 6) {
                return Color.BLACK;
            }

            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            return Color.fromRGB(r, g, b);
        } catch (IllegalArgumentException e) {
            return Color.BLACK;
        }
    }
}
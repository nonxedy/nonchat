package com.nonxedy.nonchat.util;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for detecting and replacing ping placeholders in chat messages
 * Similar to ItemDetector but for [ping] placeholders
 */
public class PingDetector {
    // Pattern to match the [ping] placeholder
    private static final Pattern PING_PATTERN = Pattern.compile("\\[ping\\]", Pattern.CASE_INSENSITIVE);
    
    /**
     * Scans the text for [ping] placeholders and replaces them with 
     * colored ping values
     *
     * @param player The player whose ping to check
     * @param text The message text to process
     * @return Component with ping placeholders converted to colored values
     */
    public static Component processPingPlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return Component.text(text);
        }
        
        TextComponent.Builder builder = Component.text().content("");
        Matcher matcher = PING_PATTERN.matcher(text);
        int lastEnd = 0;
        boolean foundPing = false;
        
        while (matcher.find()) {
            // Add text before the placeholder
            if (matcher.start() > lastEnd) {
                String textBefore = text.substring(lastEnd, matcher.start());
                builder.append(Component.text(textBefore));
            }
            
            // Get player's ping
            int ping = player.getPing();
            
            // Determine color based on ping
            NamedTextColor color;
            if (ping < 100) {
                color = NamedTextColor.GREEN; // Good
            } else if (ping < 300) {
                color = NamedTextColor.GOLD; // Medium
            } else {
                color = NamedTextColor.RED; // Bad
            }
            
            // Create ping component with appropriate color
            Component pingComponent = Component.text(ping + "ms").color(color);
            builder.append(pingComponent);
            
            lastEnd = matcher.end();
            foundPing = true;
        }
        
        // Add remaining text
        if (lastEnd < text.length()) {
            String remainingText = text.substring(lastEnd);
            builder.append(Component.text(remainingText));
        }
        
        return foundPing ? builder.build() : Component.text(text);
    }
}

package com.nonxedy.nonchat.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates hoverable text components with player information
 * Handles formatting and placeholder replacement for hover text
 */
public class HoverTextUtil {
    /** Format template for hover text display */
    private final List<String> hoverFormat;
    private final boolean enabled;
    private final boolean usePlaceholderAPI;

    // Constructor to initialize hover text format
    public HoverTextUtil(List<String> format, boolean enabled) {
        this.hoverFormat = format;
        this.enabled = enabled;
        this.usePlaceholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /**
     * Creates a hoverable component from player name with additional information
     * @param player The player to create hover text for
     * @param originalText The original text to make hoverable
     * @return Component with hover event containing player information
     */
    public Component createHoverablePlayerName(Player player, String originalText) {
        if (!enabled) {
            return ColorUtil.parseComponent(originalText);
        }

        String hoverText = hoverFormat.stream()
            .map(line -> processLine(line, player))
            .collect(Collectors.joining("\n"));

        return ColorUtil.parseComponent(originalText)
            .hoverEvent(HoverEvent.showText(ColorUtil.parseComponent(hoverText)));
    }

    private String processLine(String text, Player player) {
        if (text == null) return "";
        
        String prefix = IntegrationUtil.getPlayerPrefix(player);
        String balance = IntegrationUtil.getBalance(player);
        String playtime = IntegrationUtil.getPlayTime(player);
        
        String processed = text
            .replace("{player}", player.getName())
            .replace("{level}", String.valueOf(player.getLevel()))
            .replace("{prefix}", prefix != null ? prefix : "")
            .replace("{playtime}", playtime != null ? playtime : "0h")
            .replace("{balance}", balance != null ? balance : "0");
    
        if (usePlaceholderAPI) {
            try {
                processed = PlaceholderAPI.setPlaceholders(player, processed);
            } catch (Exception e) {
                // Log the error but don't crash
                Bukkit.getLogger().warning("[nonchat] Error processing placeholder: " + e.getMessage());
            }
        }
    
        return ColorUtil.parseColor(processed);
    }
    
}

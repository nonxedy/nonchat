package com.nonxedy.nonchat.placeholders;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.utils.ColorUtil;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * Provides PlaceholderAPI integration for nonchat plugin
 * Handles custom placeholders for chat features
 */
public class NonchatExpansion extends PlaceholderExpansion {

    private final nonchat plugin;

    // Constructor initializes plugin reference
    public NonchatExpansion(nonchat plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets expansion author name
     * @return Author identifier
     */
    @Override
    public String getAuthor() {
        return "nonxedy";
    }

    /**
     * Gets expansion identifier for PlaceholderAPI
     * @return Expansion identifier string
     */
    @Override
    public String getIdentifier() {
        return "nonchat";
    }

    /**
     * Gets expansion version from plugin
     * @return Current version string
     */
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Processes placeholder requests and returns values
     * @param player Player requesting the placeholder
     * @param identifier Placeholder identifier string
     * @return Processed placeholder value or null if invalid
     */
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
    
        switch (identifier) {
            case "spy_enabled":
                return plugin.getSpyCommand().isSpying(player) ? 
                ColorUtil.parseColor(plugin.getPluginMessages().getString("placeholders.spy.enabled")) :
                ColorUtil.parseColor(plugin.getPluginMessages().getString("placeholders.spy.disabled"));
                
            case "bubble_enabled":
                return plugin.getConfig().getBoolean("chat-bubbles.enabled") ?
                ColorUtil.parseColor(plugin.getPluginMessages().getString("placeholders.bubble.enabled")) :
                ColorUtil.parseColor(plugin.getPluginMessages().getString("placeholders.bubble.disabled"));
                
            case "caps_filter_enabled":
                return plugin.getPluginConfig().getCapsFilter().isEnabled() ?
                ColorUtil.parseColor(plugin.getPluginMessages().getString("placeholders.caps-filter.enabled")) :
                ColorUtil.parseColor(plugin.getPluginMessages().getString("placeholders.caps-filter.disabled"));
                
            case "ignored":
                return !plugin.getIgnoreManager().getIgnoredPlayers(player).isEmpty() ? 
                ColorUtil.parseColor(plugin.getPluginMessages().getString("placeholders.ignored.true")) :
                ColorUtil.parseColor(plugin.getPluginMessages().getString("placeholders.ignored.false"));
                
            default:
                return null;
        }
    }
}
package com.nonxedy.nonchat.placeholders;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.utils.ColorUtil;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class NonchatExpansion extends PlaceholderExpansion {

    private final nonchat plugin;

    public NonchatExpansion(nonchat plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getAuthor() {
        return "nonxedy";
    }

    @Override
    public String getIdentifier() {
        return "nonchat";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

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
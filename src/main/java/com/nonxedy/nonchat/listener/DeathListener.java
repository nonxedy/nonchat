package com.nonxedy.nonchat.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

/**
 * Handles player death events and customizes death messages
 */
public class DeathListener implements Listener {

    // Store plugin configuration instance
    private final PluginConfig config;

    // Constructor to initialize the listener with config
    public DeathListener(PluginConfig config) {
        this.config = config;
    }
    
    /**
     * Processes player death events and formats messages
     * Uses HIGH priority to modify messages after other plugins
     * @param event The player death event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Only process if custom death messages are enabled
        if (!config.isDeathMessagesEnabled()) {
            return; // Keep vanilla death message
        }
        
        // Get the player who died
        Player player = event.getEntity();
        
        // Get death format from config
        String deathFormat = config.getDeathFormat();
        if (deathFormat == null) {
            return;
        }

        // Apply PlaceholderAPI if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                deathFormat = PlaceholderAPI.setPlaceholders(player, deathFormat);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[nonchat] Error processing death message placeholders: " + e.getMessage());
            }
        }

        // Set the formatted death message with color support
        event.deathMessage(ColorUtil.parseComponent(deathFormat));
    }
}

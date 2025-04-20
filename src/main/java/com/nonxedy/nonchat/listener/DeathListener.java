package com.nonxedy.nonchat.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.util.ColorUtil;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

/**
 * Handles player death events and customizes death messages
 * Integrates with LuckPerms for prefix/suffix support
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
        // Get the player who died
        Player player = event.getEntity();
        // Get LuckPerms user data for the player
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        
        // Return if user is null or death format is not configured
        if (user == null || config.getDeathFormat() == null) {
            return;
        }

        // Get prefix and suffix from LuckPerms metadata
        String prefix = user.getCachedData().getMetaData().getPrefix();
        String suffix = user.getCachedData().getMetaData().getSuffix();

        // Create custom death message by replacing placeholders
        String deathMessage = config.getDeathFormat()
            // Replace {prefix} with player's prefix or empty string if null
            .replace("{prefix}", prefix != null ? prefix : "")
            // Replace {suffix} with player's suffix or empty string if null
            .replace("{suffix}", suffix != null ? suffix : "")
            // Replace {player} with player's name
            .replace("{player}", player.getName());

        // Set the formatted death message with color support
        event.deathMessage(ColorUtil.parseComponent(deathMessage));
    }
}

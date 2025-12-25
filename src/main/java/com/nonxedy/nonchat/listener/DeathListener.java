package com.nonxedy.nonchat.listener;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.service.DeathMessageService;

/**
 * Handles player death events and customizes death messages
 */
public class DeathListener implements Listener {

    // Store plugin configuration instance
    private final PluginConfig config;
    
    // Death message service for custom death messages by cause
    private final DeathMessageService deathMessageService;

    // Constructor to initialize the listener with config
    public DeathListener(PluginConfig config) {
        this.config = config;
        this.deathMessageService = null;
    }
    
    // Constructor with death message service support
    public DeathListener(PluginConfig config, DeathMessageService deathMessageService) {
        this.config = config;
        this.deathMessageService = deathMessageService;
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

        // Use new deaths.yml system
        if (deathMessageService != null) {
            try {
                // Delegate everything to the service (message + actions)
                deathMessageService.handleDeath(event);
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Error handling death with deaths.yml system: {0}", e.getMessage());
                // Keep vanilla death message on error
            }
        }
    }

}

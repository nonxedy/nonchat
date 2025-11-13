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
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;

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
        
        // Priority 1: Use new deaths.yml system if service is available
        if (deathMessageService != null) {
            try {
                // Delegate everything to the service (message + actions)
                deathMessageService.handleDeath(event);
                
                // If the service set a message, we're done
                if (event.deathMessage() != null && !event.deathMessage().equals(Component.empty())) {
                    return;
                }
                // If service didn't set a message, fall through to legacy system
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Error handling death with deaths.yml system: {0}", e.getMessage());
                // Fall through to legacy system
            }
        }
        
        // Priority 2: Legacy fallback - Use config.yml death.format (deprecated)
        // This maintains backward compatibility for users who haven't migrated to deaths.yml
        if (!config.isDeathMessagesEnabled()) {
            return; // Keep vanilla death message if legacy system is also disabled
        }
        
        String deathFormat = config.getDeathFormat();
        if (deathFormat == null || deathFormat.isEmpty()) {
            return;
        }

        // Log deprecation warning once per server start
        logDeprecationWarning();

        // Apply PlaceholderAPI if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                deathFormat = PlaceholderAPI.setPlaceholders(player, deathFormat);
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Error processing death message placeholders: {0}", e.getMessage());
            }
        }

        // Set the formatted death message with color support
        event.deathMessage(ColorUtil.parseComponent(deathFormat));
    }
    
    // Track if deprecation warning has been shown
    private static boolean deprecationWarningShown = false;
    
    /**
     * Logs a deprecation warning for the old config.yml death message system
     */
    private void logDeprecationWarning() {
        if (!deprecationWarningShown) {
            Bukkit.getLogger().log(Level.WARNING, 
                "[nonchat] DEPRECATION WARNING: The death message configuration in config.yml is deprecated.");
            Bukkit.getLogger().log(Level.WARNING, 
                "[nonchat] Please migrate to the new deaths.yml file for more features and better customization.");
            Bukkit.getLogger().log(Level.WARNING, 
                "[nonchat] The old system will be removed in a future version.");
            deprecationWarningShown = true;
        }
    }
}

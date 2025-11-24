package com.nonxedy.nonchat.listener;

import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nonxedy.nonchat.config.DeathConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

/**
 * Handles death location tracking and messaging
 * Provides players with their death coordinates
 */
public class DeathCoordinates implements Listener {
    
    private final DeathConfig deathConfig;
    private final PluginMessages messages;
    
    public DeathCoordinates(DeathConfig deathConfig, PluginMessages messages) {
        this.deathConfig = deathConfig;
        this.messages = messages;
    }
    
    /**
     * Handles player death events to display death coordinates
     * Runs with HIGH priority after vanilla message generation
     * @param event Death event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // This listener now ONLY handles coordinates, not death messages
        Player player = event.getPlayer();
        Location deathLoc = player.getLocation();
        Environment dimension = deathLoc.getWorld().getEnvironment();
        
        // Only send coordinates if enabled in deaths.yml config
        if (deathConfig.showCoordinates()) {
            String coordsMessage = String.format(
                messages.getString("death-coordinates"),
                formatDimension(dimension),
                deathLoc.getBlockX(),
                deathLoc.getBlockY(),
                deathLoc.getBlockZ()
            );
            
            player.sendMessage(ColorUtil.parseComponent(coordsMessage));
        }
    }
    
    /**
     * Converts dimension enum to readable name using localized messages
     * @param dimension World environment type
     * @return Localized dimension name
     */
    private String formatDimension(Environment dimension) {
        return switch (dimension) {
            case NORMAL -> messages.getString("dimension-overworld");
            case NETHER -> messages.getString("dimension-nether");
            case THE_END -> messages.getString("dimension-end");
            default -> dimension.toString();
        };
    }
}

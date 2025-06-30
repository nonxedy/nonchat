package com.nonxedy.nonchat.listener;

import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

/**
 * Handles death location tracking and messaging
 * Provides players with their death coordinates
 */
public class DeathCoordinates implements Listener {
    
    private final PluginConfig config;
    
    public DeathCoordinates(PluginConfig config) {
        this.config = config;
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
        
        // Only send coordinates if enabled in config
        if (config.isShowDeathCoordinatesEnabled()) {
            String coordsMessage = String.format(
                "&fYou died in &d%s &fat coordinates: &fx:&d%d &fy:&d%d &fz:&d%d",
                formatDimension(dimension),
                deathLoc.getBlockX(),
                deathLoc.getBlockY(),
                deathLoc.getBlockZ()
            );
            
            player.sendMessage(ColorUtil.parseComponent(coordsMessage));
        }
    }
    
    /**
     * Converts dimension enum to readable name
     * @param dimension World environment type
     * @return Formatted dimension name
     */
    private String formatDimension(Environment dimension) {
        switch (dimension) {
            case NORMAL: return "Overworld";
            case NETHER: return "Nether";
            case THE_END: return "The End";
            default: return dimension.toString();
        }
    }
}

package com.nonxedy.nonchat.listeners;

import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.utils.ColorUtil;

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
     * Handles player death events
     * @param event Death event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Location deathLoc = player.getLocation();
        Environment dimension = deathLoc.getWorld().getEnvironment();
        
        event.deathMessage(ColorUtil.parseComponent(config.getDeathFormat()
            .replace("{prefix}", "")
            .replace("{player}", player.getName())
            .replace("{suffix}", "")));
            
        String coordsMessage = String.format(
            "&fYou died in &d%s &fat coordinates: &fx:&d%d &fy:&d%d &fz:&d%d",
            formatDimension(dimension),
            deathLoc.getBlockX(),
            deathLoc.getBlockY(),
            deathLoc.getBlockZ()
        );
        
        player.sendMessage(ColorUtil.parseComponent(coordsMessage));
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
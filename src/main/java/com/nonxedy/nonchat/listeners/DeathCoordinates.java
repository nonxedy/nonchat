package com.nonxedy.nonchat.listeners;

import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nonxedy.nonchat.config.PluginConfig;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * Handles death location messages and coordinates display
 * Provides dimension-specific location information on player death
 */
public class DeathCoordinates implements Listener {
    
    // Define constant colors for message formatting
    
    // I'll rework work with colors
    private static final TextColor WHITE = TextColor.fromHexString("#FFFFFF");
    private static final TextColor PURPLE = TextColor.fromHexString("#E088FF");
    
    // Store plugin configuration
    private final PluginConfig config;
    
    // Constructor to initialize config
    public DeathCoordinates(PluginConfig config) {
        this.config = config;
    }
    
    /**
     * Processes death events and sends coordinate information
     * @param event The player death event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Get the player who died
        Player player = event.getPlayer();
        // Get the location where player died
        Location deathLoc = player.getLocation();
        // Get the dimension where death occurred
        Environment dimension = deathLoc.getWorld().getEnvironment();
        
        // Configure death message using format from config
        event.deathMessage(Component.text(config.getDeathFormat()
            .replace("{prefix}", "")
            .replace("{player}", player.getName())
            .replace("{suffix}", "")));
            
        // Build and send death coordinates message to the player
        Component coordsMessage = buildDeathMessage(dimension, deathLoc);
        player.sendMessage(coordsMessage);
    }
    
    /**
     * Constructs formatted death location message
     * @param dimension World dimension where death occurred
     * @param loc Location of player death
     * @return Formatted component with death coordinates
     */
    private Component buildDeathMessage(Environment dimension, Location loc) {
        return Component.text()
            .append(Component.text("You died in ", WHITE))
            .append(Component.text(formatDimension(dimension), PURPLE))
            .append(Component.text(" at coordinates: ", WHITE))
            .append(formatCoordinate("x", loc.getBlockX()))
            .append(formatCoordinate("y", loc.getBlockY()))
            .append(formatCoordinate("z", loc.getBlockZ()))
            .build();
    }
    
    /**
     * Formats individual coordinate components
     * @param axis Coordinate axis label (x/y/z)
     * @param value Coordinate value
     * @return Formatted coordinate component
     */
    private Component formatCoordinate(String axis, int value) {
        return Component.text()
            .append(Component.text(" " + axis + ":", WHITE))
            .append(Component.text(String.valueOf(value), PURPLE))
            .build();
    }
    
    /**
     * Converts dimension type to readable name
     * @param dimension Environment type to format
     * @return Human-readable dimension name
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

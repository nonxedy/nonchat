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

public class DeathCoordinates implements Listener {
    
    private static final TextColor WHITE = TextColor.fromHexString("#FFFFFF");
    private static final TextColor PURPLE = TextColor.fromHexString("#E088FF");
    private final PluginConfig config;
    
    public DeathCoordinates(PluginConfig config) {
        this.config = config;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Location deathLoc = player.getLocation();
        Environment dimension = deathLoc.getWorld().getEnvironment();
        
        // Set the death message format from config
        event.deathMessage(Component.text(config.getDeathFormat()
            .replace("{prefix}", "")
            .replace("{player}", player.getName())
            .replace("{suffix}", "")));
            
        // Send coordinates message to player
        Component coordsMessage = buildDeathMessage(dimension, deathLoc);
        player.sendMessage(coordsMessage);
    }
    
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
    
    private Component formatCoordinate(String axis, int value) {
        return Component.text()
            .append(Component.text(" " + axis + ":", WHITE))
            .append(Component.text(String.valueOf(value), PURPLE))
            .build();
    }
    
    private String formatDimension(Environment dimension) {
        switch (dimension) {
            case NORMAL: return "Overworld";
            case NETHER: return "Nether";
            case THE_END: return "The End";
            default: return dimension.toString();
        }
    }
}

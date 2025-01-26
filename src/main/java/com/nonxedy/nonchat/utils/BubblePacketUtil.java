package com.nonxedy.nonchat.utils;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

// Utility class for managing chat bubble armor stands
public class BubblePacketUtil {
    
    // Creates and spawns a new chat bubble armor stand
    public static ArmorStand spawnBubble(Player player, String text, Location location) {
        // Spawn new armor stand entity at specified location
        ArmorStand bubble = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        
        // Set the chat text as custom name with color support
        bubble.setCustomName(ColorUtil.parseColor(text));
        // Make the custom name visible
        bubble.setCustomNameVisible(true);
        // Make armor stand invisible, only showing text
        bubble.setInvisible(true);
        // Disable gravity effects
        bubble.setGravity(false);
        // Set as marker to prevent interaction
        bubble.setMarker(true);
        // Use small armor stand model
        bubble.setSmall(true);
        
        return bubble;
    }
    
    // Removes a chat bubble armor stand if it exists
    public static void removeBubble(ArmorStand bubble) {
        // Check if bubble exists and is not already removed
        if (bubble != null && !bubble.isDead()) {
            // Remove the armor stand entity
            bubble.remove();
        }
    }

    // Updates the location of an existing chat bubble
    public static void updateBubbleLocation(ArmorStand bubble, Location location) {
        // Check if bubble exists and is not removed
        if (bubble != null && !bubble.isDead()) {
            // Teleport armor stand to new location
            bubble.teleport(location);
        }
    }
}

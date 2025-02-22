package com.nonxedy.nonchat.utils;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Manages chat bubble display using armor stands
 * Handles creation, removal and updating of floating text displays
 */
public class BubblePacketUtil {
    
    /**
     * Creates and configures a new chat bubble armor stand
     * @param player The player to create bubble for
     * @param text The text to display in bubble
     * @param location The location to spawn bubble at
     * @return Configured armor stand entity
     */
    public static ArmorStand spawnBubble(Player player, String text, Location location) {
        ArmorStand bubble = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        
        bubble.setCustomName(ColorUtil.parseColor(text));
        bubble.setCustomNameVisible(true);
        bubble.setInvisible(true);
        bubble.setGravity(false);
        bubble.setMarker(true);
        bubble.setSmall(true);
        
        return bubble;
    }
    
    /**
     * Removes a chat bubble from the world
     * @param bubble The armor stand to remove
     */
    public static void removeBubble(ArmorStand bubble) {
        if (bubble != null && !bubble.isDead()) {
            bubble.remove();
        }
    }

    /**
     * Updates the position of an existing chat bubble
     * @param bubble The armor stand to move
     * @param location New location for the bubble
     */
    public static void updateBubbleLocation(ArmorStand bubble, Location location) {
        if (bubble != null && !bubble.isDead()) {
            bubble.teleport(location);
        }
    }
}

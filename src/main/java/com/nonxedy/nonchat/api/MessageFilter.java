package com.nonxedy.nonchat.api;

import org.bukkit.entity.Player;

/**
 * Interface for filtering messages in chat channels.
 * Implementations can determine if a message should be filtered out
 * and provide a reason for the filtering.
 */
public interface MessageFilter {
    
    /**
     * Determines if a message should be filtered.
     * 
     * @param player The player who sent the message
     * @param message The message to check
     * @return true if the message should be filtered out, false if it should be allowed
     */
    boolean shouldFilter(Player player, String message);
    
    /**
     * Gets the reason why the message was filtered.
     * This message can be displayed to the player when their message is blocked.
     * 
     * @return The reason for filtering the message
     */
    default String getReason() {
        return "Your message was blocked by a filter";
    }
    
    /**
     * Gets the filter name for identification.
     * 
     * @return Filter name
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Checks if the filter is enabled.
     * 
     * @return true if filter is enabled
     */
    default boolean isEnabled() {
        return true;
    }
}

package com.nonxedy.nonchat.api;

import org.bukkit.entity.Player;

/**
 * Interface for filtering messages in a channel.
 * Implementations can determine if a message should be filtered out.
 */
@FunctionalInterface
public interface MessageFilter {
    
    /**
     * Determine if a message should be filtered.
     * 
     * @param player The player who sent the message
     * @param message The message to check
     * @return true if the message should be filtered out, false if it should be allowed
     */
    boolean shouldFilter(Player player, String message);
}

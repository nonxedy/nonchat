package com.nonxedy.nonchat.api;

import org.bukkit.entity.Player;

/**
 * Interface for processing messages in a channel.
 * Implementations can modify or cancel messages sent in a channel.
 */
@FunctionalInterface
public interface MessageProcessor {
    
    /**
     * Process a message sent by a player in a channel.
     * 
     * @param player The player who sent the message
     * @param message The original message
     * @return The processed message, or null to cancel the message
     */
    String process(Player player, String message);
}

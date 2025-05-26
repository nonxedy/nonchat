package com.nonxedy.nonchat.api;

import org.bukkit.entity.Player;

/**
 * Interface for processing chat messages.
 * Implementations can modify messages before they are sent to chat.
 */
public interface MessageProcessor {
    
    /**
     * Processes a chat message.
     * 
     * @param player The player who sent the message
     * @param message The original message
     * @return The processed message
     */
    String process(Player player, String message);
    
    /**
     * Gets the processor name for identification.
     * 
     * @return Processor name
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Checks if the processor is enabled.
     * 
     * @return true if processor is enabled
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * Gets processor priority (lower = higher priority).
     * 
     * @return Priority value
     */
    default int getPriority() {
        return 0;
    }
}

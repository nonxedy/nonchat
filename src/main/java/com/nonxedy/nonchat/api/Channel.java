package com.nonxedy.nonchat.api;

import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;

/**
 * Represents a chat channel in the NonChat plugin.
 * Each channel has its own properties like format, permission, etc.
 */
public interface Channel {
    
    /**
     * Gets the unique identifier of this channel.
     * @return Channel ID.
     */
    String getId();
    
    /**
     * Gets the display name of this channel.
     * @return Display name.
     */
    String getDisplayName();
    
    /**
     * Gets the message format for this channel.
     * @return Message format.
     */
    String getFormat();
    
    /**
     * Gets the character that triggers this channel.
     * @return Trigger character.
     */
    char getCharacter();
    
    /**
     * Checks if this channel has a trigger character.
     * @return True if the channel has a trigger character.
     */
    boolean hasTriggerCharacter();
    
    /**
     * Checks if this channel is enabled.
     * @return True if enabled.
     */
    boolean isEnabled();
    
    /**
     * Gets the permission required to send messages to this channel.
     * @return Permission string.
     */
    String getSendPermission();
    
    /**
     * Gets the permission required to receive messages from this channel.
     * @return Permission string.
     */
    String getReceivePermission();
    
    /**
     * Gets the radius for local chat channels. -1 means global.
     * @return Radius value.
     */
    int getRadius();
    
    /**
     * Checks if this is a global channel.
     * @return True if global, false if local.
     */
    boolean isGlobal();
    
    /**
     * Determines if a player can send messages to this channel.
     * @param player The player to check.
     * @return True if player can send messages.
     */
    boolean canSend(Player player);
    
    /**
     * Determines if a player can receive messages from this channel.
     * @param player The player to check.
     * @return True if player can receive messages.
     */
    boolean canReceive(Player player);
    
    /**
     * Determines if a recipient is in range to receive a message from the sender.
     * @param sender Message sender
     * @param recipient Message recipient
     * @return True if in range
     */
    boolean isInRange(Player sender, Player recipient);
    
    /**
     * Sets whether this channel is enabled.
     * @param enabled New enabled state
     */
    void setEnabled(boolean enabled);
    
    /**
     * Process a message and return the formatted component.
     * @param player The sender
     * @param message The raw message
     * @return Formatted message component
     */
    Component formatMessage(Player player, String message);
    
    /**
     * Gets the cooldown for this channel in seconds.
     * @return Cooldown in seconds
     */
    int getCooldown();
    
    /**
     * Gets the minimum message length for this channel.
     * @return Minimum length
     */
    int getMinLength();
    
    /**
     * Gets the maximum message length for this channel.
     * @return Maximum length
     */
    int getMaxLength();
}

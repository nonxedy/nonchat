package com.nonxedy.nonchat.util;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;

public class MessageDeliveryTracker {
    private final PluginMessages messages;
    private final PluginConfig config;

    public MessageDeliveryTracker(PluginMessages messages, PluginConfig config) {
        this.messages = messages;
        this.config = config;
    }

    /**
     * Checks if a message would be delivered to any recipients
     * @param sender The player sending the message
     * @param chatType The chat channel being used
     * @return true if message would be delivered, false otherwise
     */
    public boolean checkMessageDelivery(Player sender, ChatTypeUtil chatType) {
        // Check if delivery notifications are enabled
        if (!config.isMessageDeliveryNotificationEnabled()) {
            return true; // Don't block message, just don't notify
        }
        
        int recipientCount = countRecipients(sender, chatType);
        
        if (recipientCount == 0) {
            // Send notification to sender that message was not delivered
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("message-not-delivered")));
            return false;
        }
        
        return true;
    }

    /**
     * Counts the number of players who would receive the message
     * @param sender The player sending the message
     * @param chatType The chat channel being used
     * @return Number of potential recipients
     */
    private int countRecipients(Player sender, ChatTypeUtil chatType) {
        int count = 0;
        List<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers().stream().toList();
        
        // If no players online except sender, return 0
        if (onlinePlayers.size() <= 1) {
            return 0;
        }
        
        for (Player player : onlinePlayers) {
            // Skip the sender
            if (player.equals(sender)) {
                continue;
            }
            
            // Check if player can receive messages in this channel
            if (!canReceiveMessage(player, chatType)) {
                continue;
            }
            
            // Check radius for local channels
            if (chatType.getRadius() > 0) {
                if (!isInRange(sender, player, chatType.getRadius())) {
                    continue;
                }
            }
            
            count++;
        }
        
        return count;
    }

    /**
     * Checks if a player can receive messages in the given chat type
     * @param player The player to check
     * @param chatType The chat channel
     * @return true if player can receive messages
     */
    private boolean canReceiveMessage(Player player, ChatTypeUtil chatType) {
        // Check receive permission if specified
        String receivePermission = chatType.getReceivePermission();
        if (receivePermission != null && !receivePermission.isEmpty()) {
            return player.hasPermission(receivePermission);
        }
        
        // If no specific receive permission, check if channel is enabled
        return chatType.isEnabled();
    }

    /**
     * Checks if two players are within the specified range
     * @param sender The sending player
     * @param receiver The receiving player
     * @param radius The maximum distance
     * @return true if players are in range
     */
    private boolean isInRange(Player sender, Player receiver, int radius) {
        // Check if players are in the same world
        if (!sender.getWorld().equals(receiver.getWorld())) {
            return false;
        }
        
        Location senderLoc = sender.getLocation();
        Location receiverLoc = receiver.getLocation();
        
        // Calculate distance and check if within radius
        double distance = senderLoc.distance(receiverLoc);
        return distance <= radius;
    }
}

package com.nonxedy.nonchat.api;

import org.bukkit.entity.Player;

import com.nonxedy.nonchat.nonchat;

import java.util.Collection;

public class ChannelAPI {
    private static nonchat plugin;

    public static void initialize(nonchat instance) {
        plugin = instance;
    }

    /**
     * Gets all available chat channels
     * @return Collection of all channels
     */
    public static Collection<Channel> getAllChannels() {
        return plugin.getChatManager().getChannelManager().getAllChannels();
    }

    /**
     * Gets a channel by its ID
     * @param channelId The channel ID
     * @return The channel, or null if not found
     */
    public static Channel getChannel(String channelId) {
        return plugin.getChatManager().getChannelManager().getChannel(channelId);
    }

    /**
     * Gets the current channel of a player
     * @param player The player
     * @return The player's current channel
     */
    public static Channel getPlayerChannel(Player player) {
        return plugin.getChatManager().getPlayerChannel(player);
    }

    /**
     * Checks if a message is meant for a specific channel
     * @param message The message to check
     * @param channelId The channel ID to check against
     * @return true if the message belongs to the channel
     */
    public static boolean isMessageForChannel(String message, String channelId) {
        Channel channel = getChannel(channelId);
        if (channel == null || !channel.isEnabled() || !channel.hasTriggerCharacter()) {
            return false;
        }
        
        return message.startsWith(String.valueOf(channel.getCharacter()));
    }
}
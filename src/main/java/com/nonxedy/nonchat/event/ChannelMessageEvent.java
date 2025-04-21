package com.nonxedy.nonchat.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.api.Channel;

/**
 * Event fired when a message is sent to a channel.
 * This event is used for integrations with external chat systems like DiscordSRV.
 */
public class ChannelMessageEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player sender;
    private final Channel channel;
    private final String message;
    private final String discordChannelId;
    
    public ChannelMessageEvent(Player sender, Channel channel, String message, String discordChannelId) {
        this.sender = sender;
        this.channel = channel;
        this.message = message;
        this.discordChannelId = discordChannelId;
    }
    
    /**
     * Gets the player who sent the message.
     * @return The sender
     */
    public Player getSender() {
        return sender;
    }
    
    /**
     * Gets the channel the message was sent to.
     * @return The channel
     */
    public Channel getChannel() {
        return channel;
    }
    
    /**
     * Gets the message content.
     * @return The message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the Discord channel ID associated with this channel.
     * @return The Discord channel ID for the channel
     */
    public String getDiscordChannelId() {
        return discordChannelId;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

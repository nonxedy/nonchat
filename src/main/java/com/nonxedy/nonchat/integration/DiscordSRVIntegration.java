package com.nonxedy.nonchat.integration;

import org.bukkit.entity.Player;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.api.Channel;
import com.nonxedy.nonchat.api.ChannelAPI;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;

public class DiscordSRVIntegration {
    private final Nonchat plugin;
    
    public DiscordSRVIntegration(Nonchat plugin) {
        this.plugin = plugin;
        DiscordSRV.api.subscribe(this);
    }

    public void unregister() {
        DiscordSRV.api.unsubscribe(this);
    }

    @Subscribe
    public void onGameChatMessagePreProcess(GameChatMessagePreProcessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Get the player's current channel
        Channel playerChannel = ChannelAPI.getPlayerChannel(player);
        
        // Check if the message starts with a channel prefix
        for (Channel channel : ChannelAPI.getAllChannels()) {
            if (channel.hasPrefix() && message.startsWith(channel.getPrefix())) {
                // Message is for a specific channel
                if (!channel.getId().equals(event.getChannel())) {
                    // Cancel if the message is for a different channel than what DiscordSRV is trying to process
                    event.setCancelled(true);
                } else {
                    // Remove the trigger prefix from the message before sending to Discord
                    String cleanMessage = message.substring(channel.getPrefix().length());
                    event.setMessage(cleanMessage);
                }
                return;
            }
        }
        
        // If no channel character, use the player's current channel
        if (playerChannel != null && !playerChannel.getId().equals(event.getChannel())) {
            event.setCancelled(true);
        }
    }
    
    @Subscribe
    public void onDiscordGuildMessagePreProcess(DiscordGuildMessagePreProcessEvent event) {
        // Handle messages from Discord to Minecraft if needed
        // This is where you can filter which Discord messages go to which Minecraft channels
    }
}
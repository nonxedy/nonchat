package com.nonxedy.nonchat.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.api.Channel;
import com.nonxedy.nonchat.core.ChatManager;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;

/**
 * Listener for DiscordSRV events
 * Used to prevent DiscordSRV from processing messages that should
 * be handled by our custom integration
 */
public class DiscordSRVListener implements Listener {

    private final Nonchat plugin;

    public DiscordSRVListener(Nonchat plugin) {
        this.plugin = plugin;
        
        // Register with DiscordSRV's API
        Plugin discordSRV = plugin.getServer().getPluginManager().getPlugin("DiscordSRV");
        if (discordSRV != null && discordSRV.isEnabled()) {
            DiscordSRV.api.subscribe(this);
            plugin.logResponse("Successfully registered DiscordSRV chat listener!");
        }
    }

    /**
     * This event is called before DiscordSRV processes a chat message.
     * We need to inspect the message and determine whether it should be sent to
     * Discord and which channel it should go to.
     */
    @Subscribe
    public void onGameChatMessagePreProcess(GameChatMessagePreProcessEvent event) {
        // Get the player and message
        Player player = event.getPlayer();
        String message = event.getMessage();
    
        if (player == null) {
            return;
        }
    
        // Get channel for this message
        ChatManager chatManager = plugin.getChatManager();
        if (chatManager == null) {
            return;
        }

        // Determine which channel handles this message
        Channel channel = null;
    
        // First check if message starts with a channel character
        if (message.length() > 0) {
            char firstChar = message.charAt(0);
            for (Channel ch : chatManager.getAllChannels()) {
                if (ch.isEnabled() && ch.hasTriggerCharacter() && ch.getCharacter() == firstChar) {
                    channel = ch;
                    message = message.substring(1); // Remove the channel character
                    break;
                }
            }
        }
    
        // If no channel was found by character, use the player's active channel
        if (channel == null) {
            channel = chatManager.getPlayerChannel(player);
        }
    
        // Cancel the event by default - we'll send to Discord ourselves
        event.setCancelled(true);
    }

    
    /**
     * Unsubscribe from DiscordSRV events
     */
    public void shutdown() {
        Plugin discordSRV = plugin.getServer().getPluginManager().getPlugin("DiscordSRV");
        if (discordSRV != null && discordSRV.isEnabled()) {
            DiscordSRV.api.unsubscribe(this);
        }
    }
}

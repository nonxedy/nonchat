package com.nonxedy.nonchat.integration;

import com.nonxedy.nonchat.api.Channel;
import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.nonchat;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * Handles integration with DiscordSRV plugin.
 */
public class DiscordSRVIntegration {
    private final nonchat plugin;
    private final ChatManager chatManager;
    private boolean registered = false;
    
    /**
     * Creates a new DiscordSRV integration handler.
     * @param plugin The plugin instance
     */
    public DiscordSRVIntegration(nonchat plugin) {
        this.plugin = plugin;
        this.chatManager = plugin.getChatManager();
    }
    
    /**
     * Registers this integration with DiscordSRV.
     */
    public void register() {
        if (registered) return;
        
        if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
            DiscordSRV.api.subscribe(this);
            registered = true;
            plugin.logResponse("Registered DiscordSRV integration");
        } else {
            plugin.logError("DiscordSRV is not available, integration disabled");
        }
    }
    
    /**
     * Unregisters this integration.
     */
    public void unregister() {
        if (!registered) return;
        
        if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
            DiscordSRV.api.unsubscribe(this);
            registered = false;
            plugin.logResponse("Unregistered DiscordSRV integration");
        }
    }
    
    /**
     * Sends a direct message string to Discord.
     */
    public boolean sendDirectMessageToDiscord(Player player, String message, Channel channel) {
        if (!registered) return false;
        if (!Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) return false;
        
        // Check if channel has a Discord channel ID
        String discordChannelId = channel.getDiscordChannelId();
        if (discordChannelId == null || discordChannelId.isEmpty()) return true;
        
        try {
            // Get Discord channel
            TextChannel textChannel = DiscordSRV.getPlugin().getJda().getTextChannelById(discordChannelId);
            if (textChannel == null) return false;
            
            // Clean any formatting codes
            String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "");
            
            plugin.logResponse("Sending direct message to Discord: " + cleanMessage);
            DiscordUtil.sendMessage(textChannel, cleanMessage);
            return true;
        } catch (Exception e) {
            plugin.logError("Error sending to Discord: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Sends a raw message directly to Discord, completely bypassing any Component formatting.
     * This approach treats the player as a bot user in Discord.
     * 
     * @param player The player who sent the message
     * @param rawMessage The raw message text (no formatting)
     * @param channel The channel the message was sent to
     * @return true if the message was sent successfully
     */
    public boolean sendRawMessageToDiscord(Player player, String rawMessage, Channel channel) {
        if (!registered) return false;
        if (!Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) return false;
        
        // Check if channel has a Discord channel ID
        String discordChannelId = channel.getDiscordChannelId();
        if (discordChannelId == null || discordChannelId.isEmpty()) return true;
        
        try {
            // Get Discord channel
            TextChannel textChannel = DiscordSRV.getPlugin().getJda().getTextChannelById(discordChannelId);
            if (textChannel == null) return false;
            
            // Simple approach: format as "prefix user suffix: message"
            String username = player.getName();
            
            String prefix = "";
            if (player.hasPermission("nonchat.prefix.admin")) {
                prefix = "Adm";
            } else if (player.hasPermission("nonchat.prefix.mod")) {
                prefix = "Mod";
            }
            
            String suffix = "";
            
            // Format as requested: "prefix user suffix: message"
            StringBuilder messageBuilder = new StringBuilder();
            if (!prefix.isEmpty()) {
                messageBuilder.append(prefix).append(" ");
            }
            
            messageBuilder.append(username);
            
            if (!suffix.isEmpty()) {
                messageBuilder.append(" ").append(suffix);
            }
            
            messageBuilder.append(": ").append(rawMessage);
            
            // Send the simple text message
            String finalMessage = messageBuilder.toString();
            DiscordUtil.sendMessage(textChannel, finalMessage);
            
            plugin.logResponse("Sent message to Discord: " + finalMessage);
            return true;
        } catch (Exception e) {
            plugin.logError("Error sending raw message to Discord: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Legacy method kept for compatibility.
     */
    public boolean sendMessageToDiscord(Player player, String message, Channel channel) {
        // Just pass through to the new raw message method
        return sendRawMessageToDiscord(player, message, channel);
    }
    
    /**
     * Handles Discord messages coming to Minecraft.
     */
    @Subscribe(priority = ListenerPriority.NORMAL)
    public void onDiscordMessageReceived(DiscordGuildMessagePreProcessEvent event) {
        // Skip if no author or it's a bot
        User author = event.getAuthor();
        if (author == null || author.isBot()) return;
        
        // Skip empty messages
        Message message = event.getMessage();
        String content = message.getContentDisplay();
        if (content.trim().isEmpty()) return;
        
        // Get Discord channel ID
        String discordChannelId = message.getChannel().getId();
        
        // Find the global channel to send to
        Channel globalChannel = chatManager.getChannel("global");
        if (globalChannel == null) return;
        
        // Check for matching channel
        boolean hasMatchingChannel = false;
        for (Channel channel : chatManager.getAllChannels()) {
            if (discordChannelId.equals(channel.getDiscordChannelId())) {
                hasMatchingChannel = true;
                break;
            }
        }
        
        if (!hasMatchingChannel) return;
        
        // Format Discord -> Minecraft message
        String authorName = message.getAuthor().getName();
        
        // Create component message
        Component component = Component.text("[Discord] ", TextColor.fromHexString("#7289DA"))
            .append(Component.text(authorName + ": ", TextColor.fromHexString("#FFFFFF")))
            .append(Component.text(content, TextColor.fromHexString("#CCCCCC")));
        
        // Send to players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (globalChannel.canReceive(player)) {
                player.sendMessage(component);
            }
        }
        
        // Send to console
        Bukkit.getConsoleSender().sendMessage(component);
    }
    
    /**
     * Intercepts messages to control what goes to Discord.
     */
    @Subscribe(priority = ListenerPriority.HIGHEST)
    public void onGameChatMessagePreProcess(GameChatMessagePreProcessEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        
        // Get message & channel
        String message = event.getMessage();
        if (message == null || message.trim().isEmpty()) return;
        
        Channel playerChannel = plugin.getChatManager().getPlayerChannel(player);
        if (playerChannel == null) {
            playerChannel = plugin.getChatManager().getChannel("global");
        }
        
        // Check if channel has Discord integration
        String discordChannelId = playerChannel.getDiscordChannelId();
        
        // Cancel event if no Discord channel ID
        if (discordChannelId == null || discordChannelId.isEmpty()) {
            plugin.logResponse("Cancelling DiscordSRV message for channel: " + playerChannel.getId());
            event.setCancelled(true); // This prevents DiscordSRV from sending the message to Discord
        } else {
            plugin.logResponse("Allowing DiscordSRV message for channel: " + playerChannel.getId());
            String channelName = event.getChannel();
            if (channelName == null || !channelName.equals(playerChannel.getId())) {
                event.setChannel(playerChannel.getId());
            }
        }
    }
}

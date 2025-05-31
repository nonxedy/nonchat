package com.nonxedy.nonchat.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.api.Channel;
import com.nonxedy.nonchat.api.ChannelAPI;
import com.nonxedy.nonchat.chat.channel.ChannelManager;
import com.nonxedy.nonchat.command.impl.IgnoreCommand;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.util.BubblePacketUtil;
import com.nonxedy.nonchat.util.CapsFilter;
import com.nonxedy.nonchat.util.ChatTypeUtil;
import com.nonxedy.nonchat.util.ColorUtil;
import com.nonxedy.nonchat.util.WordBlocker;

import net.kyori.adventure.text.Component;

public class ChatManager {
    private final nonchat plugin;
    private final PluginConfig config;
    private final PluginMessages messages;
    private final ChannelManager channelManager;
    private final Pattern mentionPattern = Pattern.compile("@(\\w+)");
    private final Map<Player, List<ArmorStand>> bubbles = new HashMap<>();
    private IgnoreCommand ignoreCommand;

    public ChatManager(nonchat plugin, PluginConfig config, PluginMessages messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.channelManager = new ChannelManager(config);
        this.ignoreCommand = plugin.getIgnoreCommand();
        startBubbleUpdater();
    }

    public void processChat(Player player, String messageContent) {
        if (handleBlockedWords(player, messageContent)) {
            return;
        }
    
        CapsFilter capsFilter = config.getCapsFilter();
        if (capsFilter.shouldFilter(messageContent)) {
            player.sendMessage(ColorUtil.parseComponent(messages.getString("caps-filter")
                .replace("{percentage}", String.valueOf(capsFilter.getMaxCapsPercentage()))));
            return;
        }
    
        // Determine which channel to use based on message prefix or player's active channel
        Channel channel;
        String finalMessage;
        
        // First check if message starts with a channel character
        if (messageContent.length() > 0) {
            char firstChar = messageContent.charAt(0);
            channel = findChannelByChar(firstChar);
            
            // If a channel was found by character, remove the character from the message
            if (channel != null) {
                finalMessage = messageContent.substring(1);
            } else {
                // No character match, use player's active channel or default
                channel = channelManager.getPlayerChannel(player);
                finalMessage = messageContent;
            }
        } else {
            // Empty message, use player's active channel
            channel = channelManager.getPlayerChannel(player);
            finalMessage = messageContent;
        }
    
        // Check if channel is enabled
        if (!channel.isEnabled()) {
            player.sendMessage(ColorUtil.parseComponent(messages.getString("chat-disabled")));
            return;
        }
        
        // Check if player has permission to use this channel
        if (!channel.canSend(player)) {
            player.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            return;
        }
        
        // Check message length restrictions
        if (finalMessage.length() < channel.getMinLength()) {
            player.sendMessage(ColorUtil.parseComponent(messages.getString("message-too-short")
                .replace("{min}", String.valueOf(channel.getMinLength()))));
            return;
        }
        
        if (channel.getMaxLength() > 0 && finalMessage.length() > channel.getMaxLength()) {
            player.sendMessage(ColorUtil.parseComponent(messages.getString("message-too-long")
                .replace("{max}", String.valueOf(channel.getMaxLength()))));
            return;
        }
        
        // Check cooldown
        if (!channelManager.canSendMessage(player, channel)) {
            int remainingSeconds = channelManager.getRemainingCooldown(player, channel);
            player.sendMessage(ColorUtil.parseComponent(messages.getString("channel-cooldown")
                .replace("{seconds}", String.valueOf(remainingSeconds))
                .replace("{channel}", channel.getDisplayName())));
            return;
        }
        
        // The final message content to be used from now on
        final String messageToSend;
        
        // Check if message should be filtered by registered filters
        if (ChannelAPI.shouldFilterMessage(player, finalMessage, channel.getId())) {
            player.sendMessage(ColorUtil.parseComponent(messages.getString("message-filtered")));
            return;
        }
        
        // Process message through registered processors
        String processedMessage = ChannelAPI.processMessage(player, finalMessage, channel.getId());
        if (processedMessage == null) {
            // Message was cancelled by a processor
            return;
        }
        messageToSend = processedMessage; // Use a new variable for the processed message
    
        if (config.isChatBubblesEnabled() && player.hasPermission("nonchat.chatbubbles")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                removeBubble(player);
                createBubble(player, messageToSend);
            });
        }
    
        handleMentions(player, messageToSend);
        Component formattedMessage = channel.formatMessage(player, messageToSend);
        broadcastMessage(player, formattedMessage, channel, messageToSend);
        
        // Record message sent for cooldown tracking
        channelManager.recordMessageSent(player);
    }

    private void startBubbleUpdater() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Use Stream API to handle updating bubbles
            bubbles.entrySet().stream()
                .filter(entry -> entry.getKey().isOnline() && !entry.getValue().isEmpty())
                .forEach(entry -> {
                    Player player = entry.getKey();
                    Location newLoc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);
                    BubblePacketUtil.updateBubblesLocation(entry.getValue(), newLoc);
                });
        }, 1L, 1L);
    }

    private void createBubble(Player player, String message) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return; // Don't spawn bubble if player is in spectator mode
        }

        Location loc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);
        
        List<ArmorStand> playerBubbles = BubblePacketUtil.spawnMultilineBubble(player, message, loc);
        bubbles.put(player, playerBubbles);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeBubble(player);
        }, config.getChatBubblesDuration() * 20L);
    }

    private void removeBubble(Player player) {
        List<ArmorStand> playerBubbles = bubbles.remove(player);
        if (playerBubbles != null) {
            BubblePacketUtil.removeBubbles(playerBubbles);
        }
    }

    private boolean handleBlockedWords(Player player, String message) {
        if (!player.hasPermission("nonchat.antiblockedwords")) {
            WordBlocker wordBlocker = config.getWordBlocker();
            if (!wordBlocker.isMessageAllowed(message)) {
                player.sendMessage(ColorUtil.parseComponent(messages.getString("blocked-words")));
                return true;
            }
        }
        return false;
    }

    private void handleMentions(Player sender, String message) {
        // Find all mentions in the message
        Matcher mentionMatcher = mentionPattern.matcher(message);
        
        // Collect all the names found into a list and process with Stream API
        java.util.List<String> mentionedNames = new java.util.ArrayList<>();
        while (mentionMatcher.find()) {
            mentionedNames.add(mentionMatcher.group(1));
        }
        
        // Process the mentions using Stream API
        mentionedNames.stream()
            .map(Bukkit::getPlayer)
            .filter(java.util.Objects::nonNull)
            .filter(Player::isOnline)
            .forEach(player -> notifyMentionedPlayer(player, sender));
    }

    private void notifyMentionedPlayer(Player mentioned, Player sender) {
        mentioned.sendMessage(ColorUtil.parseComponent(
            messages.getString("mentioned")
                .replace("{player}", sender.getName())
        ));
        mentioned.playSound(mentioned.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }

    private void broadcastMessage(Player sender, Component message, Channel channel, String originalMessage) {
        // Always send to console
        Bukkit.getConsoleSender().sendMessage(message);

        // Send to players using Stream API
        Bukkit.getOnlinePlayers().stream()
            // Skip players ignoring the sender
            .filter(recipient -> ignoreCommand == null || !ignoreCommand.isIgnoring(recipient, sender))
            // Check channel-specific conditions
            .filter(recipient -> channel.canReceive(recipient))
            // For local channels, also check range
            .filter(recipient -> channel.isGlobal() || channel.isInRange(sender, recipient))
            // Send message to filtered recipients
            .forEach(recipient -> recipient.sendMessage(message));
    }
    
    /**
     * Finds a channel by its trigger character.
     * @param c The character to find
     * @return The channel, or null if not found
     */
    private Channel findChannelByChar(char c) {
        return channelManager.findChannelByCharacter(c).orElse(null);
    }
    
    /**
     * Sets a player's active channel.
     * @param player The player
     * @param channelId The channel ID
     * @return true if successful, false if channel not found or not enabled
     */
    public boolean setPlayerChannel(Player player, String channelId) {
        return channelManager.setPlayerChannel(player, channelId);
    }
    
    /**
     * Gets a player's active channel.
     * @param player The player
     * @return The player's channel
     */
    public Channel getPlayerChannel(Player player) {
        return channelManager.getPlayerChannel(player);
    }
    
    /**
     * Gets a channel by ID.
     * @param id The channel ID
     * @return The channel, or null if not found
     */
    public Channel getChannel(String id) {
        return channelManager.getChannel(id);
    }
    
    /**
     * Gets all available channels.
     * @return Collection of all channels
     */
    public java.util.Collection<Channel> getAllChannels() {
        return channelManager.getAllChannels();
    }
    
    /**
     * Gets all enabled channels.
     * @return Collection of enabled channels
     */
    public java.util.Collection<Channel> getEnabledChannels() {
        return channelManager.getEnabledChannels();
    }
    
    /**
     * Reloads channels from config.
     */
    public void reloadChannels() {
        channelManager.loadChannels();
    }
    
    /**
     * Creates a new channel with the specified properties.
     * @param channelId The unique channel ID
     * @param displayName The display name for the channel
     * @param format The message format for the channel
     * @param character The trigger character, or null for none
     * @param sendPermission Permission to send to this channel, or empty for everyone
     * @param receivePermission Permission to receive from this channel, or empty for everyone
     * @param radius Radius of the channel in blocks, or -1 for global
     * @param cooldown Cooldown between messages in seconds
     * @param minLength Minimum message length
     * @param maxLength Maximum message length, or -1 for unlimited
     * @return The created channel, or null if the ID already exists
     */
    public Channel createChannel(String channelId, String displayName, String format,
                                Character character, String sendPermission, String receivePermission,
                                int radius, int cooldown, int minLength, int maxLength) {
        return channelManager.createChannel(channelId, displayName, format, character, 
                                           sendPermission, receivePermission, radius,
                                           cooldown, minLength, maxLength);
    }
    
    /**
     * Updates an existing channel with new properties, including Discord channel ID.
     * @param channelId The channel ID to update
     * @param displayName The display name for the channel (null to keep existing)
     * @param format The message format for the channel (null to keep existing)
     * @param character The trigger character (null to keep existing, '\0' to remove)
     * @param sendPermission Permission to send to this channel (null to keep existing)
     * @param receivePermission Permission to receive from this channel (null to keep existing)
     * @param radius Radius of the channel in blocks (-1 for global, null to keep existing)
     * @param enabled Whether the channel is enabled (null to keep existing)
     * @param cooldown Cooldown between messages in seconds (null to keep existing)
     * @param minLength Minimum message length (null to keep existing)
     * @param maxLength Maximum message length (null to keep existing)
     * @return True if the channel was updated, false otherwise
     */
    public boolean updateChannel(String channelId, String displayName, String format,
                                Character character, String sendPermission, String receivePermission,
                                Integer radius, Boolean enabled, Integer cooldown, 
                                Integer minLength, Integer maxLength) {
        return channelManager.updateChannel(channelId, displayName, format, character,
                                          sendPermission, receivePermission, radius, enabled,
                                          cooldown, minLength, maxLength);
    }
    
    /**
     * Deletes a channel.
     * @param channelId The channel ID to delete
     * @return True if the channel was deleted, false otherwise
     */
    public boolean deleteChannel(String channelId) {
        return channelManager.deleteChannel(channelId);
    }
    
    /**
     * Sets a new default channel.
     * @param channelId The channel ID to set as default
     * @return True if successful, false if channel doesn't exist or isn't enabled
     */
    public boolean setDefaultChannel(String channelId) {
        return channelManager.setDefaultChannel(channelId);
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }
    
    /**
     * For backward compatibility with existing ChatTypeUtil system.
     * @param message The message to check
     * @param chats Map of chat types
     * @return The appropriate chat type
     */
    @Deprecated
    private ChatTypeUtil determineChat(String message, Map<String, ChatTypeUtil> chats) {
        if (message.length() > 0) {
            char firstChar = message.charAt(0);
            return config.getChatTypeByChar(firstChar);
        }
        return config.getDefaultChatType();
    }
    
    /**
     * Sets the ignore command instance.
     * @param ignoreCommand The ignore command instance
     */
    public void setIgnoreCommand(IgnoreCommand ignoreCommand) {
        this.ignoreCommand = ignoreCommand;
    }
}

package com.nonxedy.nonchat.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.api.Channel;
import com.nonxedy.nonchat.api.ChannelAPI;
import com.nonxedy.nonchat.chat.channel.ChannelManager;
import com.nonxedy.nonchat.command.impl.IgnoreCommand;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.util.chat.filters.CapsFilter;
import com.nonxedy.nonchat.util.chat.filters.WordBlocker;
import com.nonxedy.nonchat.util.chat.formatting.ChatTypeUtil;
import com.nonxedy.nonchat.util.chat.packets.BubblePacketUtil;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;

public class ChatManager {

    private final Nonchat plugin;
    private final PluginConfig config;
    private final PluginMessages messages;
    private final ChannelManager channelManager;
    private final Pattern mentionPattern = Pattern.compile("@(\\w+)");
    private final Map<Player, List<ArmorStand>> bubbles = new ConcurrentHashMap<>();
    private final Map<Player, ReentrantLock> playerLocks = new ConcurrentHashMap<>();
    private IgnoreCommand ignoreCommand;

    public ChatManager(Nonchat plugin, PluginConfig config, PluginMessages messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.channelManager = new ChannelManager(config);
        this.ignoreCommand = plugin.getIgnoreCommand();
        startBubbleUpdater();
    }

    public void processChat(Player player, String messageContent) {
        // Get or create player-specific lock
        ReentrantLock lock = playerLocks.computeIfAbsent(player, p -> new ReentrantLock());
        lock.lock();
        try {
            if (handleBlockedWords(player, messageContent)) {
                return;
            }

            CapsFilter capsFilter = config.getCapsFilter();
            if (capsFilter.shouldFilter(messageContent)) {
                player.sendMessage(ColorUtil.parseComponentCached(messages.getString("caps-filter")
                        .replace("{percentage}", String.valueOf(capsFilter.getMaxCapsPercentage()))));
                return;
            }

            // Check if player is trying to use colors without permission
            if (!player.hasPermission("nonchat.color") && ColorUtil.hasColorCodes(messageContent)) {
                // Strip colors but continue processing the message
                messageContent = ColorUtil.stripAllColors(messageContent);
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
                player.sendMessage(ColorUtil.parseComponentCached(messages.getString("chat-disabled")));
                return;
            }

            // Check if player has permission to use this channel
            if (!channel.canSend(player)) {
                player.sendMessage(ColorUtil.parseComponentCached(messages.getString("no-permission")));
                return;
            }

            // Check message length restrictions (use stripped message for length check)
            String messageForLengthCheck = player.hasPermission("nonchat.color") ? finalMessage : ColorUtil.stripAllColors(finalMessage);

            if (messageForLengthCheck.length() < channel.getMinLength()) {
                player.sendMessage(ColorUtil.parseComponentCached(messages.getString("message-too-short")
                        .replace("{min}", String.valueOf(channel.getMinLength()))));
                return;
            }

            if (channel.getMaxLength() > 0 && messageForLengthCheck.length() > channel.getMaxLength()) {
                player.sendMessage(ColorUtil.parseComponentCached(messages.getString("message-too-long")
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
                player.sendMessage(ColorUtil.parseComponentCached(messages.getString("message-filtered")));
                return;
            }

            // Process message through registered processors
            String processedMessage = ChannelAPI.processMessage(player, finalMessage, channel.getId());
            if (processedMessage == null) {
                // Message was cancelled by a processor
                return;
            }
            messageToSend = processedMessage; // Use a new variable for the processed message

            // Only show chat bubbles for public channels (channels without receive permission requirements)
            // or if the channel doesn't have restricted access
            boolean shouldShowBubble = config.isChatBubblesEnabled()
                    && player.hasPermission("nonchat.chatbubbles")
                    && isPublicChannel(channel);

            if (shouldShowBubble) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    removeBubble(player);
                    // For bubbles, use the message without colors if player doesn't have permission
                    String bubbleMessage = player.hasPermission("nonchat.color") ? messageToSend : ColorUtil.stripAllColors(messageToSend);
                    createBubble(player, bubbleMessage);
                });
            }

            handleMentions(player, messageToSend);
            Component formattedMessage = channel.formatMessage(player, messageToSend);
            boolean messageDelivered = broadcastMessage(player, formattedMessage, channel, messageToSend);

            // Check if undelivered message notifications are enabled and notify if message wasn't delivered
            if (config.isUndeliveredMessageNotificationEnabled() && !messageDelivered) {
                player.sendMessage(ColorUtil.parseComponentCached(messages.getString("message-not-delivered")));
            }

        } finally {
            lock.unlock();

            // Clean up lock if player is offline
            if (!player.isOnline()) {
                playerLocks.remove(player);
            }

            // Record message sent for cooldown tracking
            channelManager.recordMessageSent(player);
        }
    }

    /**
     * Checks if a channel is considered "public" (should show chat bubbles) A
     * channel is public if it doesn't require special permissions to receive
     * messages or if the config allows bubbles in private channels
     *
     * @param channel The channel to check
     * @return true if the channel should show bubbles, false otherwise
     */
    private boolean isPublicChannel(Channel channel) {
        // If config allows bubbles in private channels, always show them
        if (config.shouldShowBubblesInPrivateChannels()) {
            return true;
        }

        // If channel has receive permission requirements, it's considered private
        String receivePermission = channel.getReceivePermission();
        if (receivePermission != null && !receivePermission.isEmpty()) {
            return false;
        }

        // If channel has send permission requirements, it's also considered private
        String sendPermission = channel.getSendPermission();
        if (sendPermission != null && !sendPermission.isEmpty()) {
            return false;
        }

        // Channel is public if it doesn't require any special permissions
        return true;
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
            
            // Clean up bubbles for offline players
            bubbles.entrySet().removeIf(entry -> {
                if (!entry.getKey().isOnline()) {
                    BubblePacketUtil.removeBubbles(entry.getValue());
                    return true;
                }
                return false;
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
            // Check blocked words on the message without color codes
            String messageToCheck = ColorUtil.stripAllColors(message);
            if (!wordBlocker.isMessageAllowed(messageToCheck)) {
                player.sendMessage(ColorUtil.parseComponentCached(messages.getString("blocked-words")));
                return true;
            }
        }
        return false;
    }

    private void handleMentions(Player sender, String message) {
        // Find all mentions in the message (strip colors first to avoid false matches)
        String messageToCheck = ColorUtil.stripAllColors(message);
        Matcher mentionMatcher = mentionPattern.matcher(messageToCheck);

        // Collect all the names found into a list and process with Stream API
        List<String> mentionedNames = new ArrayList<>();
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
        String mentionMessage = messages.getString("mentioned");

        // Apply PlaceholderAPI to mention message
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                mentionMessage = PlaceholderAPI.setPlaceholders(sender, mentionMessage);
            } catch (Exception e) {
                plugin.logError("Error processing mention message placeholders: " + e.getMessage());
            }
        }

        // Replace {player} with sender name (keeping this for backward compatibility)
        mentionMessage = mentionMessage.replace("{player}", sender.getName());

        mentioned.sendMessage(ColorUtil.parseComponent(mentionMessage));
        mentioned.playSound(mentioned.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }

    /**
     * Broadcasts a message to all eligible recipients and returns whether it
     * was delivered to any players.
     *
     * @param sender The player sending the message
     * @param message The formatted message component
     * @param channel The channel being used
     * @param originalMessage The original message content
     * @return true if the message was delivered to at least one player, false
     * otherwise
     */
    private boolean broadcastMessage(Player sender, Component message, Channel channel, String originalMessage) {
        // Always send to console
        Bukkit.getConsoleSender().sendMessage(message);

        // Count how many players received the message
        long recipientCount = Bukkit.getOnlinePlayers().stream()
                // Skip players ignoring the sender
                .filter(recipient -> ignoreCommand == null || !ignoreCommand.isIgnoring(recipient, sender))
                // Check channel-specific conditions
                .filter(recipient -> channel.canReceive(recipient))
                // For local channels, also check range
                .filter(recipient -> channel.isGlobal() || channel.isInRange(sender, recipient))
                // Send message to filtered recipients and count them
                .peek(recipient -> recipient.sendMessage(message))
                .count();

        // Return true if at least one player (other than sender) received the message
        // We subtract 1 because the sender is also counted in the recipients
        return recipientCount > 1;
    }

    /**
     * Finds a channel by its trigger character.
     *
     * @param c The character to find
     * @return The channel, or null if not found
     */
    private Channel findChannelByChar(char c) {
        return channelManager.findChannelByCharacter(c).orElse(null);
    }

    /**
     * Sets a player's active channel.
     *
     * @param player The player
     * @param channelId The channel ID
     * @return true if successful, false if channel not found or not enabled
     */
    public boolean setPlayerChannel(Player player, String channelId) {
        return channelManager.setPlayerChannel(player, channelId);
    }

    /**
     * Gets a player's active channel.
     *
     * @param player The player
     * @return The player's channel
     */
    public Channel getPlayerChannel(Player player) {
        return channelManager.getPlayerChannel(player);
    }

    /**
     * Gets a channel by ID.
     *
     * @param id The channel ID
     * @return The channel, or null if not found
     */
    public Channel getChannel(String id) {
        return channelManager.getChannel(id);
    }

    /**
     * Gets all available channels.
     *
     * @return Collection of all channels
     */
    public java.util.Collection<Channel> getAllChannels() {
        return channelManager.getAllChannels();
    }

    /**
     * Gets all enabled channels.
     *
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
     *
     * @param channelId The unique channel ID
     * @param displayName The display name for the channel
     * @param format The message format for the channel
     * @param character The trigger character, or null for none
     * @param sendPermission Permission to send to this channel, or empty for
     * everyone
     * @param receivePermission Permission to receive from this channel, or
     * empty for everyone
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
     * Updates an existing channel with new properties, including Discord
     * channel ID.
     *
     * @param channelId The channel ID to update
     * @param displayName The display name for the channel (null to keep
     * existing)
     * @param format The message format for the channel (null to keep existing)
     * @param character The trigger character (null to keep existing, '\0' to
     * remove)
     * @param sendPermission Permission to send to this channel (null to keep
     * existing)
     * @param receivePermission Permission to receive from this channel (null to
     * keep existing)
     * @param radius Radius of the channel in blocks (-1 for global, null to
     * keep existing)
     * @param enabled Whether the channel is enabled (null to keep existing)
     * @param cooldown Cooldown between messages in seconds (null to keep
     * existing)
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
     *
     * @param channelId The channel ID to delete
     * @return True if the channel was deleted, false otherwise
     */
    public boolean deleteChannel(String channelId) {
        return channelManager.deleteChannel(channelId);
    }

    /**
     * Sets a new default channel.
     *
     * @param channelId The channel ID to set as default
     * @return True if successful, false if channel doesn't exist or isn't
     * enabled
     */
    public boolean setDefaultChannel(String channelId) {
        return channelManager.setDefaultChannel(channelId);
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    /**
     * For backward compatibility with existing ChatTypeUtil system.
     *
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
     *
     * @param ignoreCommand The ignore command instance
     */
    public void setIgnoreCommand(IgnoreCommand ignoreCommand) {
        this.ignoreCommand = ignoreCommand;
    }
    
    /**
     * Cleanup method to remove all bubbles when plugin is disabled
     */
    public void cleanup() {
        bubbles.values().forEach(BubblePacketUtil::removeBubbles);
        bubbles.clear();
        playerLocks.clear();
    }
}

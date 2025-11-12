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
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.api.Channel;
import com.nonxedy.nonchat.api.ChannelAPI;
import com.nonxedy.nonchat.chat.channel.ChannelManager;
import com.nonxedy.nonchat.command.impl.IgnoreCommand;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.util.AsyncFilterService;
import com.nonxedy.nonchat.util.chat.filters.AdDetector;
import com.nonxedy.nonchat.util.chat.filters.CapsFilter;
import com.nonxedy.nonchat.util.chat.filters.SpamDetector;
import com.nonxedy.nonchat.util.chat.filters.WordBlocker;
import com.nonxedy.nonchat.util.chat.packets.DisplayEntityUtil;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;

public class ChatManager {

    private final Nonchat plugin;
    private final PluginConfig config;
    private final PluginMessages messages;
    private final ChannelManager channelManager;
    private final Pattern mentionPattern = Pattern.compile("@(\\w+)");
    private final Map<Player, List<TextDisplay>> bubbles = new ConcurrentHashMap<>();
    private final Map<Player, ReentrantLock> playerLocks = new ConcurrentHashMap<>();
    private IgnoreCommand ignoreCommand;
    private final AdDetector adDetector;
    private final SpamDetector spamDetector;
    private final AsyncFilterService asyncFilterService;

    public ChatManager(Nonchat plugin, PluginConfig config, PluginMessages messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.adDetector = new AdDetector(config,
                                      config.getAntiAdSensitivity(),
                                      config.getAntiAdPunishCommand());
        this.spamDetector = new SpamDetector(config, messages);
        this.asyncFilterService = new AsyncFilterService(plugin, adDetector);
        this.channelManager = new ChannelManager(plugin, config);
        this.ignoreCommand = plugin.getIgnoreCommand();
        startBubbleUpdater();
    }

    public void processChat(Player player, String messageContent) {
        // Get or create player-specific lock
        ReentrantLock lock = playerLocks.computeIfAbsent(player, p -> new ReentrantLock());
        lock.lock();
        try {
            // Check if the message is empty or contains only whitespace
            if (messageContent == null || messageContent.trim().isEmpty()) {
                return; // Silently cancel empty messages
            }
            
            if (handleBlockedWords(player, messageContent)) {
                return;
            }

            CapsFilter capsFilter = config.getCapsFilter();
            if (!player.hasPermission("nonchat.caps.bypass") && capsFilter.shouldFilter(messageContent)) {
                player.sendMessage(ColorUtil.parseComponentCached(messages.getString("caps-filter")
                        .replace("{percentage}", String.valueOf(capsFilter.getMaxCapsPercentage()))));
                return;
            }

            // Check for spam
            if (config.isAntiSpamEnabled() && !player.hasPermission("nonchat.spam.bypass")) {
                if (spamDetector.shouldFilter(player, messageContent)) {
                    // Warning message is already sent by SpamDetector
                    return;
                }
            }

            // Check for advertisements
            if (config.isAntiAdEnabled() && !player.hasPermission("nonchat.ad.bypass")) {
                if (adDetector.shouldFilter(player, messageContent)) {
                    player.sendMessage(ColorUtil.parseComponentCached(messages.getString("blocked-words")));
                    return;
                }
            }

            // Check if player is trying to use colors without permission
            if (!player.hasPermission("nonchat.color") && ColorUtil.hasColorCodes(messageContent)) {
                // Strip colors but continue processing the message
                messageContent = ColorUtil.stripAllColors(messageContent);
                
                // Check if the message is empty after stripping colors
                if (messageContent.trim().isEmpty()) {
                    return; // Silently cancel empty messages
                }
            }

            // Determine which channel to use based on message prefix or player's active channel
            Channel channel = channelManager.getChannelForMessage(messageContent);
            String finalMessage;

            // If a channel was found by prefix, remove the prefix from the message
            if (channel != null && channel.hasPrefix() && messageContent.startsWith(channel.getPrefix())) {
                finalMessage = messageContent.substring(channel.getPrefix().length());
                
                // Check if the message is empty after removing channel prefix
                if (finalMessage.trim().isEmpty()) {
                    return; // Silently cancel empty messages
                }
            } else {
                // No prefix match, use the message as-is
                finalMessage = messageContent;
            }

            // Ensure we have a valid channel (should not be null from getChannelForMessage)
            if (channel == null) {
                return; // Silently cancel if no channel available
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

            // Record message sent immediately after passing cooldown check
            // This prevents the cooldown from getting stuck when multiple messages are sent rapidly
            channelManager.recordMessageSent(player);

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

            // Apply mention coloring for all recipients if enabled
            final String messageToSend = config.isMentionColoringEnabled()
                ? processMentionColoring(processedMessage)
                : processedMessage;

            // Only show chat bubbles for public channels (channels without receive permission requirements)
            // or if the channel doesn't have restricted access
            boolean shouldShowBubble = config.isChatBubblesEnabled()
                    && player.hasPermission("nonchat.chatbubbles")
                    && isPublicChannel(channel);

            if (shouldShowBubble) {
                try {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            removeBubble(player);
                            // For bubbles, use the message without colors if player doesn't have permission
                            String bubbleMessage = player.hasPermission("nonchat.color") ? messageToSend : ColorUtil.stripAllColors(messageToSend);
                            createBubble(player, bubbleMessage);
                        } catch (Exception e) {
                            plugin.logError("Error in bubble creation task for player " + player.getName() + ": " + e.getMessage());
                        }
                    });
                } catch (IllegalArgumentException e) {
                    plugin.logError("Failed to schedule bubble creation for player " + player.getName() + ": " + e.getMessage());
                    // Try to create bubble immediately as fallback
                    try {
                        removeBubble(player);
                        String bubbleMessage = player.hasPermission("nonchat.color") ? messageToSend : ColorUtil.stripAllColors(messageToSend);
                        createBubble(player, bubbleMessage);
                    } catch (Exception fallbackError) {
                        plugin.logError("Fallback bubble creation also failed for player " + player.getName() + ": " + fallbackError.getMessage());
                        // Try to create bubble using global scheduler as last resort
                        try {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                try {
                                    removeBubble(player);
                                    String bubbleMessage = player.hasPermission("nonchat.color") ? messageToSend : ColorUtil.stripAllColors(messageToSend);
                                    createBubble(player, bubbleMessage);
                                } catch (Exception globalError) {
                                    plugin.logError("Global scheduler bubble creation also failed for player " + player.getName() + ": " + globalError.getMessage());
                                }
                            });
                        } catch (IllegalArgumentException globalSchedulerError) {
                            plugin.logError("Failed to schedule global bubble creation for player " + player.getName() + ": " + globalSchedulerError.getMessage());
                        }
                    }
                }
            }

            handleMentions(player, processedMessage); // Use original message for mention detection
            Component formattedMessage = channel.formatMessage(player, messageToSend);
            boolean messageDelivered = broadcastMessage(player, formattedMessage, channel, processedMessage); // Use original message for console

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
        // If channel has receive permission requirements, it's considered private
        String receivePermission = channel.getReceivePermission();
        if (receivePermission != null && !receivePermission.isEmpty()) {
            return false;
        }

        // If channel has send permission requirements, it's also considered private
        String sendPermission = channel.getSendPermission();
        // Channel is public if it doesn't require any special permissions
        return !(sendPermission != null && !sendPermission.isEmpty());
    }

    private void startBubbleUpdater() {
        try {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                try {
                    // Use Stream API to handle updating bubbles
                    bubbles.entrySet().stream()
                            .filter(entry -> entry.getKey().isOnline() && !entry.getValue().isEmpty())
                            .forEach(entry -> {
                                try {
                                    Player player = entry.getKey();
                                    Location newLoc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);
                                    DisplayEntityUtil.updateBubblesLocation(entry.getValue(), newLoc);
                                } catch (Exception e) {
                                    plugin.logError("Error updating bubbles for player " + entry.getKey().getName() + ": " + e.getMessage());
                                }
                            });

                    // Clean up bubbles for offline players
                    bubbles.entrySet().removeIf(entry -> {
                        try {
                            if (!entry.getKey().isOnline()) {
                                DisplayEntityUtil.removeBubbles(entry.getValue());
                                return true;
                            }
                        } catch (Exception e) {
                            plugin.logError("Error cleaning up bubbles for offline player: " + e.getMessage());
                            return true; // Remove entry on error
                        }
                        return false;
                    });
                } catch (Exception e) {
                    plugin.logError("Error in bubble updater: " + e.getMessage());
                }
            }, 1L, 1L);
        } catch (IllegalArgumentException e) {
            plugin.logError("Failed to start bubble updater: " + e.getMessage());
            // Try to start with global scheduler as fallback
            try {
                Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    try {
                        // Use Stream API to handle updating bubbles
                        bubbles.entrySet().stream()
                                .filter(entry -> entry.getKey().isOnline() && !entry.getValue().isEmpty())
                                .forEach(entry -> {
                                    try {
                                        Player player = entry.getKey();
                                        Location newLoc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);
                                        DisplayEntityUtil.updateBubblesLocation(entry.getValue(), newLoc);
                                    } catch (Exception e2) {
                                        plugin.logError("Error updating bubbles for player " + entry.getKey().getName() + ": " + e2.getMessage());
                                    }
                                });

                        // Clean up bubbles for offline players
                        bubbles.entrySet().removeIf(entry -> {
                            try {
                                if (!entry.getKey().isOnline()) {
                                    DisplayEntityUtil.removeBubbles(entry.getValue());
                                    return true;
                                }
                            } catch (Exception e2) {
                                plugin.logError("Error cleaning up bubbles for offline player: " + e2.getMessage());
                                return true; // Remove entry on error
                            }
                            return false;
                        });
                    } catch (Exception e2) {
                        plugin.logError("Error in fallback bubble updater: " + e2.getMessage());
                    }
                }, 1L, 1L);
                plugin.logResponse("Bubble updater started with fallback scheduler");
            } catch (IllegalArgumentException fallbackError) {
                plugin.logError("Failed to start bubble updater with fallback scheduler: " + fallbackError.getMessage());
                // Try to start with immediate execution as last resort
                try {
                    plugin.logResponse("Starting bubble updater with immediate execution as last resort");
                    // This will run the updater immediately and then stop
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            // Use Stream API to handle updating bubbles
                            bubbles.entrySet().stream()
                                    .filter(entry -> entry.getKey().isOnline() && !entry.getValue().isEmpty())
                                    .forEach(entry -> {
                                        try {
                                            Player player = entry.getKey();
                                            Location newLoc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);
                                            DisplayEntityUtil.updateBubblesLocation(entry.getValue(), newLoc);
                                        } catch (Exception e2) {
                                            plugin.logError("Error updating bubbles for player " + entry.getKey().getName() + ": " + e2.getMessage());
                                        }
                                    });

                            // Clean up bubbles for offline players
                            bubbles.entrySet().removeIf(entry -> {
                                try {
                                    if (!entry.getKey().isOnline()) {
                                        DisplayEntityUtil.removeBubbles(entry.getValue());
                                        return true;
                                    }
                                } catch (Exception e2) {
                                    plugin.logError("Error cleaning up bubbles for offline player: " + e2.getMessage());
                                    return true; // Remove entry on error
                                }
                                return false;
                            });
                        } catch (Exception e2) {
                            plugin.logError("Error in immediate bubble updater: " + e2.getMessage());
                        }
                    });
                    plugin.logResponse("Bubble updater started with immediate execution");
                } catch (IllegalArgumentException immediateError) {
                    plugin.logError("Failed to start bubble updater with immediate execution: " + immediateError.getMessage());
                }
            }
        }
    }

    private void createBubble(Player player, String message) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return; // Don't spawn bubble if player is in spectator mode
        }

        try {
            Location loc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);

            List<TextDisplay> playerBubbles = DisplayEntityUtil.spawnMultilineBubble(player, message, loc, 
                config.getChatBubblesScale(), config.getChatBubblesScaleX(), config.getChatBubblesScaleY(), config.getChatBubblesScaleZ());
            
            // Only add bubbles if they were successfully created
            if (playerBubbles != null && !playerBubbles.isEmpty()) {
                bubbles.put(player, playerBubbles);

                try {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        removeBubble(player);
                    }, config.getChatBubblesDuration() * 20L);
                } catch (IllegalArgumentException e) {
                    plugin.logError("Failed to schedule bubble removal for player " + player.getName() + ": " + e.getMessage());
                    // Schedule removal using global scheduler as fallback
                    try {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            removeBubble(player);
                        }, config.getChatBubblesDuration() * 20L);
                    } catch (IllegalArgumentException fallbackError) {
                        plugin.logError("Fallback bubble removal scheduling also failed for player " + player.getName() + ": " + fallbackError.getMessage());
                        // Try to remove bubble immediately as last resort
                        try {
                            removeBubble(player);
                        } catch (Exception immediateError) {
                            plugin.logError("Immediate bubble removal also failed for player " + player.getName() + ": " + immediateError.getMessage());
                        }
                    }
                }
            } else {
                plugin.logError("Failed to create chat bubbles for player: " + player.getName());
            }
        } catch (Exception e) {
            plugin.logError("Error creating chat bubbles for player " + player.getName() + ": " + e.getMessage());
        }
    }

    private void removeBubble(Player player) {
        try {
            List<TextDisplay> playerBubbles = bubbles.remove(player);
            if (playerBubbles != null) {
                DisplayEntityUtil.removeBubbles(playerBubbles);
            }
        } catch (Exception e) {
            plugin.logError("Error removing bubbles for player " + player.getName() + ": " + e.getMessage());
            // Try to clean up the map entry even if removal fails
            try {
                bubbles.remove(player);
            } catch (Exception cleanupError) {
                plugin.logError("Error cleaning up bubble map for player " + player.getName() + ": " + cleanupError.getMessage());
            }
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

        // Play mention sound if enabled for mention events
        if (config.isMentionSoundEnabled()) {
            try {
                mentioned.playSound(mentioned.getLocation(),
                    config.getMentionSound(),
                    config.getMentionSoundVolume(), config.getMentionSoundPitch());
            } catch (Exception e) {
                plugin.logError("Error playing mention sound: " + e.getMessage());
            }
        }
    }

    /**
     * Processes mention coloring in a message for the sender's view
     * @param message The message to process
     * @return The message with colored mentions
     */
    private String processMentionColoring(String message) {
        String mentionColor = config.getMentionColor();
        Matcher mentionMatcher = mentionPattern.matcher(message);

        // Use StringBuilder for efficient string manipulation
        StringBuilder coloredMessage = new StringBuilder();
        int lastEnd = 0;

        while (mentionMatcher.find()) {
            // Add text before the mention
            coloredMessage.append(message.substring(lastEnd, mentionMatcher.start()));
            // Add the colored mention with reset after it
            coloredMessage.append(mentionColor).append(mentionMatcher.group(0)).append("&r");
            lastEnd = mentionMatcher.end();
        }

        // Add remaining text after the last mention
        coloredMessage.append(message.substring(lastEnd));

        return coloredMessage.toString();
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
        // For console, create a simple message without our color modifications to avoid &f appearing
        String consoleFormat = channel.getFormat().replace("{message}", originalMessage);
        
        // Apply PlaceholderAPI for console
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                consoleFormat = PlaceholderAPI.setPlaceholders(sender, consoleFormat);
            } catch (Exception e) {
                plugin.logError("Error processing format placeholders for console: " + e.getMessage());
            }
        }
        
        // Send to console with processed format
        Bukkit.getConsoleSender().sendMessage(ColorUtil.parseComponent(consoleFormat));

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
        return channelManager.findChannelByPrefix(String.valueOf(c)).orElse(null);
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
        String prefix = character != null ? String.valueOf(character) : "";
        return channelManager.createChannel(channelId, displayName, format, prefix,
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
        String prefix = character != null ? String.valueOf(character) : null;
        return channelManager.updateChannel(channelId, displayName, format, prefix,
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
        bubbles.values().forEach(DisplayEntityUtil::removeBubbles);
        bubbles.clear();
        playerLocks.clear();
    }
}

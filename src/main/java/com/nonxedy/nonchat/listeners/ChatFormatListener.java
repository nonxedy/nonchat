package com.nonxedy.nonchat.listeners;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.CapsFilter;
import com.nonxedy.nonchat.utils.ChatTypeUtil;
import com.nonxedy.nonchat.utils.ColorUtil;
import com.nonxedy.nonchat.utils.WordBlocker;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

/**
 * Main chat formatting and processing handler
 * Manages chat types, filtering, and message delivery
 */
public class ChatFormatListener implements Listener {
    
    // Configuration instances for plugin settings and messages
    private final PluginConfig config;
    private final PluginMessages messages;
    // Regular expression pattern to detect mentions using @username format
    private final Pattern mentionPattern = Pattern.compile("@(\\w+)");

    // Constructor initializes config and messages
    public ChatFormatListener(PluginConfig config, PluginMessages messages) {
        this.config = config;
        this.messages = messages;
    }

    /**
     * Processes chat messages and applies formatting
     * @param event The async chat event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncChatEvent event) {
        // Cancel the default chat event to handle custom formatting
        event.setCancelled(true);
        // Get the player who sent the message
        Player player = event.getPlayer();
        // Convert the modern component message to legacy string format
        String messageContent = getLegacyContent(event.message());
        // Get capitalization settings from config
        CapsFilter capsFilter = config.getCapsFilter();
        
        // Check for blocked words and return if message is not allowed
        if (handleBlockedWords(player, messageContent)) {
            return;
        }

        // Check for excessive capitalization and cancel the event if needed
        if (capsFilter.shouldFilter(messageContent)) {
            // Cancel the default chat event to prevent sending the message
            event.setCancelled(true);
            // Get the warning message + percentages from config
            String warningMessage = messages.getString("caps-filter")
                .replace("{percentage}", String.valueOf(config.getMaxCapsPercentage()));
            // Send a warning message to the player
            event.getPlayer().sendMessage(ColorUtil.parseComponent(warningMessage));
            return;
        }

        // Get all configured chat types from config
        Map<String, ChatTypeUtil> chats = config.getChats();
        // Determine which chat type to use based on message prefix
        ChatTypeUtil chatTypeUtil = determineChat(messageContent, chats);
        
        // Check if the determined chat type is enabled
        if (!chatTypeUtil.isEnabled()) {
            // Send disabled message to player if chat type is disabled
            player.sendMessage(ColorUtil.parseComponent(messages.getString("chat-disabled")));
            return;
        }

        // Remove chat prefix character if present
        String finalMessage = chatTypeUtil.getChatChar() != '\0' ? 
            messageContent.substring(1) : messageContent;

        // Process any @mentions in the message
        handleMentions(player, finalMessage);
        // Format the message with player prefix, suffix and chat format
        Component formattedMessage = formatMessage(player, finalMessage, chatTypeUtil);
        
        // Broadcast the formatted message to appropriate recipients
        broadcastMessage(player, formattedMessage, chatTypeUtil);
    }

    /**
     * Broadcasts message to appropriate recipients
     * @param sender Message sender
     * @param message Formatted message
     * @param chatTypeUtil Chat type configuration
     */
    private void broadcastMessage(Player sender, Component message, ChatTypeUtil chatTypeUtil) {
        // If chat type is global, broadcast to all players
        if (chatTypeUtil.isGlobal()) {
            Bukkit.broadcast(message);
        } else {
            // For local chat, only send to players within specified radius
            for (Player recipient : Bukkit.getOnlinePlayers()) {
                if (isInRange(sender, recipient, chatTypeUtil.getRadius())) {
                    recipient.sendMessage(message);
                }
            }
        }
    }

    /**
     * Checks if recipient is within chat range
     * @param sender Message sender
     * @param recipient Message recipient
     * @param radius Chat range in blocks
     * @return true if in range, false otherwise
     */
    private boolean isInRange(Player sender, Player recipient, int radius) {
        // Check if players are in same world and within radius (-1 means unlimited)
        return sender.getWorld() == recipient.getWorld() && 
                (radius == -1 || sender.getLocation().distance(recipient.getLocation()) <= radius);
    }

    /**
     * Determines chat type from message prefix
     * @param message Raw message content
     * @param chats Available chat types
     * @return Appropriate chat type configuration
     */
    private ChatTypeUtil determineChat(String message, Map<String, ChatTypeUtil> chats) {
        // If message is not empty, check first character for chat type
        if (message.length() > 0) {
            char firstChar = message.charAt(0);
            return config.getChatTypeByChar(firstChar);
        }
        // Return default chat type if no prefix is found
        return config.getDefaultChatType();
    }

    /**
     * Formats message with player info and chat format
     * @param player Message sender
     * @param message Raw message content
     * @param chatTypeUtil Chat type configuration
     * @return Formatted component
     */
    private Component formatMessage(Player player, String message, ChatTypeUtil chatTypeUtil) {
        // Get LuckPerms user data for prefix/suffix
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        String prefix = user.getCachedData().getMetaData().getPrefix();
        String suffix = user.getCachedData().getMetaData().getSuffix();
        
        // Convert null prefix/suffix to empty string and parse colors
        prefix = prefix == null ? "" : ColorUtil.parseColor(prefix);
        suffix = suffix == null ? "" : ColorUtil.parseColor(suffix);

        // Create base format without player name
        String baseFormat = chatTypeUtil.getFormat()
            .replace("{prefix}", prefix)
            .replace("{suffix}", suffix)
            .replace("{message}", ColorUtil.parseColor(message));

        // Split format at {sender} placeholder
        String[] parts = baseFormat.split("\\{sender\\}");

        // Create the final component
        Component finalMessage = ColorUtil.parseComponent(parts[0])
            .append(config.getHoverTextUtil().createHoverablePlayerName(player, player.getName()));

        // Append remaining format if present
        if (parts.length > 1) {
            finalMessage = finalMessage.append(ColorUtil.parseComponent(parts[1]));
        }

        return finalMessage;
    }

    /**
     * Converts component to legacy string format
     * @param message Component to convert
     * @return Legacy format string
     */
    private String getLegacyContent(Component message) {
        return LegacyComponentSerializer.legacySection().serialize(message);
    }

    /**
     * Checks message for blocked words
     * @param player Message sender
     * @param message Message content
     * @return true if message blocked, false otherwise
     */
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

    /**
     * Processes @mentions in messages
     * @param sender Message sender
     * @param message Message content
     */
    private void handleMentions(Player sender, String message) {
        Matcher mentionMatcher = mentionPattern.matcher(message);
        while (mentionMatcher.find()) {
            String mentionedPlayerName = mentionMatcher.group(1);
            Player mentionedPlayer = Bukkit.getPlayer(mentionedPlayerName);
            
            if (mentionedPlayer != null && mentionedPlayer.isOnline()) {
                notifyMentionedPlayer(mentionedPlayer, sender);
            }
        }
    }

    /**
     * Notifies mentioned player with sound and message
     * @param mentioned Player mentioned
     * @param sender Message sender
     */
    private void notifyMentionedPlayer(Player mentioned, Player sender) {
        mentioned.sendMessage(ColorUtil.parseComponent(
            messages.getString("mentioned")
                .replace("{player}", sender.getName())
        ));
        mentioned.playSound(mentioned.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }

    /**
     * Processes message capitalization
     * @param message Message to process
     * @return Processed message
     */
    private String processCaps(String message) {
        CapsFilter capsFilter = config.getCapsFilter();
        if (capsFilter.shouldFilter(message)) {
            return capsFilter.filterMessage(message);
        }
        return message;
    }
}

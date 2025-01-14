package com.nonxedy.nonchat.listeners;

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
import com.nonxedy.nonchat.utils.ColorUtil;
import com.nonxedy.nonchat.utils.WordBlocker;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

// Main class that handles chat formatting and implements Bukkit's Listener interface
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

    // Main event handler for chat messages with NORMAL priority
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncChatEvent event) {
        // Cancel the default chat event since we're handling it custom
        event.setCancelled(true);
        // Get the player who sent the message
        Player player = event.getPlayer();
        // Convert the message to legacy format
        String messageContent = getLegacyContent(event.message());
        
        // Check for blocked words and return if message contains them
        if (handleBlockedWords(player, messageContent)) {
            return;
        }

        // Process any mentions in the message
        handleMentions(player, messageContent);
        // Format the message with prefix, suffix, and colors
        Component formattedMessage = formatMessage(player, messageContent);
        
        // Broadcast the formatted message to all players
        Bukkit.broadcast(formattedMessage);
    }

    // Converts modern Component message to legacy string format
    private String getLegacyContent(Component message) {
        return LegacyComponentSerializer.legacySection().serialize(message);
    }

    // Checks if message contains blocked words unless player has bypass permission
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

    // Processes @mentions in messages and notifies mentioned players
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

    // Sends notification and plays sound to mentioned player
    private void notifyMentionedPlayer(Player mentioned, Player sender) {
        mentioned.sendMessage(ColorUtil.parseComponent(
            messages.getString("mentioned")
                .replace("{player}", sender.getName())
        ));
        mentioned.playSound(mentioned.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }

    // Formats the chat message using LuckPerms prefix/suffix and config format
    private Component formatMessage(Player player, String message) {
        // Get LuckPerms user data
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        String prefix = user.getCachedData().getMetaData().getPrefix();
        String suffix = user.getCachedData().getMetaData().getSuffix();
        
        // Handle null prefix/suffix
        prefix = prefix == null ? "" : ColorUtil.parseColor(prefix);
        suffix = suffix == null ? "" : ColorUtil.parseColor(suffix);

        // Apply chat format from config with all placeholders
        String chatFormat = config.getChatFormat()
            .replace("{prefix}", prefix)
            .replace("{suffix}", suffix)
            .replace("{sender}", player.getName())
            .replace("{message}", ColorUtil.parseColor(message));

        // Convert to Component and return
        return ColorUtil.parseComponent(chatFormat);
    }
}

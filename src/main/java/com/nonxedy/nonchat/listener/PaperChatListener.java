package com.nonxedy.nonchat.listener;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.service.ChatService;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Paper chat listener that bridges the old AsyncPlayerChatEvent with the newer AsyncChatEvent.
 *
 * This keeps things compatible with ChatColor2 and other plugins that still rely on
 * the deprecated AsyncPlayerChatEvent, while also playing nicely with Paper’s modern
 * AsyncChatEvent setup.
 *
 * How it works, step by step:
 * 1. AsyncPlayerChatEvent (LOWEST) – Strip the channel prefix so ChatColor2 can do its thing
 * 2. ChatColor2 processes the message and adds colors
 * 3. AsyncPlayerChatEvent (HIGHEST) – Put the channel prefix back and grab the colored message
 * 4. AsyncChatEvent (MONITOR) – Feed the final colored message into our chat system
 */
public class PaperChatListener extends ChatListener {

    private final Nonchat plugin;
    private final Map<String, String> playerMessages = new ConcurrentHashMap<>();
    private final Map<String, String> modifiedPrefixes = new ConcurrentHashMap<>();

    public PaperChatListener(Nonchat plugin, ChatManager chatManager, ChatService chatService) {
        super(chatManager, chatService);
        this.plugin = plugin;
    }

    /**
     * LOWEST priority - Intercept message before ChatColor2 processes it
     * Temporarily remove channel prefix so ChatColor2 can color the actual message content
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    @SuppressWarnings("deprecation") // Required for ChatColor2 compatibility
    public void onAsyncPlayerChatLowest(AsyncPlayerChatEvent event) {
        // Check if message starts with a channel prefix and temporarily remove it for ChatColor2
        String message = event.getMessage();
        if (message.length() > 0 && chatManager != null) {
            // Find channel that matches the message prefix
            var channel = chatManager.getChannelManager().getChannelForMessage(message);
            if (channel != null && channel.hasPrefix() && message.startsWith(channel.getPrefix())) {
                // Temporarily remove the prefix so ChatColor2 can color the message
                event.setMessage(message.substring(channel.getPrefix().length()));
                modifiedPrefixes.put(event.getPlayer().getUniqueId().toString(), channel.getPrefix());
            }
        }
    }

    /**
     * HIGHEST priority - Capture the colored message after ChatColor2 has processed it
     * Restore the channel prefix that was temporarily removed
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    @SuppressWarnings("deprecation") // Required for ChatColor2 compatibility
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerId = player.getUniqueId().toString();
        String message = event.getMessage();

        // If we modified this message earlier, restore the prefix
        String prefix = modifiedPrefixes.remove(playerId);
        if (prefix != null) {
            message = prefix + message;
        }

        // Store the final message (potentially colored by ChatColor2)
        playerMessages.put(playerId, message);
    }

    /**
     * MONITOR priority - Process the final message using Paper's modern AsyncChatEvent
     * This is the modern Paper API that uses Adventure Components
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        String playerId = player.getUniqueId().toString();

        // Use the colored message from AsyncPlayerChatEvent if available, otherwise fall back to plain text
        final String message = playerMessages.remove(playerId); // Remove to prevent memory leaks
        final String finalMessage = message != null ? message : PlainTextComponentSerializer.plainText().serialize(event.message());

        CompletableFuture.runAsync(() -> {
            try {
                if (chatService != null) {
                    chatService.handleChat(player, finalMessage);
                } else if (chatManager != null) {
                    chatManager.processChat(player, finalMessage);
                }
            } catch (Exception e) {
                plugin.logError("Async chat processing failed: " + e.getMessage());
            }
        });
    }
}

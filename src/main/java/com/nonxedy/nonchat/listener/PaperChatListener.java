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

public class PaperChatListener extends ChatListener {

    private final Nonchat plugin;
    private final Map<String, String> playerMessages = new ConcurrentHashMap<>();
    private final Map<String, String> modifiedPrefixes = new ConcurrentHashMap<>();

    public PaperChatListener(Nonchat plugin, ChatManager chatManager, ChatService chatService) {
        super(chatManager, chatService);
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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

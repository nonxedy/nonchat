package com.nonxedy.nonchat.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.service.ChatService;

/**
 * Fallback chat listener for non-Paper servers (Spigot, CraftBukkit)
 * Uses the deprecated AsyncPlayerChatEvent as it's the only option on these platforms
 */
public class BukkitChatListener extends ChatListener {

    public BukkitChatListener(ChatManager chatManager, ChatService chatService) {
        super(chatManager, chatService);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("deprecation") // AsyncPlayerChatEvent is deprecated but required for non-Paper servers
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        // Cancel the default event handling
        event.setCancelled(true);

        // Extract the message content
        String message = event.getMessage();

        // Use service if available, otherwise use manager directly
        if (chatService != null) {
            chatService.handleChat(event.getPlayer(), message);
        } else if (chatManager != null) {
            chatManager.processChat(event.getPlayer(), message);
        }
    }
}

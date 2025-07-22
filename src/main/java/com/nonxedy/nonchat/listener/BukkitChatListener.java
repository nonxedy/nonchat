package com.nonxedy.nonchat.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.service.ChatService;

public class BukkitChatListener extends ChatListener {

    private final Nonchat plugin;

    public BukkitChatListener(Nonchat plugin, ChatManager chatManager, ChatService chatService) {
        super(chatManager, chatService);
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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

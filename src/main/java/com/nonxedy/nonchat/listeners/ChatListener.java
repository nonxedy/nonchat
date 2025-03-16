package com.nonxedy.nonchat.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.service.ChatService;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatListener implements Listener {
    private final ChatManager chatManager;
    private final ChatService chatService;

    // Constructor with both dependencies
    public ChatListener(ChatManager chatManager, ChatService chatService) {
        this.chatManager = chatManager;
        this.chatService = chatService;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncChatEvent event) {
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        // Use service if available, otherwise use manager directly
        if (chatService != null) {
            chatService.handleChat(event.getPlayer(), message);
        } else if (chatManager != null) {
            chatManager.processChat(event.getPlayer(), message);
        }
    }
}

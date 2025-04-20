package com.nonxedy.nonchat.listener;

import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.service.ChatService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class BukkitChatListener extends ChatListener {
    
    public BukkitChatListener(ChatManager chatManager, ChatService chatService) {
        super(chatManager, chatService);
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

package com.nonxedy.nonchat.listener;

import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.service.ChatService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class PaperChatListener extends ChatListener {
    
    public PaperChatListener(ChatManager chatManager, ChatService chatService) {
        super(chatManager, chatService);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        // Cancel the default event handling
        event.setCancelled(true);
        
        // Extract the message content from the component
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        // Use service if available, otherwise use manager directly
        if (chatService != null) {
            chatService.handleChat(event.getPlayer(), message);
        } else if (chatManager != null) {
            chatManager.processChat(event.getPlayer(), message);
        }
    }
}

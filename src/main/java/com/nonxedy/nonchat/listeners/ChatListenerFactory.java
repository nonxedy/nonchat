package com.nonxedy.nonchat.listeners;

import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.service.ChatService;

public class ChatListenerFactory {
    
    public static ChatListener createChatListener(ChatManager chatManager, ChatService chatService) {
        try {
            // Try to load Paper's AsyncChatEvent class
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            return new PaperChatListener(chatManager, chatService);
        } catch (ClassNotFoundException e) {
            // If Paper's event doesn't exist, use Bukkit's event
            return new BukkitChatListener(chatManager, chatService);
        }
    }
}

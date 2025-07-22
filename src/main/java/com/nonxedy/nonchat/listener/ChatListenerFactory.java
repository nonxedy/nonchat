package com.nonxedy.nonchat.listener;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.service.ChatService;

public class ChatListenerFactory {

    public static ChatListener createChatListener(Nonchat plugin, ChatManager chatManager, ChatService chatService) {
        try {
            // Try to load Paper's AsyncChatEvent class
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            return new PaperChatListener(plugin, chatManager, chatService);
        } catch (ClassNotFoundException e) {
            // If Paper's event doesn't exist, use Bukkit's event
            return new BukkitChatListener(plugin, chatManager, chatService);
        }
    }
}

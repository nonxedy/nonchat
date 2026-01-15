package com.nonxedy.nonchat.listener;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.service.ChatService;

/**
 * Factory for creating the appropriate chat listener based on server platform
 * - Paper servers: Uses modern AsyncChatEvent (Adventure API)
 * - Spigot/CraftBukkit: Uses legacy AsyncPlayerChatEvent
 */
public class ChatListenerFactory {

    public static ChatListener createChatListener(Nonchat plugin, ChatManager chatManager, ChatService chatService) {
        try {
            // Try to load Paper's AsyncChatEvent class
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            return new PaperChatListener(plugin, chatManager, chatService);
        } catch (ClassNotFoundException e) {
            // If Paper's event doesn't exist, use Bukkit's event (Spigot/CraftBukkit)
            return new BukkitChatListener(chatManager, chatService);
        }
    }
}

package com.nonxedy.nonchat.listener;

import org.bukkit.event.Listener;

import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.service.ChatService;

public abstract class ChatListener implements Listener {
    protected final ChatManager chatManager;
    protected final ChatService chatService;

    // Constructor with both dependencies
    public ChatListener(ChatManager chatManager, ChatService chatService) {
        this.chatManager = chatManager;
        this.chatService = chatService;
    }
}

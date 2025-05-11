package com.nonxedy.nonchat.service;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nonxedy.nonchat.api.IMessageHandler;
import com.nonxedy.nonchat.command.impl.IgnoreCommand;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.core.BroadcastManager;
import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.core.MessageManager;

public class ChatService implements IMessageHandler {
    private final ChatManager chatManager;
    private final MessageManager messageManager;
    private final BroadcastManager broadcastManager;
    private final PluginConfig config;
    private IgnoreCommand ignoreCommand;

    public ChatService(ChatManager chatManager, MessageManager messageManager, 
                      BroadcastManager broadcastManager, PluginConfig config) {
        this.chatManager = chatManager;
        this.messageManager = messageManager;
        this.broadcastManager = broadcastManager;
        this.config = config;
    }

    @Override
    public void handleChat(Player player, String message) {
        chatManager.processChat(player, message);
    }

    @Override
    public void handlePrivateMessage(Player sender, Player receiver, String message) {
        if (ignoreCommand != null) {
            if (ignoreCommand.isIgnoring(receiver, sender)) {
                return;
            }
            
            if (ignoreCommand.isIgnoring(sender, receiver)) {
                return;
            }
        }
        
        messageManager.sendPrivateMessage(sender, receiver, message);
    }

    @Override
    public void handleBroadcast(CommandSender sender, String message) {
        broadcastManager.broadcast(sender, message);
    }

    @Override
    public void handleStaffChat(Player sender, String message) {
        // Instead of using the old staff chat format, we'll use the chat manager with the staff chat prefix
        chatManager.processChat(sender, "@" + message);
    }
    
    /**
     * Sets the ignore command instance.
     * @param ignoreCommand The ignore command instance
     */
    public void setIgnoreCommand(IgnoreCommand ignoreCommand) {
        this.ignoreCommand = ignoreCommand;
    }
}

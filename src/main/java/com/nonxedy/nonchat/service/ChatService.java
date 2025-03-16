package com.nonxedy.nonchat.service;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nonxedy.nonchat.api.IMessageHandler;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.core.BroadcastManager;
import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.core.MessageManager;
import com.nonxedy.nonchat.util.ColorUtil;

public class ChatService implements IMessageHandler {
    private final ChatManager chatManager;
    private final MessageManager messageManager;
    private final BroadcastManager broadcastManager;
    private final PluginConfig config;

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
        messageManager.sendPrivateMessage(sender, receiver, message);
    }

    @Override
    public void handleBroadcast(CommandSender sender, String message) {
        broadcastManager.broadcast(sender, message);
    }

    @Override
    public void handleStaffChat(Player sender, String message) {
        String format = config.getScFormat()
            .replace("{sender}", sender.getName())
            .replace("{message}", message);
        
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.hasPermission("nonchat.staffchat"))
            .forEach(player -> player.sendMessage(ColorUtil.parseComponent(format)));
    }
}

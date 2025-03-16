package com.nonxedy.nonchat.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nonxedy.nonchat.command.impl.SpyCommand;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.util.ColorUtil;

import net.kyori.adventure.text.Component;

public class MessageManager {
    private final PluginConfig config;
    private final PluginMessages messages;
    private final SpyCommand spyCommand;
    private final Map<UUID, UUID> lastMessageSender;

    public MessageManager(PluginConfig config, PluginMessages messages, SpyCommand spyCommand) {
        this.config = config;
        this.messages = messages;
        this.spyCommand = spyCommand;
        this.lastMessageSender = new HashMap<>();
    }

    public void sendPrivateMessage(Player sender, Player receiver, String message) {
        lastMessageSender.put(receiver.getUniqueId(), sender.getUniqueId());

        String senderFormat = config.getPrivateChatFormat()
            .replace("{sender}", sender.getName())
            .replace("{target}", receiver.getName())
            .replace("{message}", message);
            
        String receiverFormat = config.getPrivateChatFormat()
            .replace("{sender}", sender.getName())
            .replace("{target}", receiver.getName())
            .replace("{message}", message);

        sender.sendMessage(ColorUtil.parseComponent(senderFormat));
        receiver.sendMessage(ColorUtil.parseComponent(receiverFormat));

        spyCommand.onPrivateMessage(sender, receiver, Component.text(message));
    }

    public void replyToLastMessage(Player sender, String message) {
        UUID lastSenderUUID = lastMessageSender.get(sender.getUniqueId());
        if (lastSenderUUID == null) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-reply-target")));
            return;
        }

        Player receiver = Bukkit.getPlayer(lastSenderUUID);
        if (receiver == null || !receiver.isOnline()) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("player-offline")));
            return;
        }

        sendPrivateMessage(sender, receiver, message);
    }

    public Player getLastMessageSender(Player player) {
        UUID lastSenderUUID = lastMessageSender.get(player.getUniqueId());
        return lastSenderUUID != null ? Bukkit.getPlayer(lastSenderUUID) : null;
    }

    public void clearLastMessageSender(Player player) {
        lastMessageSender.remove(player.getUniqueId());
    }
}

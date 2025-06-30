package com.nonxedy.nonchat.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.command.impl.IgnoreCommand;
import com.nonxedy.nonchat.command.impl.SpyCommand;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

import net.kyori.adventure.text.Component;

public class MessageManager {
    private final Nonchat plugin;
    private final PluginConfig config;
    private final PluginMessages messages;
    private final SpyCommand spyCommand;
    private final Map<UUID, UUID> lastMessageSender;
    private IgnoreCommand ignoreCommand;

    public MessageManager(Nonchat plugin, PluginConfig config, PluginMessages messages, SpyCommand spyCommand) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.spyCommand = spyCommand;
        this.lastMessageSender = new HashMap<>();
    }

    public Map<UUID, UUID> getLastMessageSender() {
        return lastMessageSender;
    }

    public void sendPrivateMessage(Player sender, Player receiver, String message) {
        if (ignoreCommand != null && ignoreCommand.isIgnoring(receiver, sender)) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("ignored-by-target")));
            return;
        }
        
        if (ignoreCommand != null && ignoreCommand.isIgnoring(sender, receiver)) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("you-are-ignoring-player")
                    .replace("%player%", receiver.getName())));
            return;
        }
        
        lastMessageSender.put(receiver.getUniqueId(), sender.getUniqueId());

        // Process message with color permission for both sender and receiver
        String processedMessage = sender.hasPermission("nonchat.color") ? message : ColorUtil.stripAllColors(message);
        
        String senderFormat = config.getPrivateChatFormat()
            .replace("{sender}", sender.getName())
            .replace("{target}", receiver.getName())
            .replace("{message}", processedMessage);
            
        String receiverFormat = config.getPrivateChatFormat()
            .replace("{sender}", sender.getName())
            .replace("{target}", receiver.getName())
            .replace("{message}", processedMessage);

        sender.sendMessage(ColorUtil.parseComponent(senderFormat));
        receiver.sendMessage(ColorUtil.parseComponent(receiverFormat));

        spyCommand.onPrivateMessage(sender, receiver, Component.text(processedMessage));
    }

    public void replyToLastMessage(Player sender, String message) {
        UUID lastSenderUUID = getLastMessageSender().get(sender.getUniqueId());
        if (lastSenderUUID == null) {
            plugin.logError("No last message sender found for player " + sender.getName());
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
    
    /**
     * Sets the ignore command instance.
     * @param ignoreCommand The ignore command instance
     */
    public void setIgnoreCommand(IgnoreCommand ignoreCommand) {
        this.ignoreCommand = ignoreCommand;
    }
}

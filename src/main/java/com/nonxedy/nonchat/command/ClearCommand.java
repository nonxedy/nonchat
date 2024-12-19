package com.nonxedy.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ClearCommand implements CommandExecutor {
    
    private final PluginMessages messages;
    private final nonchat plugin;
    private static final int CLEAR_LINES = 100;

    public ClearCommand(PluginMessages messages, nonchat plugin) {
        this.messages = messages;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                        @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);

        if (!hasPermission(sender)) {
            return true;
        }

        clearChat();
        sendClearNotification();
        
        return true;
    }

    private boolean hasPermission(CommandSender sender) {
        if (!sender.hasPermission("nonchat.clear")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("Player attempted to clear chat without permission");
            return false;
        }
        return true;
    }

    private void clearChat() {
        try {
            Component emptyLine = Component.empty();
            for (int i = 0; i < CLEAR_LINES; i++) {
                Bukkit.broadcast(emptyLine);
            }
            plugin.logResponse("Chat cleared successfully");
        } catch (Exception e) {
            plugin.logError("Failed to clear chat: " + e.getMessage());
            Bukkit.broadcast(Component.text("Failed to clear chat")
                    .color(NamedTextColor.RED));
        }
    }

    private void sendClearNotification() {
        Bukkit.broadcast(ColorUtil.parseComponent(messages.getString("chat-cleared")));
    }
}
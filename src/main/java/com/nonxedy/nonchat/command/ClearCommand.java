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

public class ClearCommand implements CommandExecutor {
    
    private PluginMessages messages;
    private nonchat plugin;

    public ClearCommand(PluginMessages messages, nonchat plugin) {
        this.messages = messages;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);

        if (!sender.hasPermission("nonchat.clear")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("You don't have permission to clear the chat.");
            return true;
        }

        sender.sendMessage(ColorUtil.parseComponent(messages.getString("clear-chat")));
        plugin.logResponse("Clearing chat...");
        
        try {
            for (int i = 0; i < 100; i++) {
                Bukkit.broadcast(Component.empty());
            }

        plugin.logResponse("Chat cleared.");

        Bukkit.broadcast(ColorUtil.parseComponent(messages.getString("chat-cleared")));
        } catch (Exception e) {
            plugin.logError("There was an error clearing the chat: " + e.getMessage());
        }
        return true;
    }
}

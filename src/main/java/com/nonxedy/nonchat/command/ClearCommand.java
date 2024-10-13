package com.nonxedy.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.config.PluginMessages;
import com.nonxedy.nonchat.nonchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

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
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                    .build());
            plugin.logError("You don't have permission to clear the chat.");
            return true;
        }

        sender.sendMessage(Component.text()
                .append(Component.text(messages.getClearChat(), TextColor.fromHexString("#E088FF")))
                .build());
        plugin.logResponse("Clearing chat...");
        
        try {
            for (int i = 0; i < 100; i++) {
                Bukkit.broadcast(Component.empty());
            }

        plugin.logResponse("Chat cleared.");

        Bukkit.broadcast(Component.text()
                .append(Component.text(messages.getChatCleared(), TextColor.fromHexString("#52FFA6")))
                .build());
        } catch (Exception e) {
            plugin.logError("There was an error clearing the chat: " + e.getMessage());
        }
        return true;
    }
}

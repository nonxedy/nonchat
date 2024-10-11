package com.hgtoiwr.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.hgtoiwr.config.PluginMessages;
import com.hgtoiwr.nonchat.nonchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class ClearCommand implements CommandExecutor {
    
    private PluginMessages messages;

    public ClearCommand(PluginMessages messages) {
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        nonchat plugin = (nonchat) Bukkit.getPluginManager().getPlugin("nonchat");
        plugin.logCommand(command.getName(), args);

        if (!sender.hasPermission("nonchat.clear")) {
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                    .build());
            return true;
        }

        sender.sendMessage(Component.text()
                .append(Component.text(messages.getClearChat(), TextColor.fromHexString("#E088FF")))
                .build());
        
        try {
        for (int i = 0; i < 100; i++) {
            Bukkit.broadcast(Component.empty());
        }

        plugin.logResponse("Чат очищен.");

        Bukkit.broadcast(Component.text()
                .append(Component.text(messages.getChatCleared(), TextColor.fromHexString("#52FFA6")))
                .build());
        } catch (Exception e) {
            plugin.logError("Ошибка очистки чата: " + e.getMessage());
        }
        return true;
    }
}

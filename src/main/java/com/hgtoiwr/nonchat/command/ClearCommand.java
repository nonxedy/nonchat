package com.hgtoiwr.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.hgtoiwr.config.PluginMessages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class ClearCommand implements CommandExecutor {
    
    private PluginMessages messages;

    public ClearCommand(PluginMessages messages) {
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("nonchat.clear")) {
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                    .build());
            return true;
        }

        sender.sendMessage(Component.text()
                .append(Component.text(messages.getClearChat(), TextColor.fromHexString("#E088FF")))
                .build());

        for (int i = 0; i < 100; i++) {
            Bukkit.broadcast(Component.empty());
        }

        Bukkit.broadcast(Component.text()
                .append(Component.text(messages.getChatCleared(), TextColor.fromHexString("#52FFA6")))
                .build());

        return true;
    }
}

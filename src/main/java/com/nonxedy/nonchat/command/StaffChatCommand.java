package com.nonxedy.nonchat.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.config.PluginMessages;
import com.nonxedy.nonchat.nonchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class StaffChatCommand implements CommandExecutor {

    private nonchat plugin;
    private PluginMessages messages;

    public StaffChatCommand(nonchat plugin, PluginMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);
        
        if (!sender.hasPermission("nonchat.staffchat")) {
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                    .build());
            plugin.logError("You don't have permission to send broadcast.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getInvalidUsageSc(), TextColor.fromHexString("#ADF3FD")))
                    .build());
            plugin.logError("Invalid usage for staffchat command");
            return true;
        }
        
        String message = String.join(" ", args);

        try {
            plugin.getServer().getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("nonchat.sc"))
                .forEach(player -> player.sendMessage(Component.text()
                .append(Component.text("[STAFF CHAT] ", TextColor.fromHexString("#ADF3FD")))
                .append(Component.text(sender.getName() + ": ", TextColor.fromHexString("#84B8FF ")))
                .append(Component.text(message, TextColor.fromHexString("#FFFFFF")))
                .build()));
        } catch (Exception e) {
            plugin.logError("There was an error sending message in staff chat: " + e.getMessage());
        }
        return true;
    }
}
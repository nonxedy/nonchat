package com.nonxedy.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.config.PluginMessages;
import com.nonxedy.nonchat.nonchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class BroadcastCommand implements CommandExecutor {

    private PluginMessages messages;
    private nonchat plugin;

    public BroadcastCommand(PluginMessages messages, nonchat plugin) {
        this.messages = messages;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);
        
        if (command.getName().equalsIgnoreCase("broadcast") ||
            command.getName().equalsIgnoreCase("bc")) {

            if (!sender.hasPermission("nonchat.broadcast")) {
                sender.sendMessage(Component.text()
                        .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                        .build());
                plugin.logError("You don't have permission to send broadcast.");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(Component.text()
                        .append(Component.text(messages.getBroadcastCommand(), TextColor.fromHexString("#ADF3FD")))
                        .build());
                plugin.logError("Invalid usage: /broadcast <message>");
                return true;
            }

            StringBuilder message = new StringBuilder();
            for (String arg : args) {
                message.append(arg).append(" ");
            }

            try {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(" ");
                    player.sendMessage(Component.text()
                            .append(Component.text(messages.getBroadcast(), TextColor.fromHexString("#E088FF")))
                            .append(Component.text(message.toString().trim(), TextColor.fromHexString("#FFFFFF")))
                            .build());
                    player.sendMessage(" ");

                    plugin.logResponse("Broadcast sent.");

                    Bukkit.getConsoleSender().sendMessage(Component.text()
                            .append(Component.text(messages.getBroadcast(), TextColor.fromHexString("#E088FF")))
                            .append(Component.text(message.toString().trim(), TextColor.fromHexString("#FFFFFF")))
                            .build());
                }
            } catch (Exception e) {
                plugin.logError("There was an error sending broadcast: " + e.getMessage());
            }
            return true;
        }
        return false;
    }
}

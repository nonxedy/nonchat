package com.nonxedy.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

import net.kyori.adventure.text.Component;

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
                sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
                plugin.logError("You don't have permission to send broadcast.");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(ColorUtil.parseComponent(messages.getString("broadcast-command")));
                plugin.logError("Invalid usage: /broadcast <message>");
                return true;
            }

            StringBuilder message = new StringBuilder();
            for (String arg : args) {
                message.append(arg).append(" ");
            }

            try {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Component broadcastMessage = ColorUtil.parseComponent(
                        messages.getString("broadcast")
                            .replace("{message}", message)
                    );

                    player.sendMessage(" ");
                    player.sendMessage(broadcastMessage);
                    player.sendMessage(" ");

                    plugin.logResponse("Broadcast sent.");

                    Bukkit.getConsoleSender().sendMessage(broadcastMessage);
                }
            } catch (Exception e) {
                plugin.logError("There was an error sending broadcast: " + e.getMessage());
            }
            return true;
        }
        return false;
    }
}

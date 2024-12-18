package com.nonxedy.nonchat.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

import net.kyori.adventure.text.Component;

public class BroadcastCommand implements CommandExecutor, TabCompleter {

    private final PluginMessages messages;
    private final nonchat plugin;

    public BroadcastCommand(PluginMessages messages, nonchat plugin) {
        this.messages = messages;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                    @NotNull String label, @NotNull String[] args) {
        plugin.logCommand(command.getName(), args);

        if (!sender.hasPermission("nonchat.broadcast")) {
            sendNoPermissionMessage(sender);
            return true;
        }

        if (args.length == 0) {
            sendUsageMessage(sender);
            return true;
        }

        broadcastMessage(String.join(" ", args));
        return true;
    }

    private void sendNoPermissionMessage(CommandSender sender) {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
        plugin.logError("Sender doesn't have broadcast permission");
    }

    private void sendUsageMessage(CommandSender sender) {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("broadcast-command")));
        plugin.logError("Invalid usage: /bc <message>");
    }

    private void broadcastMessage(String message) {
        try {
            Component broadcastComponent = ColorUtil.parseComponent(
                messages.getString("broadcast")
                    .replace("{message}", message)
            );

            plugin.getServer().getOnlinePlayers().forEach(player -> {
                player.sendMessage(Component.empty());
                player.sendMessage(broadcastComponent);
                player.sendMessage(Component.empty());
            });

            plugin.getServer().getConsoleSender().sendMessage(broadcastComponent);
            plugin.logResponse("Broadcast sent successfully");
        } catch (Exception e) {
            plugin.logError("Failed to send broadcast: " + e.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                        @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
package com.hgtoiwr.nonchat.command;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.hgtoiwr.config.PluginMessages;
import com.hgtoiwr.nonchat.nonchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class IgnoreCommand implements CommandExecutor {

    private PluginMessages messages;
    private nonchat plugin;

    public IgnoreCommand(nonchat plugin, PluginMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);

        if (!sender.hasPermission("nonchat.ignore")) {
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                    .build());
            plugin.logError("No permission for ignore command");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getInvalidUsageIgnore(), TextColor.fromHexString("#FF5252")))
                    .build());
            plugin.logError("Invalid arguments for ignore command");
            return true;
        }

        UUID senderUUID = ((Player) sender).getUniqueId();
        Player targetPlayer = Bukkit.getPlayer(args[0]);

        if (targetPlayer == null) {
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getPlayerNotFound(), TextColor.fromHexString("#FF5252")))
                    .build());
            plugin.logError("Player not found for ignore command");
            return true;
        }

        UUID targetUUID = Bukkit.getPlayer(args[0]).getUniqueId();

        try {
            if (plugin.ignoredPlayers.containsKey(senderUUID)) {
                Set<UUID> ignored = plugin.ignoredPlayers.get(senderUUID);
                if (ignored.contains(targetUUID)) {
                    ignored.remove(targetUUID);
                    sender.sendMessage(Component.text()
                            .append(Component.text(messages.getUnignoredPlayer(args[0]), TextColor.fromHexString("#52FFA6")))
                            .build());
                    plugin.logResponse("Player unignored");
                } else {
                    ignored.add(targetUUID);
                    sender.sendMessage(Component.text()
                            .append(Component.text(messages.getIgnoredPlayer(args[0]), TextColor.fromHexString("#E088FF")))
                            .build());
                    plugin.logResponse("Player ignored");
                }
            } else {
                Set<UUID> ignored = new HashSet<>();
                ignored.add(targetUUID);
                plugin.ignoredPlayers.put(senderUUID, ignored);
                sender.sendMessage(Component.text()
                        .append(Component.text(messages.getIgnoredPlayer(args[0]), TextColor.fromHexString("#E088FF")))
                        .build());
                plugin.logResponse("Player ignored");
            }
        } catch (Exception e) {
            plugin.logError("There was an error ignoring player: " + e.getMessage());
        }
        return true;
    }
}
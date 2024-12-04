package com.nonxedy.nonchat.command;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
            sender.sendMessage(Component.text(ColorUtil.parseColor(messages.getNoPermission())));
            plugin.logError("No permission for ignore command");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text(ColorUtil.parseColor(messages.getInvalidUsageIgnore())));
            plugin.logError("Invalid arguments for ignore command");
            return true;
        }

        UUID senderUUID = ((Player) sender).getUniqueId();
        Player targetPlayer = Bukkit.getPlayer(args[0]);

        if (targetPlayer == null) {
            sender.sendMessage(Component.text(ColorUtil.parseColor(messages.getPlayerNotFound())));
            plugin.logError("Player not found for ignore command");
            return true;
        }

        UUID targetUUID = Bukkit.getPlayer(args[0]).getUniqueId();

        try {
            if (plugin.ignoredPlayers.containsKey(senderUUID)) {
                Set<UUID> ignored = plugin.ignoredPlayers.get(senderUUID);
                if (ignored.contains(targetUUID)) {
                    ignored.remove(targetUUID);
                    sender.sendMessage(Component.text(ColorUtil.parseColor(messages.getUnignoredPlayer(args[0]))));
                    plugin.logResponse("Player unignored");
                } else {
                    ignored.add(targetUUID);
                    sender.sendMessage(Component.text(ColorUtil.parseColor(messages.getIgnoredPlayer(args[0]))));
                    plugin.logResponse("Player ignored");
                }
            } else {
                Set<UUID> ignored = new HashSet<>();
                ignored.add(targetUUID);
                plugin.ignoredPlayers.put(senderUUID, ignored);
                sender.sendMessage(Component.text(ColorUtil.parseColor(messages.getIgnoredPlayer(args[0]))));
                plugin.logResponse("Player ignored");
            }
        } catch (Exception e) {
            plugin.logError("There was an error ignoring player: " + e.getMessage());
        }
        return true;
    }
}
package com.nonxedy.nonchat.command;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

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

        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("player-only")));
            plugin.logError("Ignore command can only be used by players");
            return true;
        }

        if (!sender.hasPermission("nonchat.ignore")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("No permission for ignore command");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("invalid-usage-ignore")));
            plugin.logError("Invalid arguments for ignore command");
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("player-not-found")));
            plugin.logError("Player not found for ignore command");
            return true;
        }

        if (target == player) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("ignored-by-target")));
            plugin.logError("Cannot ignore self");
            return true;
        }

        try {
            if (plugin.ignoredPlayers.containsKey(player.getUniqueId()) &&
                plugin.ignoredPlayers.get(player.getUniqueId()).contains(target.getUniqueId())) {
                    plugin.ignoredPlayers.get(player.getUniqueId()).remove(target.getUniqueId());
                    sender.sendMessage(ColorUtil.parseComponent(
                        messages.getString("unignored-player")
                            .replace("{player}", target.getName())
                    ));
                    plugin.logResponse("Player unignored");
                } else {
                    plugin.ignoredPlayers.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>())
                        .add(target.getUniqueId());
                    sender.sendMessage(ColorUtil.parseComponent(
                        messages.getString("ignored-player")
                            .replace("{player}", target.getName())
                    ));
                    plugin.logResponse("Player ignored");

                    target.sendMessage(ColorUtil.parseComponent(
                        messages.getString("ignored-by-target")
                            .replace("{player}", player.getName())
                    ));
                }
        } catch (Exception e) {
            plugin.logError("There was an error ignoring player: " + e.getMessage());
        }
        return true;
    }
}
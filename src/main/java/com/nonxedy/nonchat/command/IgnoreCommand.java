package com.nonxedy.nonchat.command;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

public class IgnoreCommand implements CommandExecutor {

    private final nonchat plugin;
    private final PluginMessages messages;

    public IgnoreCommand(nonchat plugin, PluginMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        plugin.logCommand(command.getName(), args);

        Player player = (Player) sender;
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("player-only")));
            plugin.logError("Ignore command can only be used by players");
            return true;
        }

        if (!player.hasPermission("nonchat.ignore")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("No permission for ignore command");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("invalid-usage-ignore")));
            plugin.logError("Invalid arguments for ignore command");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("player-not-found")));
            plugin.logError("Player not found for ignore command");
            return true;
        }

        if (target == player) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("cannot-ignore-self")));
            plugin.logError("Cannot ignore self");
            return true;
        }

        handleIgnoreToggle(player, target);
        return true;
    }

    private void handleIgnoreToggle(Player player, Player target) {
        try {
            UUID playerUUID = player.getUniqueId();
            UUID targetUUID = target.getUniqueId();

            if (isIgnored(playerUUID, targetUUID)) {
                removeIgnore(player, target);
            } else {
                addIgnore(player, target);
            }
        } catch (Exception e) {
            plugin.logError("Error handling ignore toggle: " + e.getMessage());
        }
    }

    private boolean isIgnored(UUID playerUUID, UUID targetUUID) {
        return plugin.ignoredPlayers.containsKey(playerUUID) && 
            plugin.ignoredPlayers.get(playerUUID).contains(targetUUID);
    }

    private void removeIgnore(Player player, Player target) {
        plugin.ignoredPlayers.get(player.getUniqueId()).remove(target.getUniqueId());
        player.sendMessage(ColorUtil.parseComponent(messages.getString("unignored-player").replace("{player}", target.getName())));
        plugin.logResponse("Player unignored successfully");
    }

    private void addIgnore(Player player, Player target) {
        plugin.ignoredPlayers.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>())
            .add(target.getUniqueId());
        player.sendMessage(ColorUtil.parseComponent(messages.getString("ignored-player").replace("{player}", target.getName())));
        target.sendMessage(ColorUtil.parseComponent(messages.getString("ignored-by-target").replace("{player}", player.getName())));
        plugin.logResponse("Player ignored successfully");
    }
}

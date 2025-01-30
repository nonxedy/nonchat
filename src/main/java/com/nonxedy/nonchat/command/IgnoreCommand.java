package com.nonxedy.nonchat.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

// Class implements the player ignore command functionality in chat
public class IgnoreCommand implements CommandExecutor, TabCompleter {

    // Instance of the main plugin
    private final nonchat plugin;
    // Plugin messages configuration
    private final PluginMessages messages;

    // Constructor to initialize the command
    public IgnoreCommand(nonchat plugin, PluginMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    // Method executes when a player enters the command
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Log the executed command
        plugin.logCommand(command.getName(), args);

        // Convert sender to player
        Player player = (Player) sender;
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("player-only")));
            plugin.logError("Ignore command can only be used by players");
            return true;
        }

        // Check permissions for using the command
        if (!player.hasPermission("nonchat.ignore")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("No permission for ignore command");
            return true;
        }

        // Validate command usage
        if (args.length != 1) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("invalid-usage-ignore")));
            plugin.logError("Invalid arguments for ignore command");
            return true;
        }

        // Find target player
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("player-not-found")));
            plugin.logError("Player not found for ignore command");
            return true;
        }

        // Check if player tries to ignore themselves
        if (target == player) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("cannot-ignore-self")));
            plugin.logError("Cannot ignore self");
            return true;
        }

        // Handle ignore toggle
        handleIgnoreToggle(player, target);
        return true;
    }

    // Method to handle ignore toggle logic
    private void handleIgnoreToggle(Player player, Player target) {
        try {
            UUID playerUUID = player.getUniqueId();
            UUID targetUUID = target.getUniqueId();

            // Check current ignore status
            if (isIgnored(playerUUID, targetUUID)) {
                removeIgnore(player, target);
            } else {
                addIgnore(player, target);
            }
        } catch (Exception e) {
            plugin.logError("Error handling ignore toggle: " + e.getMessage());
        }
    }

    // Check if player is being ignored
    private boolean isIgnored(UUID playerUUID, UUID targetUUID) {
        return plugin.ignoredPlayers.containsKey(playerUUID) && 
            plugin.ignoredPlayers.get(playerUUID).contains(targetUUID);
    }

    // Remove player from ignore list
    private void removeIgnore(Player player, Player target) {
        plugin.ignoredPlayers.get(player.getUniqueId()).remove(target.getUniqueId());
        player.sendMessage(ColorUtil.parseComponent(messages.getString("unignored-player").replace("{player}", target.getName())));
        plugin.logResponse("Player unignored successfully");
    }

    // Add player to ignore list
    private void addIgnore(Player player, Player target) {
        plugin.ignoredPlayers.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>())
            .add(target.getUniqueId());
        player.sendMessage(ColorUtil.parseComponent(messages.getString("ignored-player").replace("{player}", target.getName())));
        target.sendMessage(ColorUtil.parseComponent(messages.getString("ignored-by-target").replace("{player}", player.getName())));
        plugin.logResponse("Player ignored successfully");
    }

    // Provide tab completion suggestions
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                    @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("nonchat.ignore")) {
            return Collections.emptyList();
        }

        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equals(sender.getName()))
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return suggestions;
    }
}

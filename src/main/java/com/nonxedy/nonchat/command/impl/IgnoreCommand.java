package com.nonxedy.nonchat.command.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.nonxedy.nonchat.util.ColorUtil;

/**
 * Manages player ignore functionality
 * Allows players to block messages from specific users
 */
public class IgnoreCommand implements CommandExecutor, TabCompleter {

    // Instance of the main plugin
    private final nonchat plugin;
    // Plugin messages configuration
    private final PluginMessages messages;
    // Map to store ignored players (key: player UUID, value: set of ignored player UUIDs)
    private final Map<UUID, Set<UUID>> ignoredPlayers = new HashMap<>();

    // Constructor to initialize the command
    public IgnoreCommand(nonchat plugin, PluginMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    /**
     * Handles ignore command execution
     * @param sender Command sender
     * @param command Command being executed
     * @param label Command label used
     * @param args Command arguments
     * @return true if command handled successfully
     */
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

    /**
     * Handles ignore status toggling
     * @param player Player toggling ignore
     * @param target Target to ignore/unignore
     */
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

    /**
     * Checks if target is ignored by player
     * @param playerUUID Player UUID
     * @param targetUUID Target UUID
     * @return true if target is ignored
     */
    private boolean isIgnored(UUID playerUUID, UUID targetUUID) {
        return ignoredPlayers.containsKey(playerUUID) && 
            ignoredPlayers.get(playerUUID).contains(targetUUID);
    }

    /**
     * Removes target from player's ignore list
     * @param player Player removing ignore
     * @param target Target to unignore
     */
    private void removeIgnore(Player player, Player target) {
        ignoredPlayers.get(player.getUniqueId()).remove(target.getUniqueId());
        player.sendMessage(ColorUtil.parseComponent(messages.getString("unignored-player")
                            .replace("{player}", target.getName())));
        plugin.logResponse("Player unignored successfully");
    }

    /**
     * Adds target to player's ignore list
     * @param player Player adding ignore
     * @param target Target to ignore
     */
    private void addIgnore(Player player, Player target) {
        ignoredPlayers.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>())
            .add(target.getUniqueId());
        player.sendMessage(ColorUtil.parseComponent(messages.getString("ignored-player")
                            .replace("{player}", target.getName())));
        target.sendMessage(ColorUtil.parseComponent(messages.getString("ignored-by-target")
                            .replace("{player}", player.getName())));
        plugin.logResponse("Player ignored successfully");
    }

    /**
     * Provides tab completion suggestions
     * @param sender Command sender
     * @param command Command being completed
     * @param label Command label used
     * @param args Current arguments
     * @return List of suggestions
     */
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
    
    /**
     * Checks if a player is ignoring another player
     * @param player The player who might be ignoring
     * @param target The potentially ignored player
     * @return true if player is ignoring target
     */
    public boolean isIgnoring(Player player, Player target) {
        return isIgnored(player.getUniqueId(), target.getUniqueId());
    }
    
    /**
     * Gets all players ignored by a specific player
     * @param player The player to check
     * @return Set of ignored player UUIDs
     */
    public Set<UUID> getIgnoredPlayers(Player player) {
        return ignoredPlayers.getOrDefault(player.getUniqueId(), Collections.emptySet());
    }
}

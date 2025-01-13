package com.nonxedy.nonchat.command;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

// Main command class for handling spy functionality
public class SpyCommand implements CommandExecutor {

    // Plugin instance reference
    private final nonchat plugin;
    // Messages configuration reference
    private final PluginMessages messages;
    // Plugin configuration reference
    private final PluginConfig pluginConfig;
    // Set to store players who are currently spying
    private final Set<Player> spyPlayers;

    // Constructor initializes all necessary dependencies
    public SpyCommand(nonchat plugin, PluginMessages messages, PluginConfig pluginConfig) {
        this.plugin = plugin;
        this.messages = messages;
        this.pluginConfig = pluginConfig;
        // Initialize empty set for spy players
        this.spyPlayers = new HashSet<>();
    }

    // Command execution handler
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Log the command execution
        plugin.logCommand(command.getName(), args);

        // Cast sender to Player
        Player player = (Player) sender;
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("player-only")));
            return true;
        }

        // Check if player has permission to use spy command
        if (!player.hasPermission("nonchat.spy")) {
            player.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("Player " + player.getName() + " tried to use spy command without permission");
            return true;
        }

        // Toggle spy mode for the player
        toggleSpyMode(player);
        return true;
    }

    // Helper method to toggle spy mode status
    private void toggleSpyMode(Player player) {
        try {
            // If player is already spying, disable it
            if (spyPlayers.contains(player)) {
                spyPlayers.remove(player);
                player.sendMessage(ColorUtil.parseComponent(messages.getString("spy-mode-disabled")));
                plugin.logResponse("Spy mode disabled for " + player.getName());
            } else {
                // If player is not spying, enable it
                spyPlayers.add(player);
                player.sendMessage(ColorUtil.parseComponent(messages.getString("spy-mode-enabled")));
                plugin.logResponse("Spy mode enabled for " + player.getName());
            }
        } catch (Exception e) {
            // Log any errors during toggle operation
            plugin.logError("Error toggling spy mode for " + player.getName() + ": " + e.getMessage());
        }
    }

    // Method to handle private messages and send them to spies
    public void onPrivateMessage(Player sender, Player target, Component message) {
        // Convert message component to plain text
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(message);
        // Format the spy message using config template
        String spyFormat = pluginConfig.getSpyFormat()
                .replace("{sender}", sender.getName())
                .replace("{target}", target.getName())
                .replace("{message}", plainMessage);

        // Send formatted message to all spies except sender and target
        for (Player spy : spyPlayers) {
            if (spy != sender && spy != target && spy.isOnline()) {
                spy.sendMessage(ColorUtil.parseComponent(spyFormat));
                plugin.logResponse("Spy " + spy.getName() + " received message: " + spyFormat);
            }
        }
    }

    // Check if a player is currently spying
    public boolean isSpying(Player player) {
        return spyPlayers.contains(player);
    }

    // Get a copy of the current spy players set
    public Set<Player> getSpyPlayers() {
        return new HashSet<>(spyPlayers);
    }
}

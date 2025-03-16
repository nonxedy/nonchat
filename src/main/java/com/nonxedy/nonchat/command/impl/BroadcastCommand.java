package com.nonxedy.nonchat.command.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.util.ColorUtil;

import net.kyori.adventure.text.Component;

/**
 * Handles server-wide broadcast functionality
 * Provides command to send formatted announcements
 */
public class BroadcastCommand implements CommandExecutor, TabCompleter {

    // Reference to plugin messages for localization
    private final PluginMessages messages;
    // Reference to main plugin instance
    private final nonchat plugin;

    // Initialize command with required dependencies
    public BroadcastCommand(PluginMessages messages, nonchat plugin) {
        this.messages = messages;
        this.plugin = plugin;
    }

    /**
     * Handles broadcast command execution
     * @param sender Command sender
     * @param command Command being executed
     * @param label Command label used
     * @param args Command arguments
     * @return true if command handled successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                    @NotNull String label, @NotNull String[] args) {
        // Log command execution for tracking
        plugin.logCommand(command.getName(), args);

        // Check if sender has broadcast permission
        if (!sender.hasPermission("nonchat.broadcast")) {
            sendNoPermissionMessage(sender);
            return true;
        }

        // Verify command has message content
        if (args.length == 0) {
            sendUsageMessage(sender);
            return true;
        }

        // Combine args into message and broadcast
        broadcastMessage(String.join(" ", args));
        return true;
    }

    /**
     * Sends permission denied message
     * @param sender Command sender
     */
    private void sendNoPermissionMessage(CommandSender sender) {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
        plugin.logError("Sender doesn't have broadcast permission");
    }

    /**
     * Sends command usage information
     * @param sender Command sender
     */
    private void sendUsageMessage(CommandSender sender) {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("broadcast-command")));
        plugin.logError("Invalid usage: /bc <message>");
    }

    /**
     * Broadcasts formatted message to all players
     * @param message Message to broadcast
     */
    private void broadcastMessage(String message) {
        try {
            // Create formatted message component
            Component broadcastComponent = ColorUtil.parseComponent(
                messages.getString("broadcast")
                    .replace("{message}", message)
            );

            // Send to all online players with spacing
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                player.sendMessage(Component.empty());
                player.sendMessage(broadcastComponent);
                player.sendMessage(Component.empty());
            });

            // Send to server console
            plugin.getServer().getConsoleSender().sendMessage(broadcastComponent);
            plugin.logResponse("Broadcast sent successfully");
        } catch (Exception e) {
            plugin.logError("Failed to send broadcast: " + e.getMessage());
        }
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
        if (!sender.hasPermission("nonchat.broadcast")) {
            return Collections.emptyList();
        }

        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            suggestions.add("message");
        }
        
        return suggestions.stream()
                .filter(suggestion -> suggestion.toLowerCase()
                .startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}

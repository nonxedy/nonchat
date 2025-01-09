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

// Main class that handles broadcast commands in the plugin
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

    // Handle the broadcast command execution
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

    // Send no permission message to command sender
    private void sendNoPermissionMessage(CommandSender sender) {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
        plugin.logError("Sender doesn't have broadcast permission");
    }

    // Send proper command usage information
    private void sendUsageMessage(CommandSender sender) {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("broadcast-command")));
        plugin.logError("Invalid usage: /bc <message>");
    }

    // Process and send the broadcast message
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

    // Provide empty tab completion as no suggestions needed
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                        @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}

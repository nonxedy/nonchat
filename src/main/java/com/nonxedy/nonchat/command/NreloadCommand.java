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

// Main command class that handles the reload functionality
public class NreloadCommand implements CommandExecutor, TabCompleter {

    // Store reference to main plugin instance
    private final nonchat plugin;
    // Store reference to plugin messages configuration
    private final PluginMessages messages;

    // Constructor initializes plugin and messages references
    public NreloadCommand(nonchat plugin, PluginMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    // Main command execution method that handles the reload command
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                        @NotNull String label, @NotNull String[] args) {
        
        // Check if sender has permission before proceeding
        if (!hasReloadPermission(sender)) {
            return true;
        }

        // Execute the reload process
        performReload(sender);
        return true;
    }

    // Verify if sender has the required permission
    private boolean hasReloadPermission(CommandSender sender) {
        if (!sender.hasPermission("nonchat.nreload")) {
            sendNoPermissionMessage(sender);
            return false;
        }
        return true;
    }

    // Send no permission message to sender and log the denial
    private void sendNoPermissionMessage(CommandSender sender) {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
        plugin.logError("Permission denied: nonchat.nreload");
    }

    // Handle the entire reload process with proper messaging
    private void performReload(CommandSender sender) {
        // Send initial reload message
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("reloading")));
        plugin.logResponse("Initiating config reload...");

        try {
            // Attempt to execute reload operations
            executeReload();
            // Send success message if reload completes
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("reloaded")));
            plugin.logResponse("Configuration reload successful");
        } catch (Exception e) {
            // Handle any errors during reload
            handleReloadError(sender, e);
        }
    }

    // Execute all reload operations
    private void executeReload() {
        // Reload main configuration
        plugin.reloadConfig();
        // Reload debugger settings
        plugin.reloadDebugger();
        // Stop auto broadcast functionality
        plugin.stopAutoBroadcastSender();
        // Re-register utility components
        plugin.registerUtils();
    }

    // Handle and log any errors during reload
    private void handleReloadError(CommandSender sender, Exception e) {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("reload-failed")));
        plugin.logError("Configuration reload failed: " + e.getMessage());
        e.printStackTrace();
    }

    // Tab completion method - returns empty list as no arguments are needed
    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                        @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}

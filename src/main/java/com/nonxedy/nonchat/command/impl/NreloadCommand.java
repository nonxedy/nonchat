package com.nonxedy.nonchat.command.impl;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.service.ConfigService;
import com.nonxedy.nonchat.util.ColorUtil;

/**
 * Handles plugin configuration reloading
 * Provides command to refresh settings at runtime
 */
public class NreloadCommand implements CommandExecutor, TabCompleter {

    // Store reference to main plugin instance
    private final nonchat plugin;
    // Store reference to plugin messages configuration
    private final PluginMessages messages;
    // Optional reference to config service for service-based architecture
    private final ConfigService configService;

    // Constructor initializes plugin and messages references
    public NreloadCommand(nonchat plugin, PluginMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.configService = null;
    }
    
    // Alternative constructor for service-based architecture
    public NreloadCommand(nonchat plugin, ConfigService configService) {
        this.plugin = plugin;
        this.configService = configService;
        this.messages = configService.getMessages();
    }

    /**
     * Handles reload command execution
     * @param sender Command sender
     * @param command Command being executed
     * @param label Command label used
     * @param args Command arguments
     * @return true if command handled successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                        @NotNull String label, @NotNull String[] args) {
        
        // Log command execution
        plugin.logCommand(command.getName(), args);
        
        // Check if sender has permission before proceeding
        if (!hasReloadPermission(sender)) {
            return true;
        }

        // Execute the reload process
        performReload(sender);
        return true;
    }

    /**
     * Verifies sender has reload permission
     * @param sender Command sender to check
     * @return true if sender has permission
     */
    private boolean hasReloadPermission(CommandSender sender) {
        if (!sender.hasPermission("nonchat.nreload")) {
            sendNoPermissionMessage(sender);
            return false;
        }
        return true;
    }

    /**
     * Sends permission denied message
     * @param sender Command sender
     */
    private void sendNoPermissionMessage(CommandSender sender) {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
        plugin.logError("Permission denied: nonchat.nreload");
    }

    /**
     * Executes reload process with messaging
     * @param sender Command sender
     */
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
        // Use service-based reload if available
        if (configService != null) {
            configService.reload();
            plugin.reloadConfig();
        } else {
            // Legacy reload method
            plugin.reloadConfig();
            
            // These methods need to be added to the nonchat class
            if (plugin instanceof nonchat) {
                ((nonchat) plugin).reloadServices();
            }
        }
    }

    /**
     * Handles reload errors with messaging
     * @param sender Command sender
     * @param e Exception that occurred
     */
    private void handleReloadError(CommandSender sender, Exception e) {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("reload-failed")));
        plugin.logError("Configuration reload failed: " + e.getMessage());
        e.printStackTrace();
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
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                        @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}

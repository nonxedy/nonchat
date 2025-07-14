package com.nonxedy.nonchat.command.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.service.ConfigService;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

import net.kyori.adventure.text.Component;

/**
 * Main nonchat command handler
 * Provides subcommands for reload and help functionality
 */
public class NonchatCommand implements CommandExecutor, TabCompleter {

    // Store reference to main plugin instance
    private final Nonchat plugin;
    // Store reference to plugin messages configuration
    private final PluginMessages messages;
    // Store reference to config service for reloading
    private final ConfigService configService;

    /**
     * Constructor to initialize command with dependencies
     */
    public NonchatCommand(Nonchat plugin, ConfigService configService) {
        this.plugin = plugin;
        this.configService = configService;
        this.messages = configService.getMessages();
    }

    /**
     * Handles command execution with subcommands
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                        @NotNull String label, @NotNull String[] args) {
        // Log command execution
        plugin.logCommand(command.getName(), args);

        // Check if no arguments provided, show help
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        // Process subcommands
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload":
                return handleReloadCommand(sender);
            case "help":
                return handleHelpCommand(sender);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    /**
     * Handles the reload subcommand
     */
    private boolean handleReloadCommand(CommandSender sender) {
        // Check if sender has permission
        if (!sender.hasPermission("nonchat.reload")) {
            sender.sendMessage(ColorUtil.parseComponentCached(messages.getString("no-permission")));
            plugin.logError("Permission denied: nonchat.reload");
            return true;
        }

        // Perform the reload
        try {
            // Send reload start message
            sender.sendMessage(ColorUtil.parseComponentCached(messages.getString("reloading")));
            plugin.logResponse("Initiating config reload...");

            // Execute reload operations
            executeReload();
            
            // Send success message
            sender.sendMessage(ColorUtil.parseComponentCached(messages.getString("reloaded")));
            plugin.logResponse("Configuration reload successful");
        } catch (Exception e) {
            // Handle any errors during reload
            sender.sendMessage(ColorUtil.parseComponentCached(messages.getString("reload-failed")));
            plugin.logError("Configuration reload failed: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Execute reload operations using the config service
     */
    private void executeReload() {
        configService.reload();
        plugin.reloadConfig();
        
        // These methods need to be added to the nonchat class if not already there
        if (plugin instanceof Nonchat) {
            ((Nonchat) plugin).reloadServices();
        }
    }

    /**
     * Handles the help subcommand
     */
    private boolean handleHelpCommand(CommandSender sender) {
        // Check if sender has permission
        if (!sender.hasPermission("nonchat.help")) {
            sender.sendMessage(ColorUtil.parseComponentCached(messages.getString("no-permission")));
            plugin.logError("Permission denied: nonchat.help");
            return true;
        }

        // Send the help message
        sendHelpMessage(sender);
        return true;
    }

    /**
     * Sends the formatted help message to the sender
     */
    private void sendHelpMessage(CommandSender sender) {
        try {
            // Create and send the help message
            Component helpMessage = Component.empty()
                .append(ColorUtil.parseComponentCached(messages.getString("help")))
                .append(Component.newline())
                .append(getCommandsList());

            sender.sendMessage(helpMessage);
            plugin.logResponse("Help message sent successfully");
        } catch (Exception e) {
            plugin.logError("Failed to send help message: " + e.getMessage());
        }
    }

    /**
     * Builds list of available commands
     */
    private Component getCommandsList() {
        // Create an empty component and append all command descriptions
        return Component.empty()
            // Add reload command description
            .append(ColorUtil.parseComponentCached(messages.getString("nreload")))
            .append(Component.newline())
            // Add help command description
            .append(ColorUtil.parseComponentCached(messages.getString("help-command")))
            .append(Component.newline())
            // Add server command description
            .append(ColorUtil.parseComponentCached(messages.getString("server-command")))
            .append(Component.newline())
            // Add message command description
            .append(ColorUtil.parseComponentCached(messages.getString("message-command")))
            .append(Component.newline())
            // Add broadcast command description
            .append(ColorUtil.parseComponentCached(messages.getString("broadcast-command")))
            .append(Component.newline())
            // Add ignore command description
            .append(ColorUtil.parseComponentCached(messages.getString("ignore-command")))
            .append(Component.newline())
            // Add spy command description
            .append(ColorUtil.parseComponentCached(messages.getString("spy-command")))
            .append(Component.newline())
            // Add me command description
            .append(ColorUtil.parseComponentCached(messages.getString("me-command")))
            .append(Component.newline())
            // Add roll command description
            .append(ColorUtil.parseComponentCached(messages.getString("roll-command")))
            .append(Component.newline())
            // Add channel command description
            .append(ColorUtil.parseComponentCached(messages.getString("channel-command"))
            .append(Component.newline())
            // Add channel command description
            .append(ColorUtil.parseComponentCached(messages.getString("reply-command"))));
    }

    /**
     * Provides tab completion for subcommands
     */
    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                        @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            
            // Add reload subcommand if they have permission
            if (sender.hasPermission("nonchat.reload")) {
                subCommands.add("reload");
            }
            
            // Add help subcommand if they have permission
            if (sender.hasPermission("nonchat.help")) {
                subCommands.add("help");
            }
            
            return filterStartingWith(args[0], subCommands);
        }
        
        // No completions for args beyond the first
        return Collections.emptyList();
    }
    
    /**
     * Filters a list of strings to only those starting with a prefix
     */
    private List<String> filterStartingWith(String prefix, List<String> options) {
        List<String> result = new ArrayList<>();
        
        for (String option : options) {
            if (option.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(option);
            }
        }
        
        return result;
    }
}

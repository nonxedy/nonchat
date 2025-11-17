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
import com.nonxedy.nonchat.util.chat.filters.LinkDetector;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

import net.kyori.adventure.text.Component;

/**
 * Main nonchat command handler
 * Provides subcommands for reload, help, and version functionality
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
            case "reload" -> {
                return handleReloadCommand(sender);
            }
            case "help" -> {
                return handleHelpCommand(sender);
            }
            case "version" -> {
                return handleVersionCommand(sender);
            }
            case "debug" -> {
                return handleDebugCommand(sender, args);
            }
            default -> {
                sendHelpMessage(sender);
                return true;
            }
        }
    }

    /**
     * Handles the reload subcommand
     */
    private boolean handleReloadCommand(CommandSender sender) {
        // Check if sender has permission
        if (!sender.hasPermission("nonchat.reload")) {
            sender.sendMessage(ColorUtil.parseComponentCached(messages.getString("no-permission")));
            plugin.logError("No permission for /nonchat reload command: " + sender.getName());
            return true;
        }

        // Perform the full reload
        try {
            // Send reload start message
            sender.sendMessage(ColorUtil.parseComponentCached(messages.getString("reloading")));
            plugin.logResponse("Initiating config reload...");

            // Execute reload operations (includes death messages)
            executeReload();

            // Send success message
            sender.sendMessage(ColorUtil.parseComponentCached(messages.getString("reloaded")));
            plugin.logResponse("Configuration reload successful");
        } catch (Exception e) {
            // Handle any errors during reload
            sender.sendMessage(ColorUtil.parseComponentCached(messages.getString("reload-failed")));
            plugin.logError("Configuration reload failed: " + e.getMessage());
        }

        return true;
    }

    /**
     * Execute reload operations using the config service
     */
    private void executeReload() {
        configService.reload();
        plugin.reloadConfig();

        // Reload death messages
        plugin.reloadDeathMessages();

        // These methods need to be added to the nonchat class if not already there
        if (plugin instanceof Nonchat nonchat) {
            nonchat.reloadServices();
        }
    }

    /**
     * Handles the help subcommand
     */
    private boolean handleHelpCommand(CommandSender sender) {
        // Check if sender has permission
        if (!sender.hasPermission("nonchat.help")) {
            sender.sendMessage(ColorUtil.parseComponentCached(messages.getString("no-permission")));
            plugin.logError("No permission for /nonchat help command: " + sender.getName());
            return true;
        }

        // Send the help message
        sendHelpMessage(sender);
        return true;
    }

    /**
     * Handles the version subcommand
     */
    private boolean handleVersionCommand(CommandSender sender) {
        // Check if sender has permission
        if (!sender.hasPermission("nonchat.version")) {
            sender.sendMessage(ColorUtil.parseComponentCached(messages.getString("no-permission")));
            plugin.logError("No permission for /nonchat version command: " + sender.getName());
            return true;
        }

        // Send the version message
        sendVersionMessage(sender);
        return true;
    }
    
    /**
     * Handles the debug subcommand
     */
    private boolean handleDebugCommand(CommandSender sender, String[] args) {
        // Check if sender has permission
        if (!sender.hasPermission("nonchat.admin.debug")) {
            sender.sendMessage(ColorUtil.parseComponentCached(messages.getString("no-permission")));
            plugin.logError("No permission for /nonchat debug command: " + sender.getName());
            return true;
        }
        
        // Check for subcommand
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parseComponent("<red>Usage: /nonchat debug <tracking|stats>"));
            return true;
        }
        
        String debugSubCommand = args[1].toLowerCase();
        switch (debugSubCommand) {
            case "tracking" -> {
                // Display indirect death tracking statistics
                plugin.logDeathTrackingStatistics();
                sender.sendMessage(ColorUtil.parseComponent("<green>Tracking statistics logged to console. Check server logs."));
                return true;
            }
            case "stats" -> {
                // Display death message statistics
                java.util.Map<org.bukkit.event.entity.EntityDamageEvent.DamageCause, Integer> stats = 
                    plugin.getDeathMessageStatistics();
                if (stats.isEmpty()) {
                    sender.sendMessage(ColorUtil.parseComponent("<yellow>No death message statistics available."));
                } else {
                    sender.sendMessage(ColorUtil.parseComponent("<green>=== Death Message Statistics ==="));
                    int total = 0;
                    for (java.util.Map.Entry<org.bukkit.event.entity.EntityDamageEvent.DamageCause, Integer> entry : stats.entrySet()) {
                        sender.sendMessage(ColorUtil.parseComponent(
                            "<gray>" + entry.getKey().name() + ": <white>" + entry.getValue() + " variants"
                        ));
                        total += entry.getValue();
                    }
                    sender.sendMessage(ColorUtil.parseComponent("<green>Total: <white>" + total + " message variants"));
                }
                return true;
            }
            default -> {
                sender.sendMessage(ColorUtil.parseComponent("<red>Unknown debug subcommand. Use: tracking, stats"));
                return true;
            }
        }
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
     * Sends the version message to the sender
     */
    private void sendVersionMessage(CommandSender sender) {
        try {
            // Get plugin version from plugin.yml
            String version = plugin.getDescription().getVersion();
            // Replace placeholder in version message
            String versionMessage = messages.getString("version").replace("{version}", version);
            // Make links clickable in the version message
            Component versionComponent = LinkDetector.makeLinksClickable(versionMessage);
            sender.sendMessage(versionComponent);
            plugin.logResponse("Version message sent successfully");
        } catch (Exception e) {
            plugin.logError("Failed to send version message: " + e.getMessage());
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
            // Add version command description
            .append(ColorUtil.parseComponentCached(messages.getString("version-command")))
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
            .append(ColorUtil.parseComponentCached(messages.getString("channel-command")))
            .append(Component.newline())
            // Add reply command description
            .append(ColorUtil.parseComponentCached(messages.getString("reply-command")));
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

            // Add version subcommand if they have permission
            if (sender.hasPermission("nonchat.version")) {
                subCommands.add("version");
            }
            
            // Add debug subcommand if they have permission
            if (sender.hasPermission("nonchat.admin.debug")) {
                subCommands.add("debug");
            }

            return filterStartingWith(args[0], subCommands);
        }
        
        // Tab completion for debug subcommands
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            if (sender.hasPermission("nonchat.admin.debug")) {
                List<String> debugSubCommands = List.of("tracking", "stats");
                return filterStartingWith(args[1], debugSubCommands);
            }
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

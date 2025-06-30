package com.nonxedy.nonchat.command.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

/**
 * Handles roleplay action command functionality
 * Broadcasts player actions in third person
 */
public class MeCommand implements CommandExecutor, TabCompleter {

    private final Nonchat plugin;
    private final PluginConfig config;
    private final PluginMessages messages;

    // Constructor to initialize all required dependencies
    public MeCommand(Nonchat plugin, PluginConfig config, PluginMessages messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    /**
     * Handles me command execution
     * @param sender Command sender
     * @param command Command being executed
     * @param label Command label used
     * @param args Command arguments
     * @return true if command handled successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);

        if (!validateCommandUsage(sender)) {
            return true;
        }

        try {
            broadcastMeMessage(sender, args);
        } catch (Exception e) {
            handleError(sender, e);
        }

        return true;
    }

    /**
     * Validates command usage permissions
     * @param sender Command sender
     * @return true if usage allowed
     */
    private boolean validateCommandUsage(CommandSender sender) {
        if (!config.isMeCommandEnabled()) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("command-disabled")));
            plugin.logError("Me command is disabled");
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("player-only")));
            plugin.logError("Me command used by non-player");
            return false;
        }

        if (!sender.hasPermission("nonchat.me")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("No permission for me command: " + sender.getName());
            return false;
        }

        return true;
    }

    /**
     * Broadcasts roleplay action message
     * @param sender Command sender
     * @param args Command arguments
     */
    private void broadcastMeMessage(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("invalid-usage-me")));
            plugin.logError("Empty me command message from: " + sender.getName());
            return;
        }

        String message = String.join(" ", args);
        String formattedMessage = config.getMeFormat()
            .replace("{message}", message);

        plugin.getServer().broadcast(ColorUtil.parseComponent(formattedMessage));
        plugin.logResponse("Me command executed: " + sender.getName() + " - " + message);
    }

    /**
     * Handles command execution errors
     * @param sender Command sender
     * @param e Exception that occurred
     */
    private void handleError(CommandSender sender, Exception e) {
        sender.sendMessage(ColorUtil.parseComponent("&cAn error occurred while executing the command"));
        plugin.logError("Me command error: " + e.getMessage());
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
        if (!sender.hasPermission("nonchat.me")) {
            return Collections.emptyList();
        }
    
        if (args.length == 1) {
            List<String> suggestions = Arrays.asList(
                "is jumping"
            );
            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
    
        return Collections.emptyList();
    }
}

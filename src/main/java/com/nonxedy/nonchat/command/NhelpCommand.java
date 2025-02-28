package com.nonxedy.nonchat.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

import net.kyori.adventure.text.Component;

/**
 * Handles help command functionality
 * Displays available commands and their usage
 */
public class NhelpCommand implements CommandExecutor {

    // Store reference to plugin messages configuration
    private final PluginMessages messages;
    // Store reference to main plugin instance
    private final nonchat plugin;

    // Constructor to initialize the command with required dependencies
    public NhelpCommand(PluginMessages messages, nonchat plugin) {
        this.messages = messages;
        this.plugin = plugin;
    }

    /**
     * Handles help command execution
     * @param sender Command sender
     * @param command Command being executed
     * @param label Command label used
     * @param args Command arguments
     * @return true if command handled successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Log the command execution
        plugin.logCommand(command.getName(), args);

        // Check if sender has permission to use this command
        if (!sender.hasPermission("nonchat.nhelp")) {
            // Send no permission message if they don't have access
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("Permission denied: nonchat.nhelp");
            return true;
        }

        // Send the help message to the player
        sendHelpMessage(sender);
        return true;
    }

    /**
     * Sends formatted help message to sender
     * @param sender Command sender
     */
    private void sendHelpMessage(CommandSender sender) {
        try {
            // Create an empty component and append help header and commands list
            Component helpMessage = Component.empty()
                .append(ColorUtil.parseComponent(messages.getString("help")))
                .append(Component.newline())
                .append(getCommandsList());

            // Send the complete help message to the player
            sender.sendMessage(helpMessage);
            plugin.logResponse("Help message sent successfully");
        } catch (Exception e) {
            // Log any errors that occur while sending the message
            plugin.logError("Failed to send help message: " + e.getMessage());
        }
    }

    /**
     * Builds list of available commands
     * @return Formatted component with commands
     */
    private Component getCommandsList() {
        // Create an empty component and append all command descriptions
        return Component.empty()
            // Add /nreload command description
            .append(ColorUtil.parseComponent(messages.getString("nreload")))
            .append(Component.newline())
            // Add help command description
            .append(ColorUtil.parseComponent(messages.getString("help-command")))
            .append(Component.newline())
            // Add server command description
            .append(ColorUtil.parseComponent(messages.getString("server-command")))
            .append(Component.newline())
            // Add message command description
            .append(ColorUtil.parseComponent(messages.getString("message-command")))
            .append(Component.newline())
            // Add broadcast command description
            .append(ColorUtil.parseComponent(messages.getString("broadcast-command")))
            .append(Component.newline())
            // Add ignore command description
            .append(ColorUtil.parseComponent(messages.getString("ignore-command")))
            .append(Component.newline())
            // Add staff chat command description
            .append(ColorUtil.parseComponent(messages.getString("sc-command")))
            .append(Component.newline())
            // Add spy command description
            .append(ColorUtil.parseComponent(messages.getString("spy-command")))
            .append(Component.newline())
            // Add me command description
            .append(ColorUtil.parseComponent(messages.getString("me-command")))
            .append(Component.newline())
            // Add roll command description
            .append(ColorUtil.parseComponent(messages.getString("roll-command")));
    }
}

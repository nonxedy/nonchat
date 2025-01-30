package com.nonxedy.nonchat.command;

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

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

// Main class that handles staff chat commands
public class StaffChatCommand implements CommandExecutor, TabCompleter {

    // Class fields for plugin instance, messages, and configuration
    private final nonchat plugin;
    private final PluginMessages messages;
    private final PluginConfig config;
    
    // Define static color constants for staff chat formatting
    private static final TextColor STAFF_CHAT_COLOR = TextColor.fromHexString("#ADF3FD");
    private static final TextColor MESSAGE_COLOR = TextColor.fromHexString("#FFFFFF");

    // Constructor to initialize the command with required dependencies
    public StaffChatCommand(nonchat plugin, PluginMessages messages, PluginConfig config) {
        this.plugin = plugin;
        this.messages = messages;
        this.config = config;
    }

    // Main command execution method that handles the staff chat command
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Log the executed command for tracking
        plugin.logCommand(command.getName(), args);

        // Check if sender has permission to use staff chat
        if (!sender.hasPermission("nonchat.sc")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("You don't have permission to send broadcast.");
            return true;
        }

        // Verify command has arguments (the message)
        if (args.length < 1) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("invalid-usage-sc")));
            plugin.logError("Invalid usage for staffchat command");
            return true;
        }

        // Join all arguments into a single message
        String message = String.join(" ", args);
        broadcastStaffMessage(sender, message);
        return true;
    }

    // Method to broadcast the staff message to all players with permission
    private void broadcastStaffMessage(CommandSender sender, String message) {
        // Get format settings from config
        String staffChatFormat = config.getScFormat();
        String staffChatName = config.getStaffChatName();
        
        // Create the formatted staff message
        Component staffMessage = createStaffMessage(sender, staffChatName, staffChatFormat, message);
        
        // Send message to all players with staff chat permission
        try {
            plugin.getServer().getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("nonchat.sc"))
                .forEach(player -> player.sendMessage(staffMessage));
        } catch (Exception e) {
            plugin.logError("There was an error sending message in staff chat: " + e.getMessage());
        }
    }

    // Method to create a formatted staff message with prefix/suffix support
    private Component createStaffMessage(CommandSender sender, String staffChatName, String staffChatFormat, String message) {
        // Initialize sender information
        String senderName = sender.getName();
        String prefix = "";
        String suffix = "";

        // Handle player-specific formatting using LuckPerms
        Player player = (Player) sender;
        if (sender instanceof Player) {
            // Get LuckPerms user data for prefix/suffix
            User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                prefix = user.getCachedData().getMetaData().getPrefix();
                suffix = user.getCachedData().getMetaData().getSuffix();
            }
        } else {
            // Set sender name as Console if not a player
            senderName = "Console";
        }

        // Replace placeholders in the message format
        String formattedMessage = staffChatFormat
            .replace("{sender}", senderName)
            .replace("{prefix}", prefix != null ? prefix : "")
            .replace("{suffix}", suffix != null ? suffix : "")
            .replace("{message}", message);

        // Build and return the final component with colors
        return Component.text()
            .append(Component.text(staffChatName + " ", STAFF_CHAT_COLOR))
            .append(Component.text(formattedMessage, MESSAGE_COLOR))
            .build();
    }

    // Provide tab completion suggestions
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                        @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("nonchat.sc")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> suggestions = Arrays.asList(
                "message"
            );

            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}

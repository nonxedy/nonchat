package com.nonxedy.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

// Chat clear command class implementing CommandExecutor interface
public class ClearCommand implements CommandExecutor {
    
    // Fields to store plugin messages and main class instance
    private final PluginMessages messages;
    private final nonchat plugin;
    // Constant defining number of empty lines for chat clearing
    private static final int CLEAR_LINES = 100;

    // Class constructor accepting required dependencies
    public ClearCommand(PluginMessages messages, nonchat plugin) {
        this.messages = messages;
        this.plugin = plugin;
    }

    // Method called when command is executed
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                        @NotNull String label, String[] args) {
        // Log command execution
        plugin.logCommand(command.getName(), args);

        // Check permissions to execute command
        if (!hasPermission(sender)) {
            return true;
        }

        // Clear chat and send notification
        clearChat();
        sendClearNotification();
        
        return true;
    }

    // Method to check sender's permissions
    private boolean hasPermission(CommandSender sender) {
        // Check for nonchat.clear permission
        if (!sender.hasPermission("nonchat.clear")) {
            // Send no permission message
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            // Log attempt to execute command without permission
            plugin.logError("Player attempted to clear chat without permission");
            return false;
        }
        return true;
    }

    // Method to clear chat
    private void clearChat() {
        try {
            // Create empty component for clearing
            Component emptyLine = Component.empty();
            // Send empty lines to all players
            for (int i = 0; i < CLEAR_LINES; i++) {
                Bukkit.broadcast(emptyLine);
            }
            // Log successful clearing
            plugin.logResponse("Chat cleared successfully");
        } catch (Exception e) {
            // Handle clearing errors
            plugin.logError("Failed to clear chat: " + e.getMessage());
            // Send error message to all players
            Bukkit.broadcast(Component.text("Failed to clear chat")
                    .color(NamedTextColor.RED));
        }
    }

    // Method to send chat clear notification
    private void sendClearNotification() {
        // Send message to all players that chat was cleared
        Bukkit.broadcast(ColorUtil.parseComponent(messages.getString("chat-cleared")));
    }
}

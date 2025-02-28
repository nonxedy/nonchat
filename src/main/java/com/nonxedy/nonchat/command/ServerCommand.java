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
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.format.TextColor;

/**
 * Handles server information command
 * Displays system and server statistics
 */
public class ServerCommand implements CommandExecutor {

    // Store plugin messages configuration
    private final PluginMessages messages;
    // Store main plugin instance
    private final nonchat plugin;
    
    // Constructor to initialize messages and plugin
    public ServerCommand(PluginMessages messages, nonchat plugin) {
        this.messages = messages;
        this.plugin = plugin;
    }

    /**
     * Handles server command execution
     * @param sender Command sender
     * @param command Command being executed
     * @param label Command label used
     * @param args Command arguments
     * @return true if command handled successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Log the executed command
        plugin.logCommand(command.getName(), args);
        
        // Check if sender has permission to use this command
        if (!sender.hasPermission("nonchat.server")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            return true;
        }

        // Try to send server info, handle any errors
        try {
            sendServerInfo(sender);
            plugin.logResponse("Server info shown successfully");
        } catch (Exception e) {
            plugin.logError("Failed to show server info: " + e.getMessage());
            sender.sendMessage(Component.text("An error occurred while fetching server info")
                .color(TextColor.color(255, 0, 0)));
        }
        
        return true;
    }
    
    /**
     * Sends server information to command sender
     * @param sender Command sender to receive info
     */
    private void sendServerInfo(CommandSender sender) {
        // Build server info message using builder pattern
        ServerInfoBuilder info = new ServerInfoBuilder()
            // Add header line
            .addLine(messages.getString("server-info"))
            // Add various server information entries
            .addInfo(messages.getString("java-version"), System.getProperty("java.version"))
            .addInfo(messages.getString("port"), String.valueOf(Bukkit.getServer().getPort()))
            .addInfo(messages.getString("os-name"), System.getProperty("os.name"))
            .addInfo(messages.getString("os-version"), System.getProperty("os.version"))
            .addInfo(messages.getString("cpu-cores"), String.valueOf(Runtime.getRuntime().availableProcessors()))
            .addInfo(messages.getString("cpu-family"), System.getenv().getOrDefault("PROCESSOR_IDENTIFIER", "Unknown"))
            .addInfo(messages.getString("number-of-plugins"), String.valueOf(Bukkit.getPluginManager().getPlugins().length))
            .addInfo(messages.getString("number-of-worlds"), String.valueOf(Bukkit.getWorlds().size()));
            
        // Send the built message to the command sender
        sender.sendMessage(info.build());
    }
    
    // Helper class to build formatted server information
    
    // Ill create file for that class
    private static class ServerInfoBuilder {
        // Component builder for creating formatted text
        private final Builder builder = Component.text();
        
        // Add a single line of text with newline
        public ServerInfoBuilder addLine(String message) {
            builder.append(ColorUtil.parseComponent(message + "\n"));
            return this;
        }
        
        // Add a label-value pair with newline
        public ServerInfoBuilder addInfo(String label, String value) {
            builder.append(ColorUtil.parseComponent(label + value + "\n"));
            return this;
        }
        
        // Build and return the final component
        public Component build() {
            return builder.build();
        }
    }
}

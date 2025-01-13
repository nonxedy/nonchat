package com.nonxedy.nonchat.config;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nonxedy.nonchat.utils.ColorUtil;
import com.nonxedy.nonchat.utils.MessageFormatter;

import net.kyori.adventure.text.Component;

// Main class for handling plugin messages and configurations
public class PluginMessages {

    // File object for messages.yml
    private File file;
    // Configuration object to store messages
    private FileConfiguration messages;
    // Message formatter utility instance
    private final MessageFormatter formatter;

    // Constructor initializes the messages system
    public PluginMessages() {
        // Set up the messages.yml file in plugins/nonchat directory
        file = new File("plugins/nonchat", "messages.yml");
        // Initialize the message formatter
        this.formatter = new MessageFormatter(this);
        // Create default config if file doesn't exist
        if (!file.exists()) {
            createDefaultConfig();
        }
        // Load the configuration from file
        messages = YamlConfiguration.loadConfiguration(file);
    }
    
    // Creates default configuration with predefined messages
    private void createDefaultConfig() {
        try {
            // Create necessary directories and file
            file.getParentFile().mkdirs();
            file.createNewFile();
    
            // Initialize new configuration
            messages = new YamlConfiguration();
            
            // Set default messages for various plugin functions
            messages.set("no-permission", "You do not have permission to use this command!");
            messages.set("player-only", "Only players can use this command!");
            messages.set("server-info", "Server Information:");
            messages.set("java-version", "Java Version: ");
            messages.set("port", "Port: ");
            messages.set("version", "Version: ");
            messages.set("os-name", "OS Name: ");
            messages.set("os-version", "OS Version: ");
            messages.set("cpu-cores", "CPU Cores: ");
            messages.set("cpu-family", "CPU Family: ");
            messages.set("number-of-plugins", "Number of Plugins: ");
            messages.set("number-of-worlds", "Number of Worlds: ");
            messages.set("reloading", "Reloading...");
            messages.set("reloaded", "Plugin reloaded!");
            messages.set("reload-failed", "Failed to reload plugin!");
            messages.set("help", "nonchat | commands:");
            messages.set("nreload", "/nreload - reload plugin");
            messages.set("help-command", "/help - commands list");
            messages.set("server-command", "/server - server information");
            messages.set("message-command", "/m <player> <message> (msg, w, whisper, message) - sent a message to a player");
            messages.set("broadcast-command", "/bc <message> (broadcast) - sent a message to all server");
            messages.set("ignore-command", "/ignore <player> - ignore a player");
            messages.set("sc-command", "/sc <message> - sent a message to staff");
            messages.set("spy-command", "/spy - enable/disable spy mode");
            messages.set("clear-chat", "Chat clearing...");
            messages.set("chat-cleared", "Chat cleared!");
            messages.set("broadcast", "Broadcast: {message}");
            messages.set("player-not-found", "Player not found.");
            messages.set("invalid-usage-message", "Use: /m <player> <message>");
            messages.set("invalid-usage-ignore", "Use: /ignore <player>");
            messages.set("invalid-usage-sc", "Use: /sc <message>");
            messages.set("invalid-usage-spy", "Use: /spy");
            messages.set("cannot-ignore-self", "You cant ignore yourself..");
            messages.set("ignored-player", "You started ignored the player {player}.");
            messages.set("unignored-player", "You no longer ignore the player {player}.");
            messages.set("ignored-by-target", "This player ignores you and you cant send him a message.");
            messages.set("spy-mode-enabled", "Spy mode enabled.");
            messages.set("spy-mode-disabled", "Spy mode disabled.");
            messages.set("blocked-words", "You are not allowed to use this word!");
            messages.set("mentioned", "You were mentioned in chat by {player}!");
            
            // Save the configuration to file
            messages.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Getter methods for each message type
    // Returns colored string for no permission message
    public String getNoPermission() {
        return getColoredString("no-permission");
    }

    // Returns colored string for player-only command message
    public String getPlayerOnly() {
        return getColoredString("player-only");
    }

    // [Additional getter methods follow the same pattern]

    // Utility method to get colored string from config
    private String getColoredString(String key) {
        return ColorUtil.parseColor(messages.getString(key, ""));
    }

    // Saves current configuration to file
    public void saveConfig() {
        try {
            messages.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Reloads configuration from file
    public void reloadConfig() {
        messages = YamlConfiguration.loadConfiguration(file);
    }

    // Gets raw string from configuration
    public String getString(String path) {
        return messages.getString(path);
    }

    // Formats message with provided arguments using MessageFormatter
    public Component getFormatted(String path, Object... args) {
        return formatter.format(path, args);
    }
}

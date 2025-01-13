package com.nonxedy.nonchat.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.utils.BroadcastMessage;
import com.nonxedy.nonchat.utils.WordBlocker;

// Main configuration class for the NonChat plugin
public class PluginConfig {
    // File object representing the config.yml file
    private final File configFile;
    // Configuration object to store and manage plugin settings
    private FileConfiguration config;
    // Debug mode flag
    private boolean debug;

    // Constructor initializes config file path and loads configuration
    public PluginConfig() {
        // Sets config file path to plugins/nonchat/config.yml
        this.configFile = new File("plugins/nonchat", "config.yml");
        loadConfig();
    }

    // Loads configuration from file or creates default if doesn't exist
    private void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        // Load configuration from file
        this.config = YamlConfiguration.loadConfiguration(configFile);
        // Set debug mode from config
        this.debug = config.getBoolean("debug", false);
    }

    // Creates default configuration file with initial settings
    private void createDefaultConfig() {
        try {
            // Create directories if they don't exist
            configFile.getParentFile().mkdirs();
            // Create new config file
            configFile.createNewFile();

            config = new YamlConfiguration();
            // Set default configuration values
            setDefaultValues();
            saveConfig();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create default config", e);
        }
    }

    // Sets default values for all configuration options
    private void setDefaultValues() {
        // Chat formatting settings
        config.set("chat-format", "{prefix} §f{sender}§r {suffix}§7: §f{message}");
        config.set("death-format", "{prefix} §f{player}§r {suffix}§f died");
        config.set("private-chat-format", "§f{sender} §7-> §f{target}§7: §7{message}");
        config.set("sc-format", "{prefix} §f{sender}§r {suffix}§7: §7{message}");
        config.set("staff-chat-name", "[STAFFCHAT]");
        config.set("spy-format", "§f{sender} §7-> §f{target}§7: §7{message}");
        
        // Broadcast system settings
        config.set("broadcast.enabled", true);
        config.set("broadcast.message", "This message will be sent every 60 seconds");
        config.set("broadcast.interval", 60);
        config.set("broadcast.random", false);
        
        // Chat bubbles configuration
        config.set("chat-bubbles.enabled", true);
        config.set("chat-bubbles.duration", 5);
        config.set("chat-bubbles.height", 2.5);
        
        // Debug mode setting
        config.set("debug", false);
    }

    // Getter methods for various chat formats with default values
    @NotNull
    public String getChatFormat() {
        return config.getString("chat-format", "{prefix} §f{sender}§r {suffix}§7: §f{message}");
    }

    @NotNull
    public String getDeathFormat() {
        return config.getString("death-format", "{prefix} §f{player}§r {suffix}§f died");
    }

    @NotNull
    public String getPrivateChatFormat() {
        return config.getString("private-chat-format", "§f{sender} §7-> §f{target}§7: §7{message}");
    }

    @NotNull
    public String getScFormat() {
        return config.getString("sc-format", "{prefix} §f{sender}§r {suffix}§7: §7{message}");
    }

    @NotNull
    public String getStaffChatName() {
        return config.getString("staff-chat-name", "[STAFFCHAT]");
    }

    @NotNull
    public String getSpyFormat() {
        return config.getString("spy-format", "§f{sender} §7-> §f{target}§7: §7{message}");
    }

    // Methods for broadcast system configuration
    public boolean isBroadcastEnabled() {
        return config.getBoolean("broadcast.enabled", true);
    }

    public boolean isRandomBroadcastEnabled() {
        return config.getBoolean("broadcast.random", false);
    }

    @NotNull
    public String getBroadcastMessage() {
        return config.getString("broadcast.message", "Default broadcast message");
    }

    public int getBroadcastInterval() {
        return config.getInt("broadcast.interval", 60);
    }

    // Retrieves all broadcast messages from config
    @NotNull
    public Map<String, BroadcastMessage> getBroadcastMessages() {
        Map<String, BroadcastMessage> messages = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("broadcast");
        
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (key.equals("random")) continue;
                
                ConfigurationSection messageSection = section.getConfigurationSection(key);
                if (messageSection != null) {
                    messages.put(key, new BroadcastMessage(
                        messageSection.getBoolean("enabled", true),
                        messageSection.getString("message", "Default message"),
                        messageSection.getInt("interval", 60)
                    ));
                }
            }
        }
        return messages;
    }

    // Chat bubbles configuration getters
    public boolean isChatBubblesEnabled() {
        return config.getBoolean("chat-bubbles.enabled", true);
    }

    public int getChatBubblesDuration() {
        return config.getInt("chat-bubbles.duration", 5);
    }

    public double getChatBubblesHeight() {
        return config.getDouble("chat-bubbles.height", 2.5);
    }

    // Word blocking system methods
    @NotNull
    public List<String> getBannedWords() {
        return config.getStringList("banned-words");
    }

    @NotNull
    public WordBlocker getWordBlocker() {
        return new WordBlocker(getBannedWords());
    }

    // Debug mode management
    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        config.set("debug", debug);
        saveConfig();
    }

    // Configuration file management methods
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }

    public void reloadConfig() {
        loadConfig();
    }
}

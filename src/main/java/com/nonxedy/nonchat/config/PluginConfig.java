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
import com.nonxedy.nonchat.utils.CapsFilter;
import com.nonxedy.nonchat.utils.ChatTypeUtil;
import com.nonxedy.nonchat.utils.HoverTextUtil;
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
        // Check if config file exists
        if (!configFile.exists()) {
            // Create default configuration if file doesn't exist
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

            // Initialize new configuration object
            config = new YamlConfiguration();
            // Set default configuration values
            setDefaultValues();
            // Save the configuration to file
            saveConfig();
        } catch (IOException e) {
            // Throw runtime exception if config creation fails
            throw new RuntimeException("Failed to create default config", e);
        }
    }

    // Sets default values for all configuration options
    private void setDefaultValues() {
        // Chat formatting settings
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

        // Update checker settings
        config.set("update-checker", true);
    }

    // Create default chat channels configuration
    private void createDefaultChats() {
        // Configure global chat channel
        config.set("chats.global.enabled", true);
        config.set("chats.global.format", "§7(§6G§7)§r {prefix} §f{sender}§r {suffix}§7: §f{message}");
        config.set("chats.global.radius", -1);
        config.set("chats.global.char", "!");
    
        // Configure local chat channel
        config.set("chats.local.enabled", true);
        config.set("chats.local.format", "§7(§6L§7)§r {prefix} §f{sender}§r {suffix}§7: §f{message}");
        config.set("chats.local.radius", 100);
        config.set("chats.local.char", "");
        
        // Save the configuration
        saveConfig();
    }

    // Getter methods for various chat formats with default values
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

    // Update checker method
    public boolean isUpdateCheckerEnabled() {
        return config.getBoolean("update-checker", true);
    }

    // Gets a chat type based on its prefix character
    public ChatTypeUtil getChatTypeByChar(char prefix) {
        // Get all available chat types from configuration
        Map<String, ChatTypeUtil> chats = getChats();
        // Stream through all chat types and find the first one matching the prefix
        return chats.values().stream()
            // Filter to find chat with matching prefix character
            .filter(chat -> chat.getChatChar() == prefix)
            // Get the first matching chat type
            .findFirst()
            // If no chat found with this prefix, return default local chat
            .orElse(getDefaultChatType());
    }

    // Returns the default chat type configuration (local chat)
    public ChatTypeUtil getDefaultChatType() {
        // Get all available chats from config
        // Return local chat if exists, otherwise create new default local chat
        return getChats().getOrDefault("local", 
            // Create new ChatTypeUtil with default local chat settings:
            new ChatTypeUtil("local", true, 
                "§7(§6L§7)§r {prefix} §f{sender}§r {suffix}§7: §f{message}", 
                100, '\0'));
    }

    // Checks if specific chat type is enabled in config
    public boolean isChatEnabled(String chatName) {
        // Get enabled status from config, default to false if not found
        return config.getBoolean("chats." + chatName + ".enabled", false);
    }

    // Sets enabled status for specific chat type
    public void setChatEnabled(String chatName, boolean enabled) {
        // Update enabled status in config
        config.set("chats." + chatName + ".enabled", enabled);
        // Save changes to config file
        saveConfig();
    }

    // Retrieves all configured chat types from config.yml
    @NotNull
    public Map<String, ChatTypeUtil> getChats() {
        // Create new map to store chat configurations
        Map<String, ChatTypeUtil> chats = new HashMap<>();
        // Get the 'chats' section from config
        ConfigurationSection chatsSection = config.getConfigurationSection("chats");
        
        // If no chats section exists in config
        if (chatsSection == null) {
            // Create default chat configurations
            createDefaultChats();
            // Get the newly created chats section
            chatsSection = config.getConfigurationSection("chats");
        }

        // Iterate through all chat types in config
        for (String chatName : chatsSection.getKeys(false)) {
            // Get configuration section for specific chat
            ConfigurationSection chatSection = chatsSection.getConfigurationSection(chatName);
            // If chat section exists
            if (chatSection != null) {
                // Create new ChatTypeUtil object with configuration values
                ChatTypeUtil chatTypeUtil = new ChatTypeUtil(
                    // Set chat name from config key
                    chatName,
                    // Set enabled status with default true
                    chatSection.getBoolean("enabled", true),
                    // Set chat format with default format
                    chatSection.getString("format", "§7({prefix})§r {sender}§r {suffix}§7: §f{message}"),
                    // Set chat radius with default -1 (unlimited)
                    chatSection.getInt("radius", -1),
                    // Set chat prefix character, default to null character if empty
                    chatSection.getString("char", "").isEmpty() ? '\0' : chatSection.getString("char").charAt(0)
                );
                // Add chat type to map with chatName as key
                chats.put(chatName, chatTypeUtil);
            }
        }
        
        // Return complete map of chat types
        return chats;
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

    // Roleplay commands configuration
    public boolean isMeCommandEnabled() {
        return config.getBoolean("roleplay-commands.me.enabled", true);
    }

    public String getMeFormat() {
        return config.getString("roleplay-commands.me.format", "&f{player}&7: &f{message}");
    }

    public boolean isRollCommandEnabled() {
        return config.getBoolean("roleplay-commands.roll.enabled", true);
    }

    public String getRollFormat() {
        return config.getString("roleplay-commands.roll.format", "&7*{player} rolled a {number}");
    }

    // Caps filter configuration
    public boolean isCapsFilterEnabled() {
        return config.getBoolean("caps-filter.enabled", true);
    }

    public int getMaxCapsPercentage() {
        return config.getInt("caps-filter.max-caps-percentage", 70);
    }

    public int getMinCapsLength() {
        return config.getInt("caps-filter.min-length", 4);
    }

    public CapsFilter getCapsFilter() {
        return new CapsFilter(
            isCapsFilterEnabled(),
            getMaxCapsPercentage(),
            getMinCapsLength()
        );
    }

    // Hover text configuration
    public boolean isHoverEnabled() {
        return config.getBoolean("hover-text.enabled", true);
    }

    public List<String> getHoverFormat() {
        return config.getStringList("hover-text.format");
    }

    public HoverTextUtil getHoverTextUtil() {
        return new HoverTextUtil(getHoverFormat(), isHoverEnabled());
    }

    // Configuration file management methods
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }

    // Load configuration from file
    public void reloadConfig() {
        loadConfig();
    }
}

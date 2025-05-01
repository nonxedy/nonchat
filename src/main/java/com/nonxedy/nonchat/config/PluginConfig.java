package com.nonxedy.nonchat.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.util.BroadcastMessage;
import com.nonxedy.nonchat.util.CapsFilter;
import com.nonxedy.nonchat.util.ChatTypeUtil;
import com.nonxedy.nonchat.util.HoverTextUtil;
import com.nonxedy.nonchat.util.WordBlocker;

/**
 * Central configuration manager for the NonChat plugin
 * Handles loading, saving and accessing all plugin settings
 */
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

    /**
     * Loads configuration from file or creates default
     */
    public void loadConfig() {
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

    /**
     * Creates default configuration file with initial settings
     * @throws RuntimeException if file creation fails
     */
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

    /**
     * Sets all default configuration values
     */
    private void setDefaultValues() {
        // Death settings
        config.set("death.enabled", true);
        config.set("death.format", "{prefix} §f{player}§r {suffix}§f died");
        config.set("death.show-coordinates", true);
        config.set("join-messages.enabled", true);
        config.set("join-messages.format", "§8(§a+§8) {prefix} §f{player}§r {suffix}");
        config.set("quit-messages.enabled", true);
        config.set("quit-messages.format", "§8(§c-§8) {prefix} §f{player}§r {suffix}");
        config.set("private-chat-format", "§f{sender} §7-> §f{target}§7: §7{message}");
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

    // Creates default chat channels configuration
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

    /**
     * Checks if custom death messages are enabled
     * @return true if enabled
     */
    public boolean isDeathMessagesEnabled() {
        return config.getBoolean("death.enabled", true);
    }
    
    /**
     * Checks if showing death coordinates is enabled
     * @return true if enabled
     */
    public boolean isShowDeathCoordinatesEnabled() {
        return config.getBoolean("death.show-coordinates", true);
    }
    
    /**
     * Gets death message format
     * @return Formatted death message string
     */
    @NotNull
    public String getDeathFormat() {
        return config.getString("death.format", "{prefix} §f{player}§r {suffix}§f died");
    }
    
    /**
     * Checks if join messages are enabled
     * @return true if enabled
     */
    public boolean isJoinMessageEnabled() {
        return config.getBoolean("join-messages.enabled", true);
    }
    
    /**
     * Gets join message format
     * @return Formatted join message string
     */
    @NotNull
    public String getJoinFormat() {
        return config.getString("join-messages.format", "§8(§a+§8) {prefix} §f{player}§r {suffix}");
    }
    
    /**
     * Checks if quit messages are enabled
     * @return true if enabled
     */
    public boolean isQuitMessageEnabled() {
        return config.getBoolean("quit-messages.enabled", true);
    }
    
    /**
     * Gets quit message format
     * @return Formatted quit message string
     */
    @NotNull
    public String getQuitFormat() {
        return config.getString("quit-messages.format", "§8(§c-§8) {prefix} §f{player}§r {suffix}");
    }

    /**
     * Gets private chat message format
     * @return Private chat format string
     */
    @NotNull
    public String getPrivateChatFormat() {
        return config.getString("private-chat-format", "§f{sender} §7-> §f{target}§7: §7{message}");
    }

    /**
     * Gets spy message format
     * @return Spy message format string
     */
    @NotNull
    public String getSpyFormat() {
        return config.getString("spy-format", "§f{sender} §7-> §f{target}§7: §7{message}");
    }

    /**
     * Checks if broadcast system is enabled
     * @return true if enabled
     */
    public boolean isBroadcastEnabled() {
        return config.getBoolean("broadcast.enabled", true);
    }

    /**
     * Checks if random broadcast is enabled
     * @return true if random mode enabled
     */
    public boolean isRandomBroadcastEnabled() {
        return config.getBoolean("broadcast.random", false);
    }

    /**
     * Gets default broadcast message
     * @return Default message string
     */
    @NotNull
    public String getBroadcastMessage() {
        return config.getString("broadcast.message", "Default broadcast message");
    }

    /**
     * Gets broadcast interval in seconds
     * @return Interval between broadcasts
     */
    public int getBroadcastInterval() {
        return config.getInt("broadcast.interval", 60);
    }

    /**
     * Gets all configured broadcast messages
     * @return Map of broadcast messages
     */
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

    /**
     * Checks if chat bubbles are enabled
     * @return true if enabled
     */
    public boolean isChatBubblesEnabled() {
        return config.getBoolean("chat-bubbles.enabled", true);
    }

    /**
     * Gets chat bubble duration in seconds
     * @return Duration time
     */
    public int getChatBubblesDuration() {
        return config.getInt("chat-bubbles.duration", 5);
    }

    /**
     * Gets chat bubble height above player
     * @return Height in blocks
     */
    public double getChatBubblesHeight() {
        return config.getDouble("chat-bubbles.height", 2.5);
    }

    /**
     * Gets list of banned words
     * @return List of blocked words
     */
    @NotNull
    public List<String> getBannedWords() {
        return config.getStringList("banned-words");
    }

    /**
     * Gets word blocker instance
     * @return Configured WordBlocker
     */
    @NotNull
    public WordBlocker getWordBlocker() {
        return new WordBlocker(getBannedWords());
    }

    /**
     * Checks if update checker is enabled
     * @return true if enabled
     */
    public boolean isUpdateCheckerEnabled() {
        return config.getBoolean("update-checker", true);
    }

    /**
     * Gets chat type by prefix character
     * @param prefix Character to check
     * @return Matching ChatTypeUtil or default
     */
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

    /**
     * Gets default chat type configuration
     * @return Default ChatTypeUtil
     */
    public ChatTypeUtil getDefaultChatType() {
        // Get all available chats from config
        // Return local chat if exists, otherwise create new default local chat
        return getChats().getOrDefault("local", 
            // Create new ChatTypeUtil with default local chat settings:
            new ChatTypeUtil(true, 
                "§7(§6L§7)§r {prefix} §f{sender}§r {suffix}§7: §f{message}", 
                100, '\0', null));
    }

    /**
     * Checks if specific chat type is enabled
     * @param chatName Chat type to check
     * @return true if enabled
     */
    public boolean isChatEnabled(String chatName) {
        // Get enabled status from config, default to false if not found
        return config.getBoolean("chats." + chatName + ".enabled", false);
    }

    /**
     * Sets enabled state for chat type
     * @param chatName Chat type to modify
     * @param enabled New enabled state
     */
    public void setChatEnabled(String chatName, boolean enabled) {
        // Update enabled status in config
        config.set("chats." + chatName + ".enabled", enabled);
        // Save changes to config file
        saveConfig();
    }

    public Map<String, ChatTypeUtil> getChats() {
        Map<String, ChatTypeUtil> chats = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("chats");
        
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection chatSection = section.getConfigurationSection(key);
                if (chatSection != null) {
                    boolean enabled = chatSection.getBoolean("enabled", true);
                    String format = chatSection.getString("format", "{prefix} {sender} {suffix}: {message}");
                    int radius = chatSection.getInt("radius", -1);
                    String charStr = chatSection.getString("char", "");
                    char chatChar = charStr.isEmpty() ? '\0' : charStr.charAt(0);
                    
                    // Permission is optional - if not specified, it will be null
                    String permission = null;
                    if (chatSection.contains("permission")) {
                        permission = chatSection.getString("permission");
                    }
                    
                    chats.put(key, new ChatTypeUtil(enabled, format, radius, chatChar, permission));
                }
            }
        }
        return chats;
    }

    /**
     * Gets debug mode state
     * @return true if debug enabled
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Sets debug mode state
     * @param debug New debug state
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
        config.set("debug", debug);
        saveConfig();
    }

    /**
     * Checks if /me command is enabled
     * @return true if enabled
     */
    public boolean isMeCommandEnabled() {
        return config.getBoolean("roleplay-commands.me.enabled", true);
    }

    /**
     * Gets /me command format
     * @return Format string
     */
    public String getMeFormat() {
        return config.getString("roleplay-commands.me.format", "&f{player}&7: &f{message}");
    }

    /**
     * Checks if /roll command is enabled
     * @return true if enabled
     */
    public boolean isRollCommandEnabled() {
        return config.getBoolean("roleplay-commands.roll.enabled", true);
    }

    /**
     * Gets /roll command format
     * @return Format string
     */
    public String getRollFormat() {
        return config.getString("roleplay-commands.roll.format", "&7*{player} rolled a {number}");
    }

    /**
     * Checks if caps filter is enabled
     * @return true if enabled
     */
    public boolean isCapsFilterEnabled() {
        return config.getBoolean("caps-filter.enabled", true);
    }

    /**
     * Gets maximum allowed caps percentage
     * @return Maximum percentage
     */
    public int getMaxCapsPercentage() {
        return config.getInt("caps-filter.max-caps-percentage", 70);
    }

    /**
     * Gets minimum length for caps checking
     * @return Minimum length
     */
    public int getMinCapsLength() {
        return config.getInt("caps-filter.min-length", 4);
    }

    /**
     * Gets caps filter instance
     * @return Configured CapsFilter
     */
    public CapsFilter getCapsFilter() {
        return new CapsFilter(
            isCapsFilterEnabled(),
            getMaxCapsPercentage(),
            getMinCapsLength()
        );
    }

    /**
     * Checks if hover text is enabled
     * @return true if enabled
     */
    public boolean isHoverEnabled() {
        return config.getBoolean("hover-text.enabled", true);
    }

    /**
     * Gets hover text format
     * @return List of format lines
     */
    public List<String> getHoverFormat() {
        return config.getStringList("hover-text.format");
    }

    /**
     * Gets hover text utility instance
     * @return Configured HoverTextUtil
     */
    public HoverTextUtil getHoverTextUtil() {
        return new HoverTextUtil(getHoverFormat(), isHoverEnabled());
    }

    /**
     * Saves configuration to file
     * @throws RuntimeException if save fails
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }

    // Reloads configuration from file
    public void reloadConfig() {
        loadConfig();
    }

    public String getString(String path) {
        return config.getString(path);
    }

    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    public int getInt(String path) {
        return config.getInt(path);
    }

    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    public void set(String path, Object value) {
        config.set(path, value);
    }

    public boolean contains(String path) {
        return config.contains(path);
    }

    public ConfigurationSection getConfigurationSection(String path) {
        return config.getConfigurationSection(path);
    }

    public Set<String> getKeys(boolean deep) {
        return config.getKeys(deep);
    }
}

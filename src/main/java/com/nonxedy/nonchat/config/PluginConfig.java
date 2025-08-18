package com.nonxedy.nonchat.config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.util.chat.filters.CapsFilter;
import com.nonxedy.nonchat.util.chat.filters.WordBlocker;
import com.nonxedy.nonchat.util.chat.formatting.ChatTypeUtil;
import com.nonxedy.nonchat.util.chat.formatting.HoverTextUtil;
import com.nonxedy.nonchat.util.core.broadcast.BroadcastMessage;

import lombok.Getter;

/**
 * Central configuration manager for the NonChat plugin
 * Handles loading, saving and accessing all plugin settings
 */
@Getter
public class PluginConfig {
    // File object representing the config.yml file
    private final File configFile;
    // Configuration object to store and manage plugin settings
    private FileConfiguration config;
    // Debug mode flag
    private boolean debug;
    // Language setting
    private String language;
    // Default channel
    private String defaultChannel;

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
        // Set language from config
        this.language = config.getString("language", "en");
        // Set default channel
        this.defaultChannel = config.getString("default-channel", "local");
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
        // Language setting
        config.set("language", "en");
        
        // Debug mode setting
        config.set("debug", false);

        // Update checker settings
        config.set("update-checker", true);
        
        // Default channel setting
        config.set("default-channel", "local");
        
        // Message delivery notifications
        config.set("message-delivery.notify-undelivered", true);
        
        // Interactive placeholders configuration
        config.set("interactive-placeholders.enabled", true);
        config.set("interactive-placeholders.item-enabled", true);
        config.set("interactive-placeholders.ping-enabled", true);
        
        // Death settings
        config.set("death.enabled", true);
        config.set("death.format", "%luckperms_prefix% §f%player_name%§r %luckperms_suffix%§f died");
        config.set("death.show-coordinates", true);
        
        // Join/Quit messages settings
        config.set("join-messages.enabled", true);
        config.set("join-messages.format", "§8(§a+§8) %luckperms_prefix% §f%player_name%§r %luckperms_suffix%");
        config.set("quit-messages.enabled", true);
        config.set("quit-messages.format", "§8(§c-§8) %luckperms_prefix% §f%player_name%§r %luckperms_suffix%");
        
        // Private chat settings (оставляем как есть)
        config.set("private-chat-format", "§f{sender} §7-> §f{target}§7: §7{message}");
        config.set("private-chat-target-you", "You");
        config.set("spy-format", "§f{sender} §7-> §f{target}§7: §7{message}");
        
        // Chat bubbles configuration
        config.set("chat-bubbles.enabled", true);
        config.set("chat-bubbles.duration", 5);
        config.set("chat-bubbles.height", 2.5);
        config.set("chat-bubbles.show-in-private-channels", false);
        
        // Create default channel configurations
        createDefaultChannels();
        
        // Roleplay commands
        config.set("roleplay-commands.me.enabled", true);
        config.set("roleplay-commands.me.format", "&7*%player_name%: {message}");
        config.set("roleplay-commands.roll.enabled", true);
        config.set("roleplay-commands.roll.format", "&7*%player_name% rolled a {number}");
        
        // Hover text
        config.set("hover-text.enabled", true);
        List<String> defaultHoverFormat = Arrays.asList(
            "&#FFAFFB⭐ %player_name%", 
            "&#FFAFFB► Rank: &#FFFFFF%luckperms_prefix%",
            "&#FFAFFB► Balance: &#FFFFFF$%vault_eco_balance%",
            "&#FFAFFB► Level: &#FFFFFF%player_level%",
            "&#FFAFFB► Playtime: &#FFFFFF%statistic_time_played%",
            "&#FFAFFB► Location: &#FFFFFF%player_world%",
            "&#FFAFFB► Ping: &#FFFFFF%player_ping%ms",
            "§7",
            "§8Click to send a private message"
        );
        config.set("hover-text.format", defaultHoverFormat);
        
        // Banned words
        config.set("banned-words", Arrays.asList("spam", "badword", "anotherbadword", "плохой"));
        
        // Caps filter
        config.set("caps-filter.enabled", true);
        config.set("caps-filter.max-caps-percentage", 70);
        config.set("caps-filter.min-length", 4);
        
        // Broadcast settings
        config.set("broadcast.format", "\n§#FFAFFBBroadcast: §f{message}\n");
        config.set("broadcast.random", true);
        config.set("broadcast.example.enabled", true);
        config.set("broadcast.example.message", "This message will be sent every 60 seconds");
        config.set("broadcast.example.interval", 60);
        
        // Anti-advertisement settings
        config.set("anti-ad.enabled", true);
        config.set("anti-ad.sensitivity", 0.7);
        config.set("anti-ad.whitelisted-urls", Arrays.asList(
            "discord.gg/NAWsxe3J3R"
        ));
        config.set("anti-ad.staff-notify", true);
        config.set("anti-ad.punish-command", "ban %player% advertising");
    }

    // Creates default chat channels configuration
    private void createDefaultChannels() {
        // Configure global chat channel
        config.set("channels.global.enabled", true);
        config.set("channels.global.display-name", "Global");
        config.set("channels.global.format", "§7(§6G§7)§r %luckperms_prefix% §f%player_name%§r %luckperms_suffix%§7: §f{message}");
        config.set("channels.global.radius", -1);
        config.set("channels.global.character", "!");
        config.set("channels.global.send-permission", "");
        config.set("channels.global.receive-permission", "");
        config.set("channels.global.cooldown", 0);
        config.set("channels.global.min-length", 0);
        config.set("channels.global.max-length", -1);

        // Configure local chat channel
        config.set("channels.local.enabled", true);
        config.set("channels.local.display-name", "Local");
        config.set("channels.local.format", "§7(§6L§7)§r %luckperms_prefix% §f%player_name%§r %luckperms_suffix%§7: §f{message}");
        config.set("channels.local.radius", 100);
        config.set("channels.local.character", "");
        config.set("channels.local.send-permission", "");
        config.set("channels.local.receive-permission", "");
        config.set("channels.local.cooldown", 0);
        config.set("channels.local.min-length", 0);
        config.set("channels.local.max-length", -1);
        
        // Configure staff chat channel
        config.set("channels.staff.enabled", true);
        config.set("channels.staff.display-name", "Staff");
        config.set("channels.staff.format", "§7(§bSC§7)§r %luckperms_prefix% §f%player_name%§r %luckperms_suffix%§7: §f{message}");
        config.set("channels.staff.radius", -1);
        config.set("channels.staff.character", "*");
        config.set("channels.staff.send-permission", "nonchat.chat.staff");
        config.set("channels.staff.receive-permission", "nonchat.chat.staff");
        config.set("channels.staff.cooldown", 0);
        config.set("channels.staff.min-length", 0);
        config.set("channels.staff.max-length", -1);
    }

    /**
     * Gets configured language
     * @return Language code (en, ru)
     */
    @NotNull
    public String getLanguage() {
        return language;
    }
    
    /**
     * Gets the default channel ID
     * @return Default channel ID
     */
    @NotNull
    public String getDefaultChannel() {
        return defaultChannel;
    }

    /**
     * Checks if undelivered message notifications are enabled
     * @return true if notifications are enabled
     */
    public boolean isUndeliveredMessageNotificationEnabled() {
        return config.getBoolean("message-delivery.notify-undelivered", true);
    }

    /**
     * Sets undelivered message notification enabled state
     * @param enabled New enabled state
     */
    public void setUndeliveredMessageNotificationEnabled(boolean enabled) {
        config.set("message-delivery.notify-undelivered", enabled);
        saveConfig();
    }

    /**
     * Checks if interactive placeholders are enabled
     * @return true if enabled
     */
    public boolean isInteractivePlaceholdersEnabled() {
        return config.getBoolean("interactive-placeholders.enabled", true);
    }

    /**
     * Checks if item placeholders are enabled
     * @return true if enabled
     */
    public boolean isItemPlaceholdersEnabled() {
        return config.getBoolean("interactive-placeholders.item-enabled", true);
    }

    /**
     * Checks if ping placeholders are enabled
     * @return true if enabled
     */
    public boolean isPingPlaceholdersEnabled() {
        return config.getBoolean("interactive-placeholders.ping-enabled", true);
    }

    /**
     * Sets interactive placeholders enabled state
     * @param enabled New enabled state
     */
    public void setInteractivePlaceholdersEnabled(boolean enabled) {
        config.set("interactive-placeholders.enabled", enabled);
        saveConfig();
    }

    /**
     * Sets item placeholders enabled state
     * @param enabled New enabled state
     */
    public void setItemPlaceholdersEnabled(boolean enabled) {
        config.set("interactive-placeholders.item-enabled", enabled);
        saveConfig();
    }

    /**
     * Sets ping placeholders enabled state
     * @param enabled New enabled state
     */
    public void setPingPlaceholdersEnabled(boolean enabled) {
        config.set("interactive-placeholders.ping-enabled", enabled);
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
        return config.getString("death.format", "%luckperms_prefix% §f%player_name%§r %luckperms_suffix%§f died");
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
        return config.getString("join-messages.format", "§8(§a+§8) %luckperms_prefix% §f%player_name%§r %luckperms_suffix%");
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
        return config.getString("quit-messages.format", "§8(§c-§8) %luckperms_prefix% §f%player_name%§r %luckperms_suffix%");
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
     * Gets private chat target "You" text
     * @return Text to show for target player in private messages
     */
    @NotNull
    public String getPrivateChatTargetYou() {
        return config.getString("private-chat-target-you", "You");
    }

    /**
     * Checks if broadcast system is enabled
     * @return true if enabled
     */
    public boolean isBroadcastEnabled() {
        return config.getBoolean("broadcast.enabled", true);
    }

    /**
     * Gets broadcast command format
     * @return Broadcast command format string
     */
    @NotNull
    public String getBroadcastFormat() {
        return config.getString("broadcast.format", "\n§#FFAFFBBroadcast: §f{message}\n");
    }

    /**
     * Sets broadcast command format
     * @param format New format string
     */
    public void setBroadcastFormat(String format) {
        config.set("broadcast.format", format);
        saveConfig();
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
     * Checks if chat bubbles should be shown in private channels
     * @return true if bubbles should show in private channels
     */
    public boolean shouldShowBubblesInPrivateChannels() {
        return config.getBoolean("chat-bubbles.show-in-private-channels", false);
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
     * @param channelId Channel ID to check
     * @return true if enabled
     */
    public boolean isChatEnabled(String channelId) {
        // Get enabled status from config, default to false if not found
        return config.getBoolean("channels." + channelId + ".enabled", false);
    }

    /**
     * Sets enabled state for chat type
     * @param channelId Channel ID to modify
     * @param enabled New enabled state
     */
    public void setChatEnabled(String channelId, boolean enabled) {
        // Update enabled status in config
        config.set("channels." + channelId + ".enabled", enabled);
        // Save changes to config file
        saveConfig();
    }

    /**
     * Gets all configured chat channels
     * @return Map of channel IDs to ChatTypeUtil objects
     */
    public Map<String, ChatTypeUtil> getChats() {
        Map<String, ChatTypeUtil> channels = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("channels");
        
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection channelSection = section.getConfigurationSection(key);
                if (channelSection != null) {
                    boolean enabled = channelSection.getBoolean("enabled", true);
                    String displayName = channelSection.getString("display-name", key);
                    String format = channelSection.getString("format", "{prefix} {sender} {suffix}: {message}");
                    int radius = channelSection.getInt("radius", -1);
                    String charStr = channelSection.getString("character", "");
                    char chatChar = charStr.isEmpty() ? '\0' : charStr.charAt(0);
                    
                    // Get send permission
                    String sendPermission = channelSection.getString("send-permission", "");
                    
                    // For backward compatibility, check old permission field too
                    if (sendPermission.isEmpty() && channelSection.contains("permission")) {
                        sendPermission = channelSection.getString("permission");
                    }
                    
                    // Get additional properties
                    String receivePermission = channelSection.getString("receive-permission", "");
                    int cooldown = channelSection.getInt("cooldown", 0);
                    int minLength = channelSection.getInt("min-length", 0);
                    int maxLength = channelSection.getInt("max-length", -1);
                    
                    channels.put(key, new ChatTypeUtil(
                        enabled, 
                        displayName,
                        format, 
                        radius, 
                        chatChar, 
                        sendPermission,
                        receivePermission,
                        cooldown,
                        minLength,
                        maxLength
                    ));
                }
            }
        }
        
        // For backward compatibility, check old "chats" section too
        ConfigurationSection oldSection = config.getConfigurationSection("chats");
        if (oldSection != null && channels.isEmpty()) {
            for (String key : oldSection.getKeys(false)) {
                ConfigurationSection chatSection = oldSection.getConfigurationSection(key);
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
                    
                    channels.put(key, new ChatTypeUtil(enabled, format, radius, chatChar, permission));
                }
            }
        }
        
        return channels;
    }

    /**
     * Gets debug mode state
     * @return true if debug enabled
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Gets debug log retention days from config
     * @return Number of days to keep debug logs (default 7)
     */
    public int getDebugLogRetentionDays() {
        return config.getInt("debug-log-retention-days", 7);
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
        return config.getString("roleplay-commands.me.format", "&7*%player_name%: {message}");
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
        return config.getString("roleplay-commands.roll.format", "&7*%player_name% rolled a {number}");
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
     * Checks if anti-advertisement system is enabled
     * @return true if enabled
     */
    public boolean isAntiAdEnabled() {
        return config.getBoolean("anti-ad.enabled", true);
    }

    /**
     * Gets anti-advertisement sensitivity level
     * @return Sensitivity between 0.0 and 1.0
     */
    public float getAntiAdSensitivity() {
        return (float) config.getDouble("anti-ad.sensitivity", 0.7);
    }

    /**
     * Gets list of whitelisted URLs
     * @return List of allowed domains/IPs
     */
    public List<String> getAntiAdWhitelistedUrls() {
        return config.getStringList("anti-ad.whitelisted-urls");
    }

    /**
     * Checks if staff should be notified about ads
     * @return true if notifications enabled
     */
    public boolean shouldNotifyStaffAboutAds() {
        return config.getBoolean("anti-ad.staff-notify", true);
    }

    /**
     * Gets the punishment command to execute when advertisement is detected
     * @return Punishment command with %player% placeholder or empty string if no punishment 
     */
    public String getAntiAdPunishCommand() {
        return config.getString("anti-ad.punish-command", "ban %player_name% advertising");
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

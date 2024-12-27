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

public class PluginConfig {
    private final File configFile;
    private FileConfiguration config;
    private boolean debug;

    public PluginConfig() {
        this.configFile = new File("plugins/nonchat", "config.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.debug = config.getBoolean("debug", false);
    }

    private void createDefaultConfig() {
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            config = new YamlConfiguration();
            setDefaultValues();
            saveConfig();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create default config", e);
        }
    }

    private void setDefaultValues() {
        config.set("chat-format", "{prefix} §f{sender}§r {suffix}§7: §f{message}");
        config.set("death-format", "{prefix} §f{player}§r {suffix}§f died");
        config.set("private-chat-format", "§f{sender} §7-> §f{target}§7: §7{message}");
        config.set("sc-format", "{prefix} §f{sender}§r {suffix}§7: §7{message}");
        config.set("staff-chat-name", "[STAFFCHAT]");
        config.set("spy-format", "§f{sender} §7-> §f{target}§7: §7{message}");
        
        // Broadcast settings
        config.set("broadcast.enabled", true);
        config.set("broadcast.message", "This message will be sent every 60 seconds");
        config.set("broadcast.interval", 60);
        config.set("broadcast.random", false);
        
        // Chat bubbles settings
        config.set("chat-bubbles.enabled", true);
        config.set("chat-bubbles.duration", 5);
        config.set("chat-bubbles.height", 2.5);
        
        // Debug mode
        config.set("debug", false);
    }

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

    // Broadcast related methods
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

    // Chat bubbles related methods
    public boolean isChatBubblesEnabled() {
        return config.getBoolean("chat-bubbles.enabled", true);
    }

    public int getChatBubblesDuration() {
        return config.getInt("chat-bubbles.duration", 5);
    }

    public double getChatBubblesHeight() {
        return config.getDouble("chat-bubbles.height", 2.5);
    }

    // Word blocker
    @NotNull
    public List<String> getBannedWords() {
        return config.getStringList("banned-words");
    }

    @NotNull
    public WordBlocker getWordBlocker() {
        return new WordBlocker(getBannedWords());
    }

    // Debug related methods
    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        config.set("debug", debug);
        saveConfig();
    }

    // Config management
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
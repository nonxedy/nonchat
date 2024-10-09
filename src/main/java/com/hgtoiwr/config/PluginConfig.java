package com.hgtoiwr.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.hgtoiwr.utils.BroadcastMessage;

public class PluginConfig {

    private File file;
    private FileConfiguration config;
    private BroadcastMessage broadcastMessage;

    public PluginConfig(BroadcastMessage broadcastMessage) {
        file = new File("plugins/nonchat", "config.yml");
        if (!file.exists()) {
            createDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(file);
        this.broadcastMessage = broadcastMessage;
    }
    
    private void createDefaultConfig() {
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
    
            config = new YamlConfiguration();
            config.set("chat-format", "{prefix} §f{sender}§r {suffix}§7: §f{message}");
            config.set("death-format", "{prefix} §f{player}§r {suffix}§f died");
            config.set("private-chat-format", "§f{sender} §7-> §f{target}§7: §7{message}");
            config.set("broadcast.enabled", true);
            config.set("broadcast.message", "This message will be sent every 60 seconds");
            config.set("broadcast.interval", 60);
            
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String getChatFormat() {
        return config.getString("chat-format");
    }

    public String getDeathFormat() {
        return config.getString("death-format");
    }
    
    public boolean isBroadcastEnabled() {
        return config.getBoolean("broadcast.enabled");
    }

    public String getBroadcastMessage() {
        return config.getString("broadcast.message");
    }

    public int getBroadcastInterval() {
        return config.getInt("broadcast.interval");
    }

    public String getPrivateChatFormat() {
        return config.getString("private-chat-format");
    }
    
    public void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isRandomBroadcastEnabled() {
        return config.getBoolean("broadcast.random");
    }

    public Map<String, BroadcastMessage> getBroadcastMessages() {
        Map<String, BroadcastMessage> messages = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("broadcast");
        for (String key : section.getKeys(false)) {
            if (key.equals("random")) {
                continue;
            }
            ConfigurationSection messageSection = section.getConfigurationSection(key);
            messages.put(key, new BroadcastMessage(
                    messageSection.getBoolean("enabled"),
                    messageSection.getString("message"),
                    messageSection.getInt("interval")
            ));
        }
        return messages;
    }
}

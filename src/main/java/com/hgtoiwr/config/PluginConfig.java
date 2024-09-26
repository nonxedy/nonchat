package com.hgtoiwr.config;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class PluginConfig {

    private File file;
    private FileConfiguration config;

    public PluginConfig() {
        file = new File("plugins/nonchat", "config.yml");
        if (!file.exists()) {
            createDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(file);
    }
    
    private void createDefaultConfig() {
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
    
            config = new YamlConfiguration();
            config.set("chat-format", "{prefix} §f{sender}§r {suffix}§7: §f{message}");
            config.set("death-format", "{prefix} §f{player}§r {suffix}§f died");
            
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
}
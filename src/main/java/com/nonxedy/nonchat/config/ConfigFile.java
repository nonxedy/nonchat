package com.nonxedy.nonchat.config;

import com.nonxedy.nonchat.nonchat;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigFile {
    private final nonchat plugin;
    private final String fileName;
    private FileConfiguration fileConfiguration = null;
    private File file = null;
    private final String folderName;

    public ConfigFile(String fileName, String folderName, nonchat plugin) {
        this.fileName = fileName;
        this.folderName = folderName;
        this.plugin = plugin;
    }

    public String getPath() {
        return this.fileName;
    }

    public void registerConfig() {
        if (folderName != null) {
            file = new File(plugin.getDataFolder() + File.separator + folderName, fileName);
            // Create folder if it doesn't exist
            File folder = new File(plugin.getDataFolder() + File.separator + folderName);
            if (!folder.exists()) {
                folder.mkdirs();
            }
        } else {
            file = new File(plugin.getDataFolder(), fileName);
        }

        if (!file.exists()) {
            try {
                if (folderName != null) {
                    // Try to save resource from jar if available
                    if (plugin.getResource(folderName + File.separator + fileName) != null) {
                        plugin.saveResource(folderName + File.separator + fileName, false);
                    } else {
                        // Create empty file if resource doesn't exist
                        file.createNewFile();
                        // Initialize with default content
                        initDefaultContent();
                    }
                } else {
                    if (plugin.getResource(fileName) != null) {
                        plugin.saveResource(fileName, false);
                    } else {
                        file.createNewFile();
                        initDefaultContent();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        fileConfiguration = new YamlConfiguration();
        try {
            fileConfiguration.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void initDefaultContent() {
        if (fileName.equals("discord.yml")) {
            fileConfiguration = new YamlConfiguration();
            fileConfiguration.set("discord.webhook", "");
            fileConfiguration.set("discord.use-discordsrv", true);
            
            // Join settings
            fileConfiguration.set("discord.join.enabled", false);
            fileConfiguration.set("discord.join.title", "Player Join");
            fileConfiguration.set("discord.join.description", "%player% has joined the server");
            fileConfiguration.set("discord.join.color", 5763719); // Green
            fileConfiguration.set("discord.join.avatar-enabled", true);
            
            // Quit settings
            fileConfiguration.set("discord.quit.enabled", false);
            fileConfiguration.set("discord.quit.title", "Player Left");
            fileConfiguration.set("discord.quit.description", "%player% has left the server");
            fileConfiguration.set("discord.quit.color", 16724530); // Red
            fileConfiguration.set("discord.quit.avatar-enabled", true);
            
            // Death settings
            fileConfiguration.set("discord.death.enabled", false);
            fileConfiguration.set("discord.death.title", "Player Death");
            fileConfiguration.set("discord.death.description", "%player% has died");
            fileConfiguration.set("discord.death.color", 16763904); // Yellow
            fileConfiguration.set("discord.death.avatar-enabled", true);
            
            // Banned words settings
            fileConfiguration.set("discord.banned-words.title", "Banned Word Detected!");
            fileConfiguration.set("discord.banned-words.description", "Player %player% used a banned word: %word%\nMessage: \"%message%\"");
            fileConfiguration.set("discord.banned-words.color", 15158332); // Purple
            fileConfiguration.set("discord.banned-words.avatar-enabled", true);
            
            // Mute settings
            fileConfiguration.set("discord.mute.enabled", false);
            fileConfiguration.set("discord.mute.webhook", "");
            fileConfiguration.set("discord.mute.title", "Player Muted!");
            fileConfiguration.set("discord.mute.description", "Player %player% has been muted by %admin%");
            fileConfiguration.set("discord.mute.color", 15158332); // Purple
            fileConfiguration.set("discord.mute.avatar-enabled", true);
            
            // Banned commands settings
            fileConfiguration.set("discord.banned-commands.title", "Banned Command Detected!");
            fileConfiguration.set("discord.banned-commands.description", "Player %player% used a banned command: %word%\nMessage: \"%message%\"");
            fileConfiguration.set("discord.banned-commands.color", 15158332); // Purple
            fileConfiguration.set("discord.banned-commands.avatar-enabled", true);
            
            try {
                fileConfiguration.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveConfig() {
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        if (fileConfiguration == null) {
            reloadConfig();
        }
        return fileConfiguration;
    }

    public void reloadConfig() {
        if (fileConfiguration == null) {
            if (folderName != null) {
                file = new File(plugin.getDataFolder() + File.separator + folderName, fileName);
            } else {
                file = new File(plugin.getDataFolder(), fileName);
            }
        }
        fileConfiguration = YamlConfiguration.loadConfiguration(file);

        if (file != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(file);
            fileConfiguration.setDefaults(defConfig);
        }
    }
}

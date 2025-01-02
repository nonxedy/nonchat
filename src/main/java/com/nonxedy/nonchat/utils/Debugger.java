package com.nonxedy.nonchat.utils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class Debugger {
    private final Plugin plugin;
    private final File logFile;
    private FileConfiguration debugConfig;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Debugger(Plugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "debug.yml");
        initializeDebugFile();
    }

    private void initializeDebugFile() {
        if (!logFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                logFile.createNewFile();
                debugConfig = YamlConfiguration.loadConfiguration(logFile);
                debugConfig.set("created", LocalDateTime.now().format(TIME_FORMAT));
                debugConfig.save(logFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create debug file", e);
            }
        }
        debugConfig = YamlConfiguration.loadConfiguration(logFile);
    }

    public void log(String message) {
        try {
            String timestamp = LocalDateTime.now().format(TIME_FORMAT);
            String currentLog = debugConfig.getString("log", "");
            String newLog = String.format("%s[%s] %s", 
                currentLog.isEmpty() ? "" : currentLog + "\n", 
                timestamp, 
                message);
            
            debugConfig.set("log", newLog);
            debugConfig.save(logFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to write to debug file", e);
        }
    }

    public void clear() {
        try {
            debugConfig.set("log", "");
            debugConfig.set("last_cleared", LocalDateTime.now().format(TIME_FORMAT));
            debugConfig.save(logFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to clear debug file", e);
        }
    }
}

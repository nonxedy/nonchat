package com.nonxedy.nonchat.util;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nonxedy.nonchat.nonchat;

/**
 * Handles debug logging in YAML format with timestamps
 * Provides functionality to log, clear, and manage debug entries
 */
public class Debugger {
    private final nonchat plugin;
    private final File logFile;
    private FileConfiguration debugConfig;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructor initializes debugger with plugin reference
    public Debugger(nonchat plugin) {
        this.plugin = plugin;
        // Set debug file path in plugin's data folder
        this.logFile = new File(plugin.getDataFolder(), "debug.yml");
        // Create and initialize debug file
        initializeDebugFile();
    }

    /**
     * Creates and initializes the debug file if it doesn't exist
     * Sets up initial configuration and timestamp
     */
    private void initializeDebugFile() {
        // Check if debug file exists
        if (!logFile.exists()) {
            // Create plugin directory if needed
            plugin.getDataFolder().mkdirs();
            try {
                // Create new debug file
                logFile.createNewFile();
                // Load configuration
                debugConfig = YamlConfiguration.loadConfiguration(logFile);
                // Set creation timestamp
                debugConfig.set("created", LocalDateTime.now().format(TIME_FORMAT));
                // Save configuration to file
                debugConfig.save(logFile);
            } catch (IOException e) {
                // Log error if file creation fails
                plugin.getLogger().log(Level.SEVERE, "Failed to create debug file", e);
            }
        }
        // Load existing configuration
        debugConfig = YamlConfiguration.loadConfiguration(logFile);
    }

    /**
     * Adds a new timestamped log entry to the debug file
     * @param message The debug message to log
     */
    public void log(String message) {
        try {
            // Get current timestamp
            String timestamp = LocalDateTime.now().format(TIME_FORMAT);
            // Get existing log content
            String currentLog = debugConfig.getString("log", "");
            // Format new log entry with timestamp
            String newLog = String.format("%s[%s] %s", 
                currentLog.isEmpty() ? "" : currentLog + "\n", 
                timestamp, 
                message);
            
            // Save new log entry
            debugConfig.set("log", newLog);
            debugConfig.save(logFile);
        } catch (IOException e) {
            // Log error if writing fails
            plugin.getLogger().log(Level.SEVERE, "Failed to write to debug file", e);
        }
    }

    /**
     * Clears all debug entries and records the clearing time
     */
    public void clear() {
        try {
            // Remove all log entries
            debugConfig.set("log", "");
            // Record when log was cleared
            debugConfig.set("last_cleared", LocalDateTime.now().format(TIME_FORMAT));
            // Save changes to file
            debugConfig.save(logFile);
        } catch (IOException e) {
            // Log error if clearing fails
            plugin.getLogger().log(Level.SEVERE, "Failed to clear debug file", e);
        }
    }
}

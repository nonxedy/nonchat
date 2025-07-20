package com.nonxedy.nonchat.util.core.debugging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.plugin.Plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nonxedy.nonchat.Nonchat;

/**
 * Advanced debug logger with JSON format, rotation and levels
 */
public class Debugger {
    private final Plugin plugin;
    private final File logsFolder;
    private final int logRetentionDays;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Gson gson = new GsonBuilder().create();

    /**
     * Constructor sets up debugger with rotation settings
     * @param plugin Plugin instance
     * @param retentionDays How many days to keep logs (default 7)
     */
    public Debugger(Nonchat plugin, int retentionDays) {
        this.plugin = plugin;
        this.logsFolder = new File(plugin.getDataFolder(), "debug_logs");
        this.logRetentionDays = retentionDays > 0 ? retentionDays : 7;
        initialize();
    }

    private void initialize() {
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
        rotateLogs();
    }

    private synchronized void rotateLogs() {
        try {
            // Delete logs older than retention period
            LocalDate cutoffDate = LocalDate.now().minusDays(logRetentionDays);
            
            File[] logFiles = logsFolder.listFiles((dir, name) -> 
                name.startsWith("debug_") && name.endsWith(".log"));
            
            if (logFiles != null) {
                for (File logFile : logFiles) {
                    String dateStr = logFile.getName().substring(6, 16);
                    LocalDate logDate = LocalDate.parse(dateStr, dateFormatter);
                    
                    if (logDate.isBefore(cutoffDate)) {
                        Files.delete(logFile.toPath());
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to rotate debug logs: {0}", e.getMessage());
        }
    }

    private File getCurrentLogFile() {
        String fileName = "debug_" + LocalDate.now().format(dateFormatter) + ".log";
        return new File(logsFolder, fileName);
    }

    private synchronized void writeLogEntry(Map<String, Object> logData) {
        if (!plugin.getConfig().getBoolean("debug", false)) {
            return;
        }

        File logFile = getCurrentLogFile();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println("=== " + LocalDateTime.now().format(timeFormatter) + " ===");
            writer.println("Level: " + logData.get("level"));
            writer.println("Module: " + logData.get("module"));
            writer.println("Message: " + logData.get("message"));
            
            if (logData.containsKey("exception")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> excData = (Map<String, Object>) logData.get("exception");
                writer.println("\nException: ");
                writer.println("Type: " + excData.get("type"));
                writer.println("Message: " + excData.get("message"));
                writer.println("Stacktrace:");
                @SuppressWarnings("unchecked")
                List<String> stacktrace = (List<String>) excData.get("stacktrace");
                for (String trace : stacktrace) {
                    writer.println("  " + trace);
                }
            }
            writer.println(); // Empty line between entries
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write debug log: {0}", e.getMessage());
        }
    }

    /**
     * Debug level message 
     * @param module Module name
     * @param message Debug message
     */
    public void debug(String module, String message) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("level", "DEBUG");
        logData.put("module", module);
        logData.put("message", message);
        writeLogEntry(logData);
    }

    /**
     * Info level message
     * @param module Module name
     * @param message Info message
     */
    public void info(String module, String message) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("level", "INFO"); 
        logData.put("module", module);
        logData.put("message", message);
        writeLogEntry(logData);
    }

    /**
     * Warning level message
     * @param module Module name
     * @param message Warning message
     */
    public void warn(String module, String message) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("level", "WARN");
        logData.put("module", module);
        logData.put("message", message);
        writeLogEntry(logData);
    }

    /**
     * Error level message with exception
     * @param module Module name  
     * @param message Error message
     * @param exception Exception object
     */
    public void error(String module, String message, Throwable exception) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("level", "ERROR");
        logData.put("module", module);
        logData.put("message", message);
        
        if (exception != null) {
            Map<String, Object> excData = new HashMap<>();
            excData.put("type", exception.getClass().getSimpleName());
            excData.put("message", exception.getMessage());
            
            List<String> stackTrace = new LinkedList<>();
            for (StackTraceElement elem : exception.getStackTrace()) {
                stackTrace.add(elem.toString());
            }
            excData.put("stacktrace", stackTrace);
            
            logData.put("exception", excData);
        }
        
        writeLogEntry(logData);
    }

    /**
     * Cleans up all debug logs
     */
    public void cleanupAllLogs() {
        try {
            File[] logFiles = logsFolder.listFiles((dir, name) -> 
                name.startsWith("debug_") && name.endsWith(".log"));
            
            if (logFiles != null) {
                for (File logFile : logFiles) {
                    Files.delete(logFile.toPath());
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to clean debug logs: {0}", e.getMessage());
        }
    }

    /**
     * Gets recent debug entries
     * @param count Number of entries to retrieve
     * @return List of log entries as JSON strings
     */
    public List<String> getRecentLogs(int count) {
        List<String> logs = new LinkedList<>();
        File logFile = getCurrentLogFile();
        
        if (!logFile.exists()) return logs;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(line);
                if (logs.size() > count * 2) {
                    logs.remove(0); // keep only latest
                }
            }
            
            // Return the last 'count' entries
            return logs.subList(Math.max(0, logs.size() - count), logs.size());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read debug logs: {0}", e.getMessage());
            return List.of();
        }
    }
}

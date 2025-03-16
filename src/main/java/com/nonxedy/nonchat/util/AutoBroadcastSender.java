package com.nonxedy.nonchat.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.nonxedy.nonchat.config.PluginConfig;

/**
 * Manages automated broadcast system with support for regular and random broadcasts
 * Handles scheduling, sending, and managing broadcast messages
 */
public class AutoBroadcastSender {
    // Plugin instance reference
    private final Plugin plugin;
    // Configuration instance reference
    private final PluginConfig config;
    // Map to store active broadcast tasks with their identifiers
    private final Map<String, BukkitTask> activeTasks;
    // List to store messages for random broadcasting
    private final List<BroadcastMessage> randomMessagePool;
    // Task for random broadcast scheduling
    private BukkitTask randomBroadcastTask;

    // Constructor initializes the sender with plugin and config references
    public AutoBroadcastSender(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.activeTasks = new HashMap<>();
        this.randomMessagePool = new ArrayList<>();
    }

    /**
     * Initializes and starts the broadcast system
     * Sets up regular or random broadcasts based on configuration
     */
    public void start() {
        // Stop any existing broadcasts before starting
        stop();
        
        // Get configured broadcast messages from config
        Map<String, BroadcastMessage> configuredMessages = config.getBroadcastMessages();
        
        // Process each configured message
        configuredMessages.forEach((key, message) -> {
            // Skip disabled messages
            if (!message.isEnabled()) return;
            
            // Add message to random pool or schedule regular broadcast
            if (config.isRandomBroadcastEnabled()) {
                randomMessagePool.add(message);
            } else {
                scheduleRegularBroadcast(key, message);
            }
        });

        // Start random broadcasts if enabled and messages exist
        if (config.isRandomBroadcastEnabled() && !randomMessagePool.isEmpty()) {
            startRandomBroadcasts();
        }
    }

    /**
     * Schedules a regular interval broadcast
     * @param key Identifier for the broadcast task
     * @param message Message configuration to broadcast
     */
    private void scheduleRegularBroadcast(String key, BroadcastMessage message) {
        // Create async task that repeats at specified intervals
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, 
            () -> broadcastMessage(message.getMessage()),
            0L,
            message.getInterval() * 20L  // Convert seconds to ticks (20 ticks = 1 second)
        );
        // Store the task for later management
        activeTasks.put(key, task);
    }

    // Initiates random message broadcasting system
    private void startRandomBroadcasts() {
        // Create task that randomly selects and broadcasts messages every minute
        randomBroadcastTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
            () -> {
                if (!randomMessagePool.isEmpty()) {
                    // Select random message from pool
                    BroadcastMessage message = randomMessagePool.get(
                        new Random().nextInt(randomMessagePool.size())
                    );
                    broadcastMessage(message.getMessage());
                }
            },
            0L,
            60 * 20L  // Run every minute (60 seconds * 20 ticks)
        );
    }

    /**
     * Sends a broadcast message to all players
     * @param message The message to broadcast
     */
    private void broadcastMessage(String message) {
        // Convert color codes and send message to server
        Bukkit.getServer().sendMessage(ColorUtil.parseComponent(message));
    }

    // Stops all broadcast tasks and clears message pools
    public void stop() {
        // Cancel all regular broadcast tasks
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
        
        // Cancel random broadcast task if exists
        if (randomBroadcastTask != null) {
            randomBroadcastTask.cancel();
            randomBroadcastTask = null;
        }
        
        // Clear the random message pool
        randomMessagePool.clear();
    }
}

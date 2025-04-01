package com.nonxedy.nonchat.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.util.BroadcastMessage;
import com.nonxedy.nonchat.util.ColorUtil;

import net.kyori.adventure.text.Component;

public class BroadcastManager {
    private final Plugin plugin;
    private final PluginConfig config;
    private final Map<String, BukkitTask> activeTasks;
    private final List<BroadcastMessage> randomMessagePool;
    private BukkitTask randomBroadcastTask;

    public BroadcastManager(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.activeTasks = new HashMap<>();
        this.randomMessagePool = new ArrayList<>();
        start();
    }

    public void start() {
        stop();
        Map<String, BroadcastMessage> configuredMessages = config.getBroadcastMessages();
        
        configuredMessages.forEach((key, message) -> {
            if (!message.isEnabled()) return;
            
            if (config.isRandomBroadcastEnabled()) {
                randomMessagePool.add(message);
            } else {
                scheduleRegularBroadcast(key, message);
            }
        });

        if (config.isRandomBroadcastEnabled() && !randomMessagePool.isEmpty()) {
            startRandomBroadcasts();
        }
    }

    public void broadcast(CommandSender sender, String message) {
        try {
            // Try to use Adventure API first
            Component formatted = ColorUtil.parseComponent(message);
            Bukkit.broadcast(formatted);
        } catch (NoSuchMethodError e) {
            // Fall back to traditional Bukkit broadcast if Adventure API is not available
            String legacyMessage = ColorUtil.parseColor(message);
            Bukkit.broadcastMessage(legacyMessage);
        }
    }

    private void scheduleRegularBroadcast(String key, BroadcastMessage message) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            broadcast(Bukkit.getConsoleSender(), message.getMessage());
        }, 0L, message.getInterval() * 20L);
        
        activeTasks.put(key, task);
    }

    private void startRandomBroadcasts() {
        randomBroadcastTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (randomMessagePool.isEmpty()) return;
            
            int randomIndex = new Random().nextInt(randomMessagePool.size());
            BroadcastMessage message = randomMessagePool.get(randomIndex);
            broadcast(Bukkit.getConsoleSender(), message.getMessage());
        }, 0L, config.getBroadcastInterval() * 20L);
    }

    public void stop() {
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
        
        if (randomBroadcastTask != null) {
            randomBroadcastTask.cancel();
            randomBroadcastTask = null;
        }
    }

    public void reload() {
        stop();
        start();
    }
}

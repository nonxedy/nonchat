package com.nonxedy.nonchat.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.nonxedy.nonchat.config.PluginConfig;

public class AutoBroadcastSender {
    private final Plugin plugin;
    private final PluginConfig config;
    private final Map<String, BukkitTask> activeTasks;
    private final List<BroadcastMessage> randomMessagePool;
    private BukkitTask randomBroadcastTask;

    public AutoBroadcastSender(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.activeTasks = new HashMap<>();
        this.randomMessagePool = new ArrayList<>();
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

    private void scheduleRegularBroadcast(String key, BroadcastMessage message) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, 
            () -> broadcastMessage(message.getMessage()),
            0L,
            message.getInterval() * 20L
        );
        activeTasks.put(key, task);
    }

    private void startRandomBroadcasts() {
        randomBroadcastTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
            () -> {
                if (!randomMessagePool.isEmpty()) {
                    BroadcastMessage message = randomMessagePool.get(
                        new Random().nextInt(randomMessagePool.size())
                    );
                    broadcastMessage(message.getMessage());
                }
            },
            0L,
            60 * 20L
        );
    }

    private void broadcastMessage(String message) {
        Bukkit.getServer().sendMessage(ColorUtil.parseComponent(message));
    }

    public void stop() {
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
        
        if (randomBroadcastTask != null) {
            randomBroadcastTask.cancel();
            randomBroadcastTask = null;
        }
        
        randomMessagePool.clear();
    }
}

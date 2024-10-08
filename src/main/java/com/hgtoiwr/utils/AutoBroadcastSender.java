package com.hgtoiwr.utils;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.hgtoiwr.config.PluginConfig;

@SuppressWarnings("deprecation")
public class AutoBroadcastSender {

    private final PluginConfig config;
    private Map<String, BukkitRunnable> broadcastTasks;

    public AutoBroadcastSender(PluginConfig config) {
        this.config = config;
        this.broadcastTasks = new HashMap<>();
    }

    public void start() {
        Map<String, BroadcastMessage> messages = config.getBroadcastMessages();
        for (Map.Entry<String, BroadcastMessage> entry : messages.entrySet()) {
            BroadcastMessage message = entry.getValue();
            if (message.isEnabled()) {
                BukkitRunnable task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.broadcastMessage(message.getMessage());
                    }
                };
                task.runTaskTimer(Bukkit.getPluginManager().getPlugin("nonchat"), 0, 20 * message.getInterval());
                broadcastTasks.put(entry.getKey(), task);
            }
        }
    }

    public void stop() {
        for (BukkitRunnable task : broadcastTasks.values()) {
            task.cancel();
        }
        broadcastTasks.clear();
    }
}
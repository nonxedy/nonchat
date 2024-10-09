package com.hgtoiwr.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.hgtoiwr.config.PluginConfig;

@SuppressWarnings("deprecation")
public class AutoBroadcastSender {

    private final PluginConfig config;
    private Map<String, BukkitRunnable> broadcastTasks;
    private List<BroadcastMessage> randomMessages;

    public AutoBroadcastSender(PluginConfig config) {
        this.config = config;
        this.broadcastTasks = new HashMap<>();
        this.randomMessages = new ArrayList<>();
    }

    public void start() {
        Map<String, BroadcastMessage> messages = config.getBroadcastMessages();
        for (Map.Entry<String, BroadcastMessage> entry : messages.entrySet()) {
            BroadcastMessage message = entry.getValue();
            if (message.isEnabled()) {
                if (config.isRandomBroadcastEnabled()) {
                    randomMessages.add(message);
                } else {
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
        if (config.isRandomBroadcastEnabled()) {
            startRandomBroadcast();
        }
    }

    private void startRandomBroadcast() {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!randomMessages.isEmpty()) {
                    BroadcastMessage message = randomMessages.get(new Random().nextInt(randomMessages.size()));
                    Bukkit.broadcastMessage(message.getMessage());
                }
            }
        };
        task.runTaskTimer(Bukkit.getPluginManager().getPlugin("nonchat"), 0, 20 * 60);
    }

    public void stop() {
        for (BukkitRunnable task : broadcastTasks.values()) {
            task.cancel();
        }
        broadcastTasks.clear();
    }
}
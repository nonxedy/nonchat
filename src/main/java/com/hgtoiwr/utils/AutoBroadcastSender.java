package com.hgtoiwr.utils;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.hgtoiwr.config.PluginConfig;

@SuppressWarnings("deprecation")
public class AutoBroadcastSender {

    private final PluginConfig config;
    private BukkitRunnable broadcastTask;

    public AutoBroadcastSender(PluginConfig config) {
        this.config = config;
    }

    public void start() {
        if (!config.isBroadcastEnabled()) {
            return;
        }
        broadcastTask = new BukkitRunnable() {

            @Override
            public void run() {
                Bukkit.broadcastMessage(config.getBroadcastMessage());
            }
        };
        broadcastTask.runTaskTimer(Bukkit.getPluginManager().getPlugin("nonchat"), 0, 20 * config.getBroadcastInterval());
    }

    public void stop() {
        if (broadcastTask != null) {
            broadcastTask.cancel();
        }
    }
}

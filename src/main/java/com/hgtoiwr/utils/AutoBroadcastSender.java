package com.hgtoiwr.utils;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

@SuppressWarnings("deprecation")
public class AutoBroadcastSender {

    public void start() {
        BukkitRunnable broadcastTask = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("This message will be sent every 60 seconds");
            }
        };
        broadcastTask.runTaskTimer(Bukkit.getPluginManager().getPlugin("nonchat"), 0, 20 * 60); // send a message every 60 seconds
    }
}
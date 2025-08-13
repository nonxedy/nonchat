package com.nonxedy.nonchat.util.folia;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Bukkit implementation of FoliaScheduler for traditional Bukkit/Paper/Spigot servers
 */
public class BukkitSchedulerImpl implements FoliaScheduler {
    
    private final Plugin plugin;
    
    public BukkitSchedulerImpl(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public BukkitTask runTask(Runnable task) {
        return plugin.getServer().getScheduler().runTask(plugin, task);
    }
    
    @Override
    public BukkitTask runTaskAsynchronously(Runnable task) {
        return plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }
    
    @Override
    public BukkitTask runTaskLater(Runnable task, long delay) {
        return plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
    }
    
    @Override
    public BukkitTask runTaskLaterAsynchronously(Runnable task, long delay) {
        return plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
    }
    
    @Override
    public BukkitTask runTaskTimer(Runnable task, long delay, long period) {
        return plugin.getServer().getScheduler().runTaskTimer(plugin, task, delay, period);
    }
    
    @Override
    public BukkitTask runTaskTimerAsynchronously(Runnable task, long delay, long period) {
        return plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
    }
    
    @Override
    public BukkitTask runTaskForEntity(Entity entity, Runnable task) {
        // On Bukkit, we just run on main thread
        return runTask(task);
    }
    
    @Override
    public BukkitTask runTaskLaterForEntity(Entity entity, Runnable task, long delay) {
        // On Bukkit, we just run on main thread with delay
        return runTaskLater(task, delay);
    }
    
    @Override
    public BukkitTask runTaskAtLocation(Location location, Runnable task) {
        // On Bukkit, we just run on main thread
        return runTask(task);
    }
    
    @Override
    public BukkitTask runTaskLaterAtLocation(Location location, Runnable task, long delay) {
        // On Bukkit, we just run on main thread with delay
        return runTaskLater(task, delay);
    }
    
    @Override
    public BukkitTask runTaskForPlayer(Player player, Runnable task) {
        // On Bukkit, we just run on main thread
        return runTask(task);
    }
    
    @Override
    public BukkitTask runTaskLaterForPlayer(Player player, Runnable task, long delay) {
        // On Bukkit, we just run on main thread with delay
        return runTaskLater(task, delay);
    }
}

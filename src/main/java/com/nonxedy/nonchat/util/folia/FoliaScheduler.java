package com.nonxedy.nonchat.util.folia;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Abstraction layer for scheduler operations that works on both Bukkit and Folia
 */
public interface FoliaScheduler {
    
    /**
     * Runs a task on the main thread
     * @param task The task to run
     * @return BukkitTask representing the task
     */
    BukkitTask runTask(Runnable task);
    
    /**
     * Runs a task asynchronously
     * @param task The task to run
     * @return BukkitTask representing the task
     */
    BukkitTask runTaskAsynchronously(Runnable task);
    
    /**
     * Runs a task on the main thread after the specified number of ticks
     * @param task The task to run
     * @param delay The delay in ticks
     * @return BukkitTask representing the task
     */
    BukkitTask runTaskLater(Runnable task, long delay);
    
    /**
     * Runs a task asynchronously after the specified number of ticks
     * @param task The task to run
     * @param delay The delay in ticks
     * @return BukkitTask representing the task
     */
    BukkitTask runTaskLaterAsynchronously(Runnable task, long delay);
    
    /**
     * Runs a task timer on the main thread
     * @param task The task to run
     * @param delay The initial delay in ticks
     * @param period The period in ticks
     * @return BukkitTask representing the task
     */
    BukkitTask runTaskTimer(Runnable task, long delay, long period);
    
    /**
     * Runs a task timer asynchronously
     * @param task The task to run
     * @param delay The initial delay in ticks
     * @param period The period in ticks
     * @return BukkitTask representing the task
     */
    BukkitTask runTaskTimerAsynchronously(Runnable task, long delay, long period);
    
    /**
     * Runs a task for a specific entity
     * @param entity The entity to run the task for
     * @param task The task to run
     * @return BukkitTask representing the task
     */
    BukkitTask runTaskForEntity(Entity entity, Runnable task);
    
    /**
     * Runs a task for a specific entity after a delay
     * @param entity The entity to run the task for
     * @param task The task to run
     * @param delay The delay in ticks
     * @return BukkitTask representing the task
     */
    BukkitTask runTaskLaterForEntity(Entity entity, Runnable task, long delay);
    
    /**
     * Runs a task at a specific location
     * @param location The location to run the task at
     * @param task The task to run
     * @return BukkitTask representing the task
     */
    BukkitTask runTaskAtLocation(Location location, Runnable task);
    
    /**
     * Runs a task at a specific location after a delay
     * @param location The location to run the task at
     * @param task The task to run
     * @param delay The delay in ticks
     * @return BukkitTask representing the task
     */
    BukkitTask runTaskLaterAtLocation(Location location, Runnable task, long delay);
    
    /**
     * Runs a task for a specific player
     * @param player The player to run the task for
     * @param task The task to run
     * @return BukkitTask representing the task
     */
    BukkitTask runTaskForPlayer(Player player, Runnable task);
    
    /**
     * Runs a task for a specific player after a delay
     * @param player The player to run the task for
     * @param task The task to run
     * @param delay The delay in ticks
     * @return BukkitTask representing the task
     */
    BukkitTask runTaskLaterForPlayer(Player player, Runnable task, long delay);
}

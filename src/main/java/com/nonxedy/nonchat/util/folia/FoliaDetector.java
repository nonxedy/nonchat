package com.nonxedy.nonchat.util.folia;

import org.bukkit.plugin.Plugin;

/**
 * Utility class to detect if the server is running Folia
 */
public class FoliaDetector {
    private static Boolean isFolia = null;

    /**
     * Checks if the server is running Folia
     * @return true if Folia is detected, false otherwise
     */
    public static boolean isFolia() {
        if (isFolia == null) {
            try {
                // Try to access Folia-specific class
                Class<?> regionizedServerClass = Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = regionizedServerClass != null;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }

    /**
     * Gets the scheduler appropriate for the current server implementation
     * @param plugin The plugin instance
     * @return Scheduler instance
     */
    public static FoliaScheduler getScheduler(Plugin plugin) {
        if (isFolia()) {
            return new FoliaSchedulerImpl(plugin);
        } else {
            return new BukkitSchedulerImpl(plugin);
        }
    }
}

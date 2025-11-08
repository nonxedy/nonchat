package com.nonxedy.nonchat.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.config.PluginConfig;

/**
 * Asynchronous configuration saver with write-behind pattern
 * Batches configuration changes and saves them periodically
 */
public class AsyncConfigSaver {

    private static class ConfigSaveTask {
        final String path;
        final Object value;

        ConfigSaveTask(String path, Object value) {
            this.path = path;
            this.value = value;
        }
    }

    private final BlockingQueue<ConfigSaveTask> saveQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread thread = new Thread(r, "nonchat-config-saver");
        thread.setDaemon(true);
        return thread;
    });
    private final PluginConfig config;
    private final Nonchat plugin;

    public AsyncConfigSaver(Nonchat plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;

        // Schedule periodic batch saving every 5 seconds
        scheduler.scheduleWithFixedDelay(this::processBatch, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * Queues a configuration change for asynchronous saving
     * @param path Configuration path
     * @param value Value to set
     */
    public void saveAsync(String path, Object value) {
        try {
            saveQueue.offer(new ConfigSaveTask(path, value));
        } catch (Exception e) {
            plugin.logError("Failed to queue config save for path: " + path + ", error: " + e.getMessage());
            // Fallback to immediate save on queue failure
            try {
                config.set(path, value);
                config.saveConfig();
            } catch (Exception saveError) {
                plugin.logError("Fallback config save also failed for path: " + path + ", error: " + saveError.getMessage());
            }
        }
    }

    /**
     * Forces immediate saving of all queued changes
     */
    public void saveNow() {
        processBatch();
    }

    /**
     * Processes the batch of queued configuration changes
     */
    private void processBatch() {
        List<ConfigSaveTask> batch = new ArrayList<>();
        saveQueue.drainTo(batch);

        if (batch.isEmpty()) {
            return;
        }

        try {
            // Apply all changes to config
            for (ConfigSaveTask task : batch) {
                config.set(task.path, task.value);
            }

            // Save to file
            config.saveConfig();

            if (plugin.getConfigService().getConfig().isDebug()) {
                plugin.logResponse("Saved " + batch.size() + " configuration changes");
            }
        } catch (Exception e) {
            plugin.logError("Failed to save config batch: " + e.getMessage());
            // Try to save individual changes on batch failure
            for (ConfigSaveTask task : batch) {
                try {
                    config.set(task.path, task.value);
                    config.saveConfig();
                } catch (Exception individualError) {
                    plugin.logError("Failed to save individual config change for path: " + task.path +
                                  ", error: " + individualError.getMessage());
                }
            }
        }
    }

    /**
     * Shuts down the async config saver
     */
    public void shutdown() {
        // Process any remaining changes before shutdown
        saveNow();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

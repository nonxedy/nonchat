package com.nonxedy.nonchat.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bukkit.entity.Player;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.util.chat.filters.AdDetector;

/**
 * Asynchronous service for processing heavy chat filters
 * Prevents main thread blocking during filter operations
 * 
 * TODO: This service is instantiated but never actually used in ChatManager 🙋‍♀️
 *       Either wire it up to run filter checks asynchronously,
 *       or just get rid of it if it's not pulling its weight.
 *       If we do end up using it, adding a cache (e.g. Caffeine)
 *       would be a good idea to avoid re-checking the same messages
 *       over and over again.
 */
public class AsyncFilterService {

    private final ExecutorService filterExecutor;
    private final AdDetector adDetector;
    private final Nonchat plugin;

    public AsyncFilterService(Nonchat plugin, AdDetector adDetector) {
        this.plugin = plugin;
        this.adDetector = adDetector;

        // Use cached thread pool for dynamic scaling
        this.filterExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "nonchat-filter-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Asynchronously checks if a message should be filtered
     * @param player The player sending the message
     * @param message The message content
     * @return CompletableFuture with filter result
     */
    public CompletableFuture<Boolean> shouldFilterAsync(Player player, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return adDetector.shouldFilter(player, message);
            } catch (Exception e) {
                // Log error and allow message through on filter failure
                plugin.logError("Error in async filter check for player " + player.getName() + ": " + e.getMessage());
                return false;
            }
        }, filterExecutor);
    }

    /**
     * Shuts down the filter executor
     */
    public void shutdown() {
        filterExecutor.shutdown();
    }
}

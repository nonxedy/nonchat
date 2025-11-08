package com.nonxedy.nonchat.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.util.chat.filters.AdDetector;

/**
 * Asynchronous service for processing heavy chat filters
 * Prevents main thread blocking during filter operations
 */
public class AsyncFilterService {

    private final ExecutorService filterExecutor;
    private final AdDetector adDetector;
    private final Nonchat plugin;
    private final Cache<String, Boolean> filterCache;

    public AsyncFilterService(Nonchat plugin, AdDetector adDetector) {
        this.plugin = plugin;
        this.adDetector = adDetector;

        // Initialize cache for filter results
        this.filterCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

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

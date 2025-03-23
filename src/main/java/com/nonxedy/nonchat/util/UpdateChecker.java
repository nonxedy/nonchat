package com.nonxedy.nonchat.util;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Handles version checking and update notifications for the plugin
 * Connects to Modrinth API to check for new versions
 */
public class UpdateChecker implements Listener {

    // Initialize class variables
    private final JavaPlugin plugin;
    private final String currentVersion;
    /** Modrinth API endpoint for version checking */
    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/nonchat/version";
    
    // Store update information
    private String latestVersion = null;
    private String downloadUrl = null;
    private boolean updateAvailable = false;

    // Constructor to initialize class variables
    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        
        // Register this as an event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Run initial update check
        checkForUpdates().thenAccept(hasUpdate -> {
            if (hasUpdate) {
                plugin.getLogger().info("New version available: " + latestVersion);
                plugin.getLogger().info("Download: " + downloadUrl);
                
                // Notify online admins
                notifyOnlinePlayers();
            } else {
                plugin.getLogger().info("You are using the latest version of nonchat!");
            }
        });
    }

    /**
     * Performs an asynchronous check for plugin updates
     * Compares current version with latest version from Modrinth
     * @return CompletableFuture<Boolean> that completes with true if update is available
     */
    public CompletableFuture<Boolean> checkForUpdates() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Run the update check asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Connect to Modrinth API
                URL url = new URL(MODRINTH_API);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                // Parse JSON response to get latest version info
                JsonObject latestVersion = JsonParser.parseReader(
                    new InputStreamReader(connection.getInputStream())
                ).getAsJsonArray().get(0).getAsJsonObject();

                // Extract version information
                this.latestVersion = latestVersion.get("version_number").getAsString();
                this.downloadUrl = "https://modrinth.com/plugin/nonchat/version/" + this.latestVersion;

                // Compare versions and update status
                this.updateAvailable = !currentVersion.equals(this.latestVersion);
                future.complete(this.updateAvailable);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
                future.complete(false);
            }
        });
        
        return future;
    }

    /**
     * Notifies all currently online players with appropriate permissions
     */
    private void notifyOnlinePlayers() {
        if (!updateAvailable) return;
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (hasUpdatePermission(player)) {
                    sendUpdateNotification(player);
                }
            }
        });
    }
    
    /**
     * Checks if a player has permission to see update notifications
     * @param player The player to check
     * @return true if player has permission
     */
    private boolean hasUpdatePermission(Player player) {
        return player.hasPermission("nonchat.admin") || 
               player.isOp();
    }
    
    /**
     * Sends update notification to a specific player
     * @param player The player to notify
     */
    private void sendUpdateNotification(Player player) {
        String message = ColorUtil.parseColor("&#FFAFFB[nonchat] &#ffffff A new version is available: &#FFAFFB" + latestVersion + 
                        "\n&#FFAFFB[nonchat] &#ffffffDownload: &#FFAFFB" + downloadUrl);
        player.sendMessage(message);
    }
    
    /**
    * Listens for player join events to notify admins about updates
    * @param event The player join event
    */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
    
        // If no update is available or update check hasn't completed yet, run a new check
        if (!updateAvailable && latestVersion == null) {
            checkForUpdates().thenAccept(hasUpdate -> {
                if (hasUpdate && hasUpdatePermission(player)) {
                    // Delay the message slightly to ensure it's seen after join messages
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        sendUpdateNotification(player);
                    }, 20L); // 1 second delay
                }
            });
        } 
        // If we already know an update is available, notify the player
        else if (updateAvailable && hasUpdatePermission(player)) {
            // Delay the message slightly to ensure it's seen after join messages
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                sendUpdateNotification(player);
            }, 20L); // 1 second delay
        }
    }
}

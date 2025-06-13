package com.nonxedy.nonchat.util;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nonxedy.nonchat.Nonchat;

import net.kyori.adventure.text.Component;

/**
 * Handles version checking and update notifications for the plugin
 * Connects to Modrinth API to check for new versions
 */
public class UpdateChecker implements Listener {

    // Initialize class variables
    private final Nonchat plugin;
    private final String currentVersion;
    /** Modrinth API endpoint for version checking */
    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/nonchat/version";
    
    // Store update information
    private String latestVersion = null;
    private String downloadUrl = null;
    private boolean updateAvailable = false;

    // Constructor to initialize class variables
    public UpdateChecker(Nonchat plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        
        if (plugin instanceof Nonchat) {
            Nonchat nonchatPlugin = (Nonchat) plugin;
            if (!nonchatPlugin.getConfigService().getConfig().isUpdateCheckerEnabled()) {
                plugin.logResponse("Update checker is disabled in config");
                return;
            }
        }

        // Register this as an event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Run initial update check
        plugin.logResponse("Initializing update checker for nonchat version " + currentVersion);
        checkForUpdates().thenAccept(hasUpdate -> {
            if (hasUpdate) {
                plugin.logResponse("New version available: " + latestVersion);
                plugin.logResponse("Download: " + downloadUrl);
                
                // Notify online admins
                notifyOnlinePlayers();
            } else {
                plugin.logResponse("You are using the latest version of nonchat!");
            }
        });
    }

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
                
                // Log connection attempt
                plugin.logResponse("Checking for updates from: " + MODRINTH_API);
                
                // Parse JSON response to get latest version info
                JsonObject latestVersion = JsonParser.parseReader(
                    new InputStreamReader(connection.getInputStream())
                ).getAsJsonArray().get(0).getAsJsonObject();
    
                // Extract version information
                this.latestVersion = latestVersion.get("version_number").getAsString();
                this.downloadUrl = "https://modrinth.com/plugin/nonchat/version/" + this.latestVersion;
    
                // Log received version info
                plugin.logResponse("Current version: " + currentVersion);
                plugin.logResponse("Latest version: " + this.latestVersion);
    
                // Compare versions and update status
                this.updateAvailable = !currentVersion.equals(this.latestVersion);
                plugin.logResponse("Update available: " + this.updateAvailable);
                
                future.complete(this.updateAvailable);
            } catch (Exception e) {
                plugin.logError("Failed to check for updates: " + e.getMessage());
                e.printStackTrace();
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
    
    private boolean hasUpdatePermission(Player player) {
        boolean hasPermission = player.hasPermission("nonchat.*") || 
                               player.isOp();
        
        return hasPermission;
    }
    
    /**
     * Sends update notification to a specific player
     * @param player The player to notify
     */
    private void sendUpdateNotification(Player player) {
        try {
            player.sendMessage(ColorUtil.parseComponent("&#FFAFFB[nonchat] &#ffffff A new version is available: &#FFAFFB" + latestVersion));
        
            Component downloadMessage = Component.text()
                .append(ColorUtil.parseComponent("&#FFAFFB[nonchat] &#ffffffDownload: "))
                .append(LinkDetector.makeLinksClickable(downloadUrl))
                .build();
        
            player.sendMessage(downloadMessage);
            
            plugin.logResponse("Sent update notification to admin: " + player.getName());
        } catch (Exception e) {
            plugin.logError("Failed to send update notification to " + player.getName() + ": " + e.getMessage());
        }
    }

    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (hasUpdatePermission(player)) {
            if (updateAvailable) {
                // Delay the message slightly to ensure it's seen after join messages
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    sendUpdateNotification(player);
                }, 40L); // 2 second delay for better visibility
            } else if (latestVersion == null) {
                checkForUpdates().thenAccept(hasUpdate -> {
                    if (hasUpdate) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            sendUpdateNotification(player);
                        }, 40L);
                    }
                });
            }
        }
    }
}
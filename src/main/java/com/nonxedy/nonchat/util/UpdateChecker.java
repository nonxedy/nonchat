package com.nonxedy.nonchat.util;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Handles version checking and update notifications for the plugin
 * Connects to Modrinth API to check for new versions
 */
public class UpdateChecker {

    // Initialize class variables
    private final JavaPlugin plugin;
    private final String currentVersion;
    /** Modrinth API endpoint for version checking */
    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/nonchat/version";

    // Constructor to initialize class variables
    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    /**
     * Performs an asynchronous check for plugin updates
     * Compares current version with latest version from Modrinth
     */
    public void checkForUpdates() {
        // Run the update check asynchronously on the main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Connect to Modrinth API
                URL url = new URL(MODRINTH_API);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                // Parse JSON response to get latest version info
                JsonObject latestVersion = JsonParser.parseReader(
                    new InputStreamReader(connection.getInputStream())
                ).getAsJsonArray().get(0).getAsJsonObject();

                // Extract version information
                String latestVersionNumber = latestVersion.get("version_number").getAsString();
                String downloadUrl = "https://modrinth.com/plugin/nonchat/version/" + latestVersionNumber;

                // Compare versions and notify accordingly
                if (!currentVersion.equals(latestVersionNumber)) {
                    notifyUpdate(latestVersionNumber, downloadUrl);
                } else {
                    notifyUpToDate();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    /**
     * Sends update notification to players with appropriate permissions
     * @param newVersion The latest available version number
     * @param downloadUrl URL where the new version can be downloaded
     */
    private void notifyUpdate(String newVersion, String downloadUrl) {
        String message = ColorUtil.parseColor("&#FFAFFB[nonchat] &#ffffff A new version is available: &#FFAFFB" + newVersion + 
                        "\n&#FFAFFB[nonchat] &#ffffffDownload: &#FFAFFB" + downloadUrl);
        
        /// Send the notification to all players with the permission "nonchat.*"
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getLogger().info("New version available: " + newVersion);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("nonchat.*")) {
                    player.sendMessage(message);
                }
            }
        });
    }
    
    /**
     * Logs a message indicating the plugin is up to date
     */
    private void notifyUpToDate() {
        plugin.getLogger().info("You are using the latest version of nonchat!");
    }
}

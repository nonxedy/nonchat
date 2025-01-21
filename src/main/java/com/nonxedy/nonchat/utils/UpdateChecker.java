package com.nonxedy.nonchat.utils;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// UpdateChecker class to check for plugin updates
public class UpdateChecker {

    // Initialize class variables
    private final JavaPlugin plugin;
    private final String currentVersion;
    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/nonchat/version";

    // Constructor to initialize class variables
    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    // Method to check for updates asynchronously
    public void checkForUpdates() {
        // Run the update check asynchronously on the main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Make a GET request to the Modrinth API to get the latest version
                URL url = new URL(MODRINTH_API);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                JsonObject latestVersion = JsonParser.parseReader(
                    new InputStreamReader(connection.getInputStream())
                ).getAsJsonArray().get(0).getAsJsonObject();

                String latestVersionNumber = latestVersion.get("version_number").getAsString();
                String downloadUrl = "https://modrinth.com/plugin/nonchat/version/" + latestVersionNumber;

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

    // Method to notify players about an update
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
    
    // Method to notify players that the plugin is up to date
    private void notifyUpToDate() {
        plugin.getLogger().info("You are using the latest version of nonchat!");
    }
}

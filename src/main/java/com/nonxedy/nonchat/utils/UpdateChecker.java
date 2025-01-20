package com.nonxedy.nonchat.utils;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private final JavaPlugin plugin;
    private final String currentVersion;
    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/nonchat/version";

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
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

    private void notifyUpdate(String newVersion, String downloadUrl) {
        String message = ColorUtil.parseColor("&#FFAFFB[nonchat] &#ffffff A new version is available: &#FFAFFB" + newVersion + 
                        "\n&#FFAFFB[nonchat] &#ffffffDownload: &#FFAFFB" + downloadUrl);
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getLogger().info("New version available: " + newVersion);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("nonchat.*")) {
                    player.sendMessage(message);
                }
            }
        });
    }
    

    private void notifyUpToDate() {
        plugin.getLogger().info("You are using the latest version of nonchat!");
    }
}

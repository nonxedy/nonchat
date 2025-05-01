package com.nonxedy.nonchat.hook;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;

public class DiscordHook {

    private final nonchat plugin;
    private DiscordSRVHook discordSRVHook;

    public DiscordHook(nonchat plugin) {
        this.plugin = plugin;
        if (plugin.getDiscordManager().isUseDiscordSRV()) {
            this.discordSRVHook = new DiscordSRVHook(plugin);
        }
    }

    /**
     * Send a message to Discord using either webhook or DiscordSRV
     * @param message The message to send
     * @param channelId The Discord channel ID to send to (only used with DiscordSRV)
     * @param URL The webhook URL (only used if DiscordSRV is disabled)
     */
    public void sendMessage(String message, String channelId, String URL) {
        // If DiscordSRV integration is enabled and hooked, use it
        if (plugin.getDiscordManager().isUseDiscordSRV() && 
            discordSRVHook != null && 
            discordSRVHook.isHooked()) {
            discordSRVHook.sendMessageToChannel(channelId != null ? channelId : "global", message);
            return;
        }
        
        // Otherwise, use webhook
        // Check if URL is provided and not empty
        if (URL == null || URL.isEmpty()) {
            plugin.getLogger().warning("Discord webhook URL is not set. Unable to send message to Discord.");
            return;
        }
        
        HttpURLConnection connection = null;
        try {
            URL url = new URL(URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            String jsonPayload = "{\"content\": \"" + escapeJson(message) + "\"}";
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_NO_CONTENT) {
                plugin.getLogger().info("Failed to send message. HTTP error code: " + code);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Send a message to Discord using either webhook or DiscordSRV
     * This is a backward compatibility method that defaults to the "global" channel for DiscordSRV
     * @param message The message to send
     * @param URL The webhook URL (only used if DiscordSRV is disabled)
     */
    public void sendMessage(String message, String URL) {
        sendMessage(message, "global", URL);
    }

    /**
     * Send a player join message to Discord
     * @param username The name of the player who joined
     */
    public void sendJoinMessage(String username) {
        // If DiscordSRV integration is enabled and hooked, use it
        if (plugin.getDiscordManager().isUseDiscordSRV() && 
            discordSRVHook != null && 
            discordSRVHook.isHooked()) {
            discordSRVHook.sendJoinMessage(username);
            return;
        }
        
        // Otherwise, use webhook
        if (plugin.getDiscordManager().isJoinEnabled()) {
            String webhookUrl = plugin.getDiscordManager().getDiscordHook();
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                plugin.getLogger().warning("Discord webhook URL is not set. Unable to send join message to Discord.");
                return;
            }
            
            HttpURLConnection connection = null;
            try {
                URL url = new URL(webhookUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                String jsonPayload = buildEmbedJoin(username);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_NO_CONTENT) {
                    plugin.getLogger().info("Failed to send message. HTTP error code: " + code);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    /**
     * Send a player left message to Discord
     * @param username The name of the player who left
     */
    public void sendLeftMessage(String username) {
        // If DiscordSRV integration is enabled and hooked, use it
        if (plugin.getDiscordManager().isUseDiscordSRV() && 
            discordSRVHook != null && 
            discordSRVHook.isHooked()) {
            discordSRVHook.sendLeftMessage(username);
            return;
        }
        
        // Otherwise, use webhook
        if (plugin.getDiscordManager().isQuitEnabled()) {
            String webhookUrl = plugin.getDiscordManager().getDiscordHook();
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                plugin.getLogger().warning("Discord webhook URL is not set. Unable to send quit message to Discord.");
                return;
            }
            
            HttpURLConnection connection = null;
            try {
                URL url = new URL(webhookUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                String jsonPayload = buildEmbedLeft(username);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_NO_CONTENT) {
                    plugin.getLogger().info("Failed to send message. HTTP error code: " + code);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    private @NotNull String buildEmbedLeft(String username) {
        String descriptionTemplate = plugin.getDiscordManager().getQuitDescription();
        String description = descriptionTemplate.replace("%player%", escapeJson(username));
        String avatarUrl = "https://mc-heads.net/avatar/" + username;

        StringBuilder embedBuilder = new StringBuilder();
        embedBuilder.append("{")
                .append("\"embeds\": [{").append("\"title\": \"").append(plugin.getDiscordManager().getQuitTitle()).append("\",").append("\"description\": \"").append(escapeJson(description)).append("\",").append("\"color\": ").append(plugin.getDiscordManager().getQuitColor());

        if (plugin.getDiscordManager().isQuitAvatarEnabled()) {
            embedBuilder.append(",")
                    .append("\"thumbnail\": {").append("\"url\": \"").append(escapeJson(avatarUrl)).append("\"")
                    .append("}");
        }

        embedBuilder.append("}]")
                .append("}");

        return embedBuilder.toString();
    }

    private @NotNull String buildEmbedJoin(String username) {
        String descriptionTemplate = plugin.getDiscordManager().getJoinDescription();
        String description = descriptionTemplate.replace("%player%", escapeJson(username));
        String avatarUrl = "https://mc-heads.net/avatar/" + username;

        StringBuilder embedBuilder = new StringBuilder();
        embedBuilder.append("{")
                .append("\"embeds\": [{").append("\"title\": \"").append(plugin.getDiscordManager().getJoinTitle()).append("\",").append("\"description\": \"").append(escapeJson(description)).append("\",").append("\"color\": ").append(plugin.getDiscordManager().getJoinColor());

        if (plugin.getDiscordManager().isJoinAvatarEnabled()) {
            embedBuilder.append(",")
                    .append("\"thumbnail\": {").append("\"url\": \"").append(escapeJson(avatarUrl)).append("\"")
                    .append("}");
        }

        embedBuilder.append("}]")
                .append("}");

        return embedBuilder.toString();
    }

    /**
     * Send a player death message to Discord
     * @param username The name of the player who died
     */
    public void sendDeathMessage(String username) {
        // If DiscordSRV integration is enabled and hooked, use it
        if (plugin.getDiscordManager().isUseDiscordSRV() && 
            discordSRVHook != null && 
            discordSRVHook.isHooked()) {
            discordSRVHook.sendDeathMessage(username);
            return;
        }
        
        // Otherwise, use webhook
        if (plugin.getDiscordManager().isDeathEnabled()) {
            String webhookUrl = plugin.getDiscordManager().getDiscordHook();
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                plugin.getLogger().warning("Discord webhook URL is not set. Unable to send death message to Discord.");
                return;
            }
            
            HttpURLConnection connection = null;
            try {
                URL url = new URL(webhookUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                String jsonPayload = buildEmbedDeath(username);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_NO_CONTENT) {
                    plugin.getLogger().info("Failed to send message. HTTP error code: " + code);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    private @NotNull String buildEmbedDeath(String username) {
        String descriptionTemplate = plugin.getDiscordManager().getDeathDescription();
        String description = descriptionTemplate.replace("%player%", escapeJson(username));
        String avatarUrl = "https://mc-heads.net/avatar/" + username;

        StringBuilder embedBuilder = new StringBuilder();
        embedBuilder.append("{")
                .append("\"embeds\": [{").append("\"title\": \"").append(plugin.getDiscordManager().getDeathTitle()).append("\",").append("\"description\": \"").append(escapeJson(description)).append("\",").append("\"color\": ").append(plugin.getDiscordManager().getDeathColor());

        if (plugin.getDiscordManager().isDeathAvatarEnabled()) {
            embedBuilder.append(",")
                    .append("\"thumbnail\": {").append("\"url\": \"").append(escapeJson(avatarUrl)).append("\"")
                    .append("}");
        }

        embedBuilder.append("}]")
                .append("}");

        return embedBuilder.toString();
    }

    public void sendBannedWordEmbed(String playerName, String word, String message, String URL) {
        // Check if URL is provided and not empty
        if (URL == null || URL.isEmpty()) {
            plugin.getLogger().warning("Discord webhook URL is not set. Unable to send banned word message to Discord.");
            return;
        }
        
        HttpURLConnection connection = null;
        try {
            URL url = new URL(URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            String jsonPayload = buildEmbedBannedWord(playerName, word, message);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_NO_CONTENT) {
                plugin.getLogger().info("Failed to send message. HTTP error code: " + code);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private @NotNull String buildEmbedBannedWord(String playerName, String word, String message) {
        String title = plugin.getDiscordManager().getBannedWordsTitle();
        String descriptionTemplate = plugin.getDiscordManager().getBannedWordsDescription();
        int color = plugin.getDiscordManager().getBannedWordsColor();
        boolean avatarEnabled = plugin.getDiscordManager().isBannedWordsAvatar();

        String description = descriptionTemplate
                .replace("%player%", escapeJson(playerName))
                .replace("%word%", escapeJson(word))
                .replace("%message%", escapeJson(message));

        String avatarUrl = "https://mc-heads.net/avatar/" + playerName;

        StringBuilder embedBuilder = new StringBuilder();
        embedBuilder.append("{")
                .append("\"embeds\": [{")
                .append("\"title\": \"").append(escapeJson(title)).append("\",")
                .append("\"description\": \"").append(escapeJson(description)).append("\",")
                .append("\"color\": ").append(color);

        if (avatarEnabled) {
            embedBuilder.append(",")
                    .append("\"thumbnail\": {")
                    .append("\"url\": \"").append(escapeJson(avatarUrl)).append("\"")
                    .append("}");
        }

        embedBuilder.append("}]")
                .append("}");

        return embedBuilder.toString();
    }

    public void sendMuteEmbed(String playerName, String senderName, String URL) {
        // Check if URL is provided and not empty
        if (URL == null || URL.isEmpty()) {
            plugin.getLogger().warning("Discord webhook URL is not set. Unable to send mute message to Discord.");
            return;
        }
        
        HttpURLConnection connection = null;
        try {
            URL url = new URL(URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            String jsonPayload = buildEmbedMute(playerName, senderName);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_NO_CONTENT) {
                plugin.getLogger().info("Failed to send message. HTTP error code: " + code);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private @NotNull String buildEmbedMute(String playerName, @NotNull String senderName) {
        String title = plugin.getDiscordManager().getMuteTitle();
        String descriptionTemplate = plugin.getDiscordManager().getMuteDescription();
        int color = plugin.getDiscordManager().getMuteColor();
        boolean avatarEnabled = plugin.getDiscordManager().isMuteAvatar();

        String description = descriptionTemplate
                .replace("%player%", escapeJson(playerName))
                .replace("%admin%", escapeJson(senderName));

        String avatarUrl = "https://mc-heads.net/avatar/" + playerName;

        StringBuilder embedBuilder = new StringBuilder();
        embedBuilder.append("{")
                .append("\"embeds\": [{")
                .append("\"title\": \"").append(escapeJson(title)).append("\",")
                .append("\"description\": \"").append(escapeJson(description)).append("\",")
                .append("\"color\": ").append(color);

        if (avatarEnabled) {
            embedBuilder.append(",")
                    .append("\"thumbnail\": {")
                    .append("\"url\": \"").append(escapeJson(avatarUrl)).append("\"")
                    .append("}");
        }

        embedBuilder.append("}]")
                .append("}");

        return embedBuilder.toString();
    }

    public void sendBannedCommandEmbed(String playerName, String word, String message, String URL) {
        // Check if URL is provided and not empty
        if (URL == null || URL.isEmpty()) {
            plugin.getLogger().warning("Discord webhook URL is not set. Unable to send banned command message to Discord.");
            return;
        }
        
        HttpURLConnection connection = null;
        try {
            URL url = new URL(URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            String jsonPayload = buildEmbedBannedCommand(playerName, word, message);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_NO_CONTENT) {
                plugin.getLogger().info("Failed to send message. HTTP error code: " + code);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private @NotNull String buildEmbedBannedCommand(String playerName, String word, String message) {
        String title = plugin.getDiscordManager().getBannedCommandsTitle();
        String descriptionTemplate = plugin.getDiscordManager().getBannedCommandsDescription();
        int color = plugin.getDiscordManager().getBannedCommandsColor();
        boolean avatarEnabled = plugin.getDiscordManager().isBannedCommandsAvatar();

        String description = descriptionTemplate
                .replace("%player%", escapeJson(playerName))
                .replace("%word%", escapeJson(word))
                .replace("%message%", escapeJson(message));

        String avatarUrl = "https://mc-heads.net/avatar/" + playerName;

        StringBuilder embedBuilder = new StringBuilder();
        embedBuilder.append("{")
                .append("\"embeds\": [{")
                .append("\"title\": \"").append(escapeJson(title)).append("\",")
                .append("\"description\": \"").append(escapeJson(description)).append("\",")
                .append("\"color\": ").append(color);

        if (avatarEnabled) {
            embedBuilder.append(",")
                    .append("\"thumbnail\": {")
                    .append("\"url\": \"").append(escapeJson(avatarUrl)).append("\"")
                    .append("}");
        }

        embedBuilder.append("}]")
                .append("}");

        return embedBuilder.toString();
    }

    private @NotNull String escapeJson(@NotNull String message) {
        return message.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

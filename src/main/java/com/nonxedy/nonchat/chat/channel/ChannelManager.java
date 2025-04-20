package com.nonxedy.nonchat.chat.channel;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.nonxedy.nonchat.api.Channel;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.util.HoverTextUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * Manages all chat channels in the NonChat plugin.
 */
public class ChannelManager {
    private final Map<String, Channel> channels = new ConcurrentHashMap<>();
    private final Map<Player, Channel> playerChannels = new ConcurrentHashMap<>();
    private final Map<Player, Long> lastMessageTimes = new ConcurrentHashMap<>();
    private String defaultChannelId;
    private final PluginConfig config;

    public ChannelManager(PluginConfig config) {
        this.config = config;
        loadChannels();
    }

    /**
     * Loads all channels from configuration.
     */
    public void loadChannels() {
        // Clear existing channels
        channels.clear();
        
        // Get the channels section from config
        ConfigurationSection channelsSection = config.getConfigurationSection("channels");
        if (channelsSection == null) {
            // Create default channels if none exist
            createDefaultChannels();
            return;
        }
        
        // Get default channel id
        this.defaultChannelId = config.getString("default-channel", "global");
        
        HoverTextUtil hoverTextUtil = config.getHoverTextUtil();
        
        // Load each channel
        for (String channelId : channelsSection.getKeys(false)) {
            ConfigurationSection channelSection = channelsSection.getConfigurationSection(channelId);
            if (channelSection == null) continue;
            
            boolean enabled = channelSection.getBoolean("enabled", true);
            String displayName = channelSection.getString("display-name", channelId);
            String format = channelSection.getString("format", "{prefix}{sender}{suffix}: {message}");
            String charStr = channelSection.getString("character", "");
            char character = charStr.isEmpty() ? '\0' : charStr.charAt(0);
            String sendPermission = channelSection.getString("send-permission", "");
            String receivePermission = channelSection.getString("receive-permission", "");
            int radius = channelSection.getInt("radius", -1);
            String discordChannelId = channelSection.getString("discord-channel", "");
            int cooldown = channelSection.getInt("cooldown", 0);
            int minLength = channelSection.getInt("min-length", 0);
            int maxLength = channelSection.getInt("max-length", 256);
            
            // Create and register channel
            Channel channel = new BaseChannel(
                channelId, displayName, format, character, sendPermission, receivePermission,
                radius, enabled, discordChannelId, hoverTextUtil, cooldown, minLength, maxLength
            );
            
            if (config.isDebug()) {
                Bukkit.getLogger().info("Loaded channel: " + channelId + ", max-length: " + maxLength);
            }
            
            channels.put(channelId, channel);
        }
        
        // If no channels were loaded, create default ones
        if (channels.isEmpty()) {
            createDefaultChannels();
        }
    }
    
    /**
     * Creates default channels if none are configured.
     */
    private void createDefaultChannels() {
        HoverTextUtil hoverTextUtil = config.getHoverTextUtil();
        
        // Create global channel
        Channel globalChannel = new BaseChannel(
            "global", "Global", "§7(§6G§7)§r {prefix} §f{sender}§r {suffix}§7: §f{message}",
            '!', "", "", -1, true, "", hoverTextUtil, 0, 0, 256
        );
        channels.put("global", globalChannel);
        
        // Create local channel
        Channel localChannel = new BaseChannel(
            "local", "Local", "§7(§6L§7)§r {prefix} §f{sender}§r {suffix}§7: §f{message}",
            '\0', "", "", 100, true, "", hoverTextUtil, 0, 0, 256
        );
        channels.put("local", localChannel);
        
        // Set default channel
        this.defaultChannelId = "local";
        
        // Save default channels to config
        saveChannelToConfig("global", globalChannel);
        saveChannelToConfig("local", localChannel);
        config.set("default-channel", defaultChannelId);
        config.saveConfig();
    }
    
    /**
     * Creates a new channel with the specified properties.
     * @param channelId The unique channel ID (must be lowercase, letters, numbers, and hyphens only)
     * @param displayName The display name for the channel
     * @param format The message format for the channel
     * @param character The trigger character, or null for none
     * @param sendPermission Permission to send to this channel, or empty for everyone
     * @param receivePermission Permission to receive from this channel, or empty for everyone
     * @param radius Radius of the channel in blocks, or -1 for global
     * @param cooldown Cooldown between messages in seconds
     * @param minLength Minimum message length
     * @param maxLength Maximum message length, or -1 for unlimited
     * @return The created channel, or null if the ID already exists
     */
    public Channel createChannel(String channelId, String displayName, String format,
                                Character character, String sendPermission, String receivePermission,
                                int radius, int cooldown, int minLength, int maxLength) {
        // Check if channel already exists
        if (channels.containsKey(channelId)) {
            return null;
        }
        
        // Sanitize inputs
        channelId = channelId.toLowerCase();
        if (!channelId.matches("^[a-z0-9-]+$")) {
            return null; // Invalid channel ID
        }
        
        // Create the channel
        Channel channel = new BaseChannel(
            channelId, 
            displayName,
            format, 
            character != null ? character : '\0',
            sendPermission,
            receivePermission,
            radius,
            true, // Enabled by default
            "", // No discord integration
            config.getHoverTextUtil(),
            cooldown,
            minLength,
            maxLength
        );
        
        // Add to channels map
        channels.put(channelId, channel);
        
        // Save to config
        saveChannelToConfig(channelId, channel);
        config.saveConfig();
        
        return channel;
    }
    
    /**
     * Updates an existing channel with new properties.
     * @param channelId The channel ID to update
     * @param displayName The display name for the channel (null to keep existing)
     * @param format The message format for the channel (null to keep existing)
     * @param character The trigger character (null to keep existing, '\0' to remove)
     * @param sendPermission Permission to send to this channel (null to keep existing)
     * @param receivePermission Permission to receive from this channel (null to keep existing)
     * @param radius Radius of the channel in blocks (-1 for global, null to keep existing)
     * @param enabled Whether the channel is enabled (null to keep existing)
     * @param cooldown Cooldown between messages in seconds (null to keep existing)
     * @param minLength Minimum message length (null to keep existing)
     * @param maxLength Maximum message length (null to keep existing)
     * @return True if the channel was updated, false otherwise
     */
    public boolean updateChannel(String channelId, String displayName, String format,
                                Character character, String sendPermission, String receivePermission,
                                Integer radius, Boolean enabled, Integer cooldown, 
                                Integer minLength, Integer maxLength) {
        // Get existing channel
        Channel existingChannel = getChannel(channelId);
        if (existingChannel == null) {
            return false;
        }
        
        // Since we can't modify the existing channel directly (it's immutable), 
        // we create a new one with updated properties
        
        Channel updatedChannel = new BaseChannel(
            channelId,
            displayName != null ? displayName : existingChannel.getDisplayName(),
            format != null ? format : existingChannel.getFormat(),
            character != null ? character : existingChannel.getCharacter(),
            sendPermission != null ? sendPermission : existingChannel.getSendPermission(),
            receivePermission != null ? receivePermission : existingChannel.getReceivePermission(),
            radius != null ? radius : existingChannel.getRadius(),
            enabled != null ? enabled : existingChannel.isEnabled(),
            "", // No discord integration
            config.getHoverTextUtil(),
            cooldown != null ? cooldown : existingChannel.getCooldown(),
            minLength != null ? minLength : existingChannel.getMinLength(),
            maxLength != null ? maxLength : existingChannel.getMaxLength()
        );
        
        // Replace in channels map
        channels.put(channelId, updatedChannel);
        
        // Save to config
        saveChannelToConfig(channelId, updatedChannel);
        config.saveConfig();
        
        return true;
    }
    
    /**
     * Deletes a channel.
     * @param channelId The channel ID to delete
     * @return True if the channel was deleted, false otherwise
     */
    public boolean deleteChannel(String channelId) {
        // Can't delete if it's the default channel
        if (channelId.equals(defaultChannelId)) {
            return false;
        }
        
        // Check if channel exists
        if (!channels.containsKey(channelId)) {
            return false;
        }
        
        // Remove from channels map
        channels.remove(channelId);
        
        // Remove from config
        config.set("channels." + channelId, null);
        config.saveConfig();
        
        // Switch any players using this channel to the default
        Channel defaultChannel = getDefaultChannel();
        for (Player player : playerChannels.keySet()) {
            if (playerChannels.get(player).getId().equals(channelId)) {
                playerChannels.put(player, defaultChannel);
            }
        }
        
        return true;
    }
    
    /**
     * Sets a new default channel.
     * @param channelId The channel ID to set as default
     * @return True if successful, false if channel doesn't exist or isn't enabled
     */
    public boolean setDefaultChannel(String channelId) {
        Channel channel = getChannel(channelId);
        if (channel == null || !channel.isEnabled()) {
            return false;
        }
        
        this.defaultChannelId = channelId;
        config.set("default-channel", channelId);
        config.saveConfig();
        
        return true;
    }
    
    /**
     * Saves a channel's configuration to the config file.
     * @param channelId The channel ID
     * @param channel The channel to save
     */
    private void saveChannelToConfig(String channelId, Channel channel) {
        String basePath = "channels." + channelId + ".";
        config.set(basePath + "enabled", channel.isEnabled());
        config.set(basePath + "display-name", channel.getDisplayName());
        config.set(basePath + "format", channel.getFormat());
        config.set(basePath + "character", channel.hasTriggerCharacter() ? String.valueOf(channel.getCharacter()) : "");
        config.set(basePath + "send-permission", channel.getSendPermission());
        config.set(basePath + "receive-permission", channel.getReceivePermission());
        config.set(basePath + "radius", channel.getRadius());
        config.set(basePath + "discord-channel", channel.getDiscordChannelId());
        config.set(basePath + "cooldown", channel.getCooldown());
        config.set(basePath + "min-length", channel.getMinLength());
        config.set(basePath + "max-length", channel.getMaxLength());
    }

    /**
     * Gets a channel by its ID.
     * @param id The channel ID
     * @return The channel, or null if not found
     */
    @Nullable
    public Channel getChannel(String id) {
        return channels.get(id);
    }
    
    /**
     * Gets the default channel.
     * @return The default channel
     */
    @NotNull
    public Channel getDefaultChannel() {
        Channel defaultChannel = channels.get(defaultChannelId);
        if (defaultChannel == null) {
            // If default channel doesn't exist, return the first available channel
            Optional<Channel> any = channels.values().stream().findFirst();
            return any.orElseThrow(() -> new IllegalStateException("No channels available"));
        }
        return defaultChannel;
    }
    
    /**
     * Gets all channels.
     * @return Collection of all channels
     */
    @NotNull
    public Collection<Channel> getAllChannels() {
        return channels.values();
    }
    
    /**
     * Gets all enabled channels.
     * @return Collection of enabled channels
     */
    @NotNull
    public Collection<Channel> getEnabledChannels() {
        return channels.values().stream()
                .filter(Channel::isEnabled)
                .collect(Collectors.toList());
    }
    
    /**
     * Determines the channel for a message based on its prefix character.
     * @param message The message to check
     * @return The appropriate channel, or default if no match
     */
    @NotNull
    public Channel getChannelForMessage(String message) {
        if (message.isEmpty()) {
            return getDefaultChannel();
        }
        
        char firstChar = message.charAt(0);
        
        for (Channel channel : channels.values()) {
            if (channel.isEnabled() && channel.hasTriggerCharacter() && channel.getCharacter() == firstChar) {
                return channel;
            }
        }
        
        return getDefaultChannel();
    }
    
    /**
     * Sets a player's active channel.
     * @param player The player
     * @param channelId The channel ID
     * @return true if successful, false if channel not found or not enabled
     */
    public boolean setPlayerChannel(Player player, String channelId) {
        Channel channel = getChannel(channelId);
        
        if (channel != null && channel.isEnabled()) {
            playerChannels.put(player, channel);
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets a player's active channel.
     * @param player The player
     * @return The player's channel, or the default if none set
     */
    @NotNull
    public Channel getPlayerChannel(Player player) {
        return playerChannels.getOrDefault(player, getDefaultChannel());
    }
    
    /**
     * Removes a player's channel setting.
     * @param player The player to remove
     */
    public void removePlayerChannel(Player player) {
        playerChannels.remove(player);
    }
    
    /**
     * Records when a player sends a message for cooldown tracking.
     * @param player The player
     */
    public void recordMessageSent(Player player) {
        lastMessageTimes.put(player, System.currentTimeMillis());
    }
    
    /**
     * Checks if a player can send a message based on cooldown.
     * @param player The player to check
     * @param channel The channel to check
     * @return true if player can send a message, false if on cooldown
     */
    public boolean canSendMessage(Player player, Channel channel) {
        if (channel.getCooldown() <= 0 || player.hasPermission("nonchat.bypass.cooldown")) {
            return true;
        }
        
        Long lastMessageTime = lastMessageTimes.get(player);
        if (lastMessageTime == null) {
            return true;
        }
        
        long cooldownMillis = channel.getCooldown() * 1000L;
        long timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime;
        
        return timeSinceLastMessage >= cooldownMillis;
    }
    
    /**
     * Gets the remaining cooldown time for a player in a channel.
     * @param player The player
     * @param channel The channel
     * @return Remaining cooldown in seconds, 0 if no cooldown
     */
    public int getRemainingCooldown(Player player, Channel channel) {
        if (channel.getCooldown() <= 0 || player.hasPermission("nonchat.bypass.cooldown")) {
            return 0;
        }
        
        Long lastMessageTime = lastMessageTimes.get(player);
        if (lastMessageTime == null) {
            return 0;
        }
        
        long cooldownMillis = channel.getCooldown() * 1000L;
        long timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime;
        
        if (timeSinceLastMessage >= cooldownMillis) {
            return 0;
        }
        
        return (int) ((cooldownMillis - timeSinceLastMessage) / 1000);
    }
}

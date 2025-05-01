package com.nonxedy.nonchat.chat.channel;

import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nonxedy.nonchat.api.Channel;
import com.nonxedy.nonchat.util.ColorUtil;
import com.nonxedy.nonchat.util.HoverTextUtil;
import com.nonxedy.nonchat.util.LinkDetector;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

/**
 * Base implementation of the Channel interface with common functionality.
 */
public class BaseChannel implements Channel {
    private final String id;
    private final String displayName;
    private final String format;
    private final char character;
    private final String sendPermission;
    private final String receivePermission;
    private final int radius;
    private final String discordChannelId;
    private final String discordWebhook;
    private final int cooldown;
    private final int minLength;
    private final int maxLength;
    private final HoverTextUtil hoverTextUtil;
    private boolean enabled;
    private static final Pattern mentionPattern = Pattern.compile("@(\\w+)");

    /**
     * Creates a new BaseChannel with all properties.
     */
    public BaseChannel(String id, String displayName, String format, char character,
                       String sendPermission, String receivePermission, int radius,
                       boolean enabled, String discordChannelId, String discordWebhook,
                       HoverTextUtil hoverTextUtil, int cooldown, int minLength, int maxLength) {
        this.id = id;
        this.displayName = displayName;
        this.format = format;
        this.character = character;
        this.sendPermission = sendPermission;
        this.receivePermission = receivePermission;
        this.radius = radius;
        this.enabled = enabled;
        this.discordChannelId = discordChannelId;
        this.discordWebhook = discordWebhook;
        this.hoverTextUtil = hoverTextUtil;
        this.cooldown = cooldown;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public char getCharacter() {
        return character;
    }
    
    @Override
    public boolean hasTriggerCharacter() {
        return character != '\0';
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getSendPermission() {
        return sendPermission;
    }

    @Override
    public String getReceivePermission() {
        return receivePermission;
    }

    @Override
    public int getRadius() {
        return radius;
    }

    @Override
    public boolean isGlobal() {
        return radius == -1;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getDiscordChannelId() {
        return discordChannelId;
    }
    
    @Override
    public String getDiscordWebhook() {
        return discordWebhook;
    }
    
    @Override
    public int getCooldown() {
        return cooldown;
    }
    
    @Override
    public int getMinLength() {
        return minLength;
    }
    
    @Override
    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public boolean canSend(Player player) {
        return sendPermission == null || sendPermission.isEmpty() || 
               player.hasPermission(sendPermission);
    }

    @Override
    public boolean canReceive(Player player) {
        return receivePermission == null || receivePermission.isEmpty() || 
               player.hasPermission(receivePermission);
    }

    @Override
    public boolean isInRange(Player sender, Player recipient) {
        if (isGlobal()) {
            return true;
        }
        
        // Make sure they're in the same world
        if (sender.getWorld() != recipient.getWorld()) {
            return false;
        }
        
        // Check distance
        return sender.getLocation().distance(recipient.getLocation()) <= radius;
    }

    @Override
    public Component formatMessage(Player player, String message) {
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        String prefix = user.getCachedData().getMetaData().getPrefix();
        String suffix = user.getCachedData().getMetaData().getSuffix();

        prefix = prefix == null ? "" : ColorUtil.parseColor(prefix);
        suffix = suffix == null ? "" : ColorUtil.parseColor(suffix);

        String baseFormat = getFormat();
        
        // Apply PlaceholderAPI if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                baseFormat = PlaceholderAPI.setPlaceholders(player, baseFormat);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Error processing format placeholders: " + e.getMessage());
            }
        }

        // Replace basic placeholders
        baseFormat = baseFormat
            .replace("{prefix}", prefix)
            .replace("{suffix}", suffix)
            .replace("{channel}", getDisplayName());

        // Split format into parts
        String[] formatParts = baseFormat.split("\\{message\\}");
        String beforeMessage = formatParts[0];
        String afterMessage = formatParts.length > 1 ? formatParts[1] : "";

        String[] beforeParts = beforeMessage.split("\\{sender\\}");
    
        // Build component with hoverable player name
        Component finalMessage = ColorUtil.parseComponent(beforeParts[0])
            .append(hoverTextUtil.createHoverablePlayerName(player, player.getName()));

        if (beforeParts.length > 1) {
            finalMessage = finalMessage.append(ColorUtil.parseComponent(beforeParts[1]));
        }
    
        // Add message with clickable links
        finalMessage = finalMessage.append(LinkDetector.makeLinksClickable(message));
    
        // Add after message part if it exists
        if (!afterMessage.isEmpty()) {
            finalMessage = finalMessage.append(ColorUtil.parseComponent(afterMessage));
        }

        return finalMessage;
    }
}

package com.nonxedy.nonchat.chat.channel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nonxedy.nonchat.api.Channel;
import com.nonxedy.nonchat.command.impl.IgnoreCommand;
import com.nonxedy.nonchat.util.ColorUtil;
import com.nonxedy.nonchat.util.HoverTextUtil;
import com.nonxedy.nonchat.util.ItemDetector;
import com.nonxedy.nonchat.util.ItemDisplayUtil;
import com.nonxedy.nonchat.util.LinkDetector;
import com.nonxedy.nonchat.util.PingDetector;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private final int cooldown;
    private final int minLength;
    private final int maxLength;
    private final HoverTextUtil hoverTextUtil;
    private boolean enabled;
    private static final Pattern mentionPattern = Pattern.compile("@(\\w+)");
    private IgnoreCommand ignoreCommand;

    /**
     * Creates a new BaseChannel with all properties.
     */
    public BaseChannel(String id, String displayName, String format, char character,
                       String sendPermission, String receivePermission, int radius,
                       boolean enabled, HoverTextUtil hoverTextUtil, int cooldown,
                       int minLength, int maxLength) {
        this.id = id;
        this.displayName = displayName;
        this.format = format;
        this.character = character;
        this.sendPermission = sendPermission;
        this.receivePermission = receivePermission;
        this.radius = radius;
        this.enabled = enabled;
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
    
        // Process the message for placeholders
        Component messageComponent;
    
        // Check what placeholders we have
        boolean hasItem = message.toLowerCase().contains("[item]");
        boolean hasPing = message.toLowerCase().contains("[ping]");
    
        if (hasItem && hasPing) {
            // Process both placeholders
            messageComponent = processBothPlaceholders(player, message);
        } else if (hasItem) {
            messageComponent = ItemDetector.processItemPlaceholders(player, message);
        } else if (hasPing) {
            messageComponent = PingDetector.processPingPlaceholders(player, message);
        } else {
            messageComponent = LinkDetector.makeLinksClickable(message);
        }
    
        finalMessage = finalMessage.append(messageComponent);

        // Add after message part if it exists
        if (!afterMessage.isEmpty()) {
            finalMessage = finalMessage.append(ColorUtil.parseComponent(afterMessage));
        }

        return finalMessage;
    }

    /**
     * Processes both item and ping placeholders in a message
     */
    private Component processBothPlaceholders(Player player, String message) {
        // Use TextComponent.Builder instead of Component.Builder
        TextComponent.Builder builder = Component.text();
    
        // Split by [item] and [ping] and process each part
        String[] parts = message.split("(?i)\\[(item|ping)\\]");
        Pattern pattern = Pattern.compile("(?i)\\[(item|ping)\\]");
        Matcher matcher = pattern.matcher(message);
    
        int partIndex = 0;
    
        while (matcher.find()) {
            // Add text before placeholder
            if (partIndex < parts.length && !parts[partIndex].isEmpty()) {
                builder.append(LinkDetector.makeLinksClickable(parts[partIndex]));
            }
            partIndex++;
            
            String placeholder = matcher.group().toLowerCase();
            if (placeholder.equals("[item]")) {
                // Process item using the new bracketed method with client-side localization
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                Component itemComponent = ItemDisplayUtil.createBracketedItemComponent(heldItem);
                builder.append(itemComponent);
            } else if (placeholder.equals("[ping]")) {
                // Process ping
                int ping = player.getPing();
                NamedTextColor color;
                if (ping < 100) {
                    color = NamedTextColor.GREEN;
                } else if (ping < 300) {
                    color = NamedTextColor.GOLD;
                } else {
                    color = NamedTextColor.RED;
                }
                builder.append(Component.text(ping + "ms").color(color));
            }
        }
        
        // Add remaining text
        if (partIndex < parts.length && !parts[partIndex].isEmpty()) {
            builder.append(LinkDetector.makeLinksClickable(parts[partIndex]));
        }
        
        return builder.build();
    }
    
    /**
     * Sets the ignore command instance.
     * @param ignoreCommand The ignore command instance
     */
    public void setIgnoreCommand(IgnoreCommand ignoreCommand) {
        this.ignoreCommand = ignoreCommand;
    }
}

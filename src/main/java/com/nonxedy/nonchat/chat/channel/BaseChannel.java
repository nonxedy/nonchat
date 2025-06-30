package com.nonxedy.nonchat.chat.channel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.api.Channel;
import com.nonxedy.nonchat.command.impl.IgnoreCommand;
import com.nonxedy.nonchat.service.ConfigService;
import com.nonxedy.nonchat.util.chat.filters.LinkDetector;
import com.nonxedy.nonchat.util.chat.formatting.HoverTextUtil;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;
import com.nonxedy.nonchat.util.items.detection.ItemDetector;
import com.nonxedy.nonchat.util.items.display.ItemDisplayUtil;
import com.nonxedy.nonchat.util.special.ping.PingDetector;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

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
    private ConfigService configService;

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
        String baseFormat = getFormat();
        
        // Split format into parts around {message}
        String[] formatParts = baseFormat.split("\\{message\\}");
        String beforeMessage = formatParts[0];
        String afterMessage = formatParts.length > 1 ? formatParts[1] : "";

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                beforeMessage = PlaceholderAPI.setPlaceholders(player, beforeMessage);
                afterMessage = PlaceholderAPI.setPlaceholders(player, afterMessage);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Error processing format placeholders: " + e.getMessage());
            }
        }

        // Process color codes after placeholders are expanded
        // The format part (beforeMessage and afterMessage) should always be colored
        Component finalMessage = ColorUtil.parseComponent(beforeMessage)
            .append(processMessageContent(player, message))
            .append(ColorUtil.parseComponent(afterMessage));

        return finalMessage;
    }

    private Component processMessageContent(Player player, String message) {
        // Check if interactive placeholders are globally disabled
        Plugin plugin = Bukkit.getPluginManager().getPlugin("nonchat");
        if (plugin instanceof com.nonxedy.nonchat.Nonchat) {
            com.nonxedy.nonchat.Nonchat nonchatPlugin = (com.nonxedy.nonchat.Nonchat) plugin;
            boolean globalEnabled = nonchatPlugin.getConfig().getBoolean("interactive-placeholders.enabled", true);
            
            if (!globalEnabled) {
                return processMessageWithColorPermission(player, message);
            }
        }

        boolean hasItem = message.toLowerCase().contains("[item]");
        boolean hasPing = message.toLowerCase().contains("[ping]");

        if (hasItem && hasPing) {
            return processBothPlaceholders(player, message);
        } else if (hasItem) {
            return processItemPlaceholderWithColorPermission(player, message);
        } else if (hasPing) {
            return processPingPlaceholderWithColorPermission(player, message);
        } else {
            return processMessageWithColorPermission(player, message);
        }
    }

    /**
     * Processes message content with color permission check
     */
    private Component processMessageWithColorPermission(Player player, String message) {
        // First make links clickable, then apply color permission
        Component linkProcessed = LinkDetector.makeLinksClickable(message);
        
        // If player doesn't have color permission, strip colors from the message part
        if (!player.hasPermission("nonchat.color")) {
            String strippedMessage = ColorUtil.stripAllColors(message);
            return LinkDetector.makeLinksClickable(strippedMessage);
        }
        
        return linkProcessed;
    }

    /**
     * Processes item placeholder with color permission check
     */
    private Component processItemPlaceholderWithColorPermission(Player player, String message) {
        // Check if item placeholders are enabled
        Plugin plugin = Bukkit.getPluginManager().getPlugin("nonchat");
        boolean itemEnabled = true;
        
        if (plugin instanceof Nonchat) {
            Nonchat nonchatPlugin = (Nonchat) plugin;
            itemEnabled = nonchatPlugin.getConfig().getBoolean("interactive-placeholders.item-enabled", true);
        }
        
        if (!itemEnabled) {
            return processMessageWithColorPermission(player, message);
        }
        
        // Process item placeholder but respect color permissions for the rest of the message
        if (!player.hasPermission("nonchat.color")) {
            // Strip colors from message but keep item placeholder functionality
            String messageWithoutColors = ColorUtil.stripAllColors(message);
            return ItemDetector.processItemPlaceholders(player, messageWithoutColors);
        }
        
        return ItemDetector.processItemPlaceholders(player, message);
    }

    /**
     * Processes ping placeholder with color permission check
     */
    private Component processPingPlaceholderWithColorPermission(Player player, String message) {
        // Check if ping placeholders are enabled
        Plugin plugin = Bukkit.getPluginManager().getPlugin("nonchat");
        boolean pingEnabled = true;
        
        if (plugin instanceof Nonchat) {
            Nonchat nonchatPlugin = (Nonchat) plugin;
            pingEnabled = nonchatPlugin.getConfig().getBoolean("interactive-placeholders.ping-enabled", true);
        }
        
        if (!pingEnabled) {
            return processMessageWithColorPermission(player, message);
        }
        
        // Process ping placeholder but respect color permissions for the rest of the message
        if (!player.hasPermission("nonchat.color")) {
            // Strip colors from message but keep ping placeholder functionality
            String messageWithoutColors = ColorUtil.stripAllColors(message);
            return PingDetector.processPingPlaceholders(player, messageWithoutColors);
        }
        
        return PingDetector.processPingPlaceholders(player, message);
    }

    /**
     * Processes both item and ping placeholders with color permission check
     */
    private Component processBothPlaceholders(Player player, String message) {
        // Check settings
        Plugin plugin = Bukkit.getPluginManager().getPlugin("nonchat");
        boolean itemEnabled = true;
        boolean pingEnabled = true;
        
        if (plugin instanceof Nonchat) {
            Nonchat nonchatPlugin = (Nonchat) plugin;
            itemEnabled = nonchatPlugin.getConfig().getBoolean("interactive-placeholders.item-enabled", true);
            pingEnabled = nonchatPlugin.getConfig().getBoolean("interactive-placeholders.ping-enabled", true);
        }
        
        // Check color permission
        boolean hasColorPermission = player.hasPermission("nonchat.color");
        String processedMessage = hasColorPermission ? message : ColorUtil.stripAllColors(message);
        
        // Use TextComponent.Builder instead of Component.Builder
        TextComponent.Builder builder = Component.text();
    
        // Split by [item] and [ping] and process each part
        String[] parts = processedMessage.split("(?i)\\[(item|ping)\\]");
        Pattern pattern = Pattern.compile("(?i)\\[(item|ping)\\]");
        Matcher matcher = pattern.matcher(processedMessage);
    
        int partIndex = 0;
    
        while (matcher.find()) {
            // Add text before placeholder
            if (partIndex < parts.length && !parts[partIndex].isEmpty()) {
                builder.append(LinkDetector.makeLinksClickable(parts[partIndex]));
            }
            partIndex++;
            
            String placeholder = matcher.group().toLowerCase();
            if (placeholder.equals("[item]") && itemEnabled) {
                // Process item using the new bracketed method with client-side localization
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                Component itemComponent = ItemDisplayUtil.createBracketedItemComponent(heldItem);
                builder.append(itemComponent);
            } else if (placeholder.equals("[ping]") && pingEnabled) {
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
            } else {
                // If placeholder is disabled, add it as plain text
                builder.append(Component.text(matcher.group()));
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

    /**
     * Sets the config service instance.
     * @param configService The config service instance
     */
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }
}

package com.nonxedy.nonchat.placeholders.impl;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.api.InteractivePlaceholder;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * A configurable interactive placeholder that reads its settings from config.yml
 * Supports hover text, click actions, and permissions
 */
public class ConfigurablePlaceholder implements InteractivePlaceholder {

    private final String placeholder;
    private final String activationKey;
    private final String displayName;
    private final String description;
    private boolean enabled;
    private String permission;
    private String format;
    private List<String> hoverText;
    private ClickAction clickAction;

    public ConfigurablePlaceholder(String placeholder, String activationKey, String displayName, String description,
                                 boolean enabled, String permission, String format, List<String> hoverText,
                                 String clickActionType, String clickActionValue) {
        this.placeholder = placeholder;
        this.activationKey = activationKey != null && !activationKey.isEmpty() ? activationKey : placeholder;
        this.displayName = displayName;
        this.description = description;
        this.enabled = enabled;
        this.permission = permission != null ? permission : "";
        this.format = format != null && !format.isEmpty() ? format : "[" + placeholder + "]";
        this.hoverText = hoverText;

        if (clickActionType != null && clickActionValue != null && !clickActionType.equals("none")) {
            this.clickAction = new ClickAction(clickActionType, clickActionValue);
        }
    }

    @Override
    @NotNull
    public String getPlaceholder() {
        return activationKey;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @Override
    @NotNull
    public String getDescription() {
        return description;
    }

    @Override
    @NotNull
    public Component process(Player player, String... arguments) {
        // Special handling for built-in placeholders
        if (placeholder.equals("item")) {
            return processItemPlaceholder(player);
        } else if (placeholder.equals("ping")) {
            return processPingPlaceholder(player);
        }

        // Default behavior for custom placeholders
        // Create the base component using custom format
        Component component = ColorUtil.parseComponent(format);

        // Add hover text if configured
        if (hoverText != null && !hoverText.isEmpty()) {
            Component hoverComponent = createHoverComponent(player);
            component = component.hoverEvent(HoverEvent.showText(hoverComponent));
        }

        // Add click action if configured
        if (clickAction != null) {
            ClickEvent clickEvent = createClickEvent(player);
            if (clickEvent != null) {
                component = component.clickEvent(clickEvent);
            }
        }

        return component;
    }

    private Component processItemPlaceholder(Player player) {
        // Process the format string with placeholders (get plain text)
        String processedFormat = processPlaceholdersAsString(player, format);

        // Create the main item component using custom format
        Component itemComponent = ColorUtil.parseComponent(processedFormat);

        // Add hover text from config if configured
        if (hoverText != null && !hoverText.isEmpty()) {
            Component hoverComponent = createHoverComponent(player);
            itemComponent = itemComponent.hoverEvent(HoverEvent.showText(hoverComponent));
        }

        // Add click action if configured
        if (clickAction != null) {
            ClickEvent clickEvent = createClickEvent(player);
            if (clickEvent != null) {
                itemComponent = itemComponent.clickEvent(clickEvent);
            }
        }

        return itemComponent;
    }

    private Component processPingPlaceholder(Player player) {
        // Process the format string with placeholders (get plain text)
        String processedFormat = processPlaceholdersAsString(player, format);

        // Create the main ping component using custom format
        Component pingComponent = ColorUtil.parseComponent(processedFormat);

        // Add custom hover text if configured
        if (hoverText != null && !hoverText.isEmpty()) {
            Component hoverComponent = createHoverComponent(player);
            pingComponent = pingComponent.hoverEvent(HoverEvent.showText(hoverComponent));
        }

        // Add click action if configured
        if (clickAction != null) {
            ClickEvent clickEvent = createClickEvent(player);
            if (clickEvent != null) {
                pingComponent = pingComponent.clickEvent(clickEvent);
            }
        }

        return pingComponent;
    }

    private Component createHoverComponent(Player player) {
        TextComponent.Builder builder = Component.text();

        for (String line : hoverText) {
            if (builder.children().isEmpty()) {
                // First line
                Component processedLine = processPlaceholders(player, line);
                builder.append(processedLine);
            } else {
                // Subsequent lines
                builder.append(Component.newline());
                Component processedLine = processPlaceholders(player, line);
                builder.append(processedLine);
            }
        }

        return builder.build();
    }

    private Component processPlaceholders(Player player, String text) {
        String processed = text;

        // Replace basic placeholders
        processed = processed.replace("{player}", player.getName());
        processed = processed.replace("{world}", player.getWorld().getName());
        processed = processed.replace("{x}", String.valueOf(player.getLocation().getBlockX()));
        processed = processed.replace("{y}", String.valueOf(player.getLocation().getBlockY()));
        processed = processed.replace("{z}", String.valueOf(player.getLocation().getBlockZ()));

        // Replace server placeholders
        processed = processed.replace("{server_name}", Bukkit.getServer().getName());
        processed = processed.replace("{online_players}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        processed = processed.replace("{max_players}", String.valueOf(Bukkit.getMaxPlayers()));
        processed = processed.replace("{server_version}", Bukkit.getServer().getVersion());

        // Special placeholders for item and ping
        if (placeholder.equals("item")) {
            org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
            processed = processItemPlaceholders(processed, item);
        } else if (placeholder.equals("ping")) {
            int ping = player.getPing();
            processed = processed.replace("{ping}", String.valueOf(ping));
            String quality = ping < 100 ? "Excellent" : ping < 300 ? "Good" : "Poor";
            processed = processed.replace("{ping_quality}", quality);
        }

        // Process PlaceholderAPI placeholders if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                processed = PlaceholderAPI.setPlaceholders(player, processed);
            } catch (Exception e) {
                // Ignore PlaceholderAPI errors
            }
        }

        // Convert color codes
        return ColorUtil.parseComponent(processed);
    }

    private String processItemPlaceholders(String text, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            text = text.replace("{item_name}", "Air");
            text = text.replace("{item_amount}", "0");
            text = text.replace("{item_lore}", "");
            text = text.replace("{item_enchantments}", "");
            text = text.replace("{item_durability}", "");
            return text;
        }

        // Basic item info - use Adventure API's displayName() instead of deprecated getDisplayName()
        String itemName;
        if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            // Use Adventure API to get display name as plain text
            itemName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(item.getItemMeta().displayName());
        } else {
            itemName = item.getType().name().replace("_", " ").toLowerCase();
        }

        text = text.replace("{item_name}", itemName);
        text = text.replace("{item_amount}", String.valueOf(item.getAmount()));

        // Lore - use Adventure API's lore() instead of deprecated getLore()
        if (item.getItemMeta() != null && item.getItemMeta().hasLore()) {
            StringBuilder loreBuilder = new StringBuilder();
            List<Component> loreComponents = item.getItemMeta().lore();
            if (loreComponents != null) {
                for (Component loreLine : loreComponents) {
                    if (loreBuilder.length() > 0) loreBuilder.append("\n");
                    // Convert Component to plain text
                    String plainLore = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(loreLine);
                    loreBuilder.append(plainLore);
                }
            }
            text = text.replace("{item_lore}", loreBuilder.toString());
        } else {
            text = text.replace("{item_lore}", "");
        }

        // Enchantments
        if (item.getItemMeta() != null && item.getItemMeta().hasEnchants()) {
            StringBuilder enchantBuilder = new StringBuilder();
            for (Enchantment enchantment : item.getItemMeta().getEnchants().keySet()) {
                if (enchantBuilder.length() > 0) enchantBuilder.append(", ");
                int level = item.getItemMeta().getEnchantLevel(enchantment);
                String enchantName = enchantment.getKey().getKey().replace("_", " ").toLowerCase();
                enchantBuilder.append(enchantName).append(" ").append(level);
            }
            text = text.replace("{item_enchantments}", enchantBuilder.toString());
        } else {
            text = text.replace("{item_enchantments}", "");
        }

        // Durability - use Damageable interface instead of deprecated getDurability()
        if (item.getType().getMaxDurability() > 0 && item.getItemMeta() instanceof Damageable) {
            Damageable damageable = (Damageable) item.getItemMeta();
            int maxDurability = item.getType().getMaxDurability();
            int damage = damageable.hasDamage() ? damageable.getDamage() : 0;
            int currentDurability = maxDurability - damage;
            double percentage = (double) currentDurability / maxDurability * 100;
            String durabilityText = currentDurability + "/" + maxDurability + " (" + String.format("%.1f", percentage) + "%)";
            text = text.replace("{item_durability}", durabilityText);
        } else {
            text = text.replace("{item_durability}", "");
        }

        return text;
    }

    private String processPlaceholdersAsString(Player player, String text) {
        String processed = text;

        // Replace basic placeholders
        processed = processed.replace("{player}", player.getName());
        processed = processed.replace("{world}", player.getWorld().getName());
        processed = processed.replace("{x}", String.valueOf(player.getLocation().getBlockX()));
        processed = processed.replace("{y}", String.valueOf(player.getLocation().getBlockY()));
        processed = processed.replace("{z}", String.valueOf(player.getLocation().getBlockZ()));

        // Replace server placeholders
        processed = processed.replace("{server_name}", Bukkit.getServer().getName());
        processed = processed.replace("{online_players}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        processed = processed.replace("{max_players}", String.valueOf(Bukkit.getMaxPlayers()));
        processed = processed.replace("{server_version}", Bukkit.getServer().getVersion());

        // Special placeholders for item and ping
        if (placeholder.equals("item")) {
            org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
            processed = processItemPlaceholders(processed, item);
        } else if (placeholder.equals("ping")) {
            int ping = player.getPing();
            processed = processed.replace("{ping}", String.valueOf(ping));
            String quality = ping < 100 ? "Excellent" : ping < 300 ? "Good" : "Poor";
            processed = processed.replace("{ping_quality}", quality);
        }

        // Process PlaceholderAPI placeholders if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                processed = PlaceholderAPI.setPlaceholders(player, processed);
            } catch (Exception e) {
                // Ignore PlaceholderAPI errors
            }
        }

        // Return processed string (don't convert to Component)
        return processed;
    }

    private ClickEvent createClickEvent(Player player) {
        String value = clickAction.value;

        // Process placeholders in the click action value
        value = processPlaceholdersAsString(player, value);

        return switch (clickAction.type.toLowerCase()) {
            case "command" -> ClickEvent.runCommand(value);
            case "suggest_command" -> ClickEvent.suggestCommand(value);
            case "open_url" -> ClickEvent.openUrl(value);
            default -> null;
        };
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getPermission() {
        return permission;
    }

    @Override
    public void setPermission(String permission) {
        this.permission = permission != null ? permission : "";
    }

    /**
     * Simple data class for click actions
     */
    private static class ClickAction {
        final String type;
        final String value;

        ClickAction(String type, String value) {
            this.type = type;
            this.value = value;
        }
    }

// ⡿⠿⠿⣿⠿⠿⣿⡿⠿⠿⠿⠿⠿⠿⠿⠿⡿⠿⢿⠿⠿⢿⡿⠿⠿⢿⠿⢿⠿⢿⠿⠿⢿⠿⠿⣿⣿⣿⠿⠿⠿⢿⠿⠿⢿⣿⡿⠿⠿⡿⠿⠿⠿⠿⡿⠿⠿⡿⠿⡿⠿⠿⠿⡿⠿⠿⠿⠿⡿⠿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
// ⣀⡀⠀⡿⠀⠀⢸⡇⠀⠀⠘⠀⠀⠀⠀⠀⠁⠀⢸⠀⠀⢸⡇⠀⠀⢸⠀⠀⠀⠀⠀⡆⠀⠀⠀⣿⣿⣿⠀⠀⠀⢸⠀⠀⢸⣿⠁⢰⠀⢰⠀⢠⡆⠀⡇⠀⠀⠁⠀⡇⠀⡄⠀⡇⠀⠀⢸⠀⠃⢠⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
// ⠿⢄⠀⠃⠀⠀⢸⠇⠀⠀⠀⡇⠀⢀⠀⠀⠀⠀⠸⠀⠄⠈⡇⠀⠀⢸⠀⠀⠀⠀⠀⡷⠶⠀⠀⠀⢸⣿⠀⠀⠀⠀⠀⠄⠸⣿⠀⢸⠶⢆⠀⢀⡇⠀⠀⠸⡄⠀⢸⡇⠀⡇⠀⡇⠀⠀⢸⠀⠀⢸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
// ⡀⠘⠀⡄⠀⡄⠘⠀⠐⠀⠀⠇⠀⢸⠀⢀⠀⠀⠀⠀⡄⠀⠃⠀⠀⢸⠀⢠⠀⢀⠀⠃⢀⠀⠀⠀⢸⣿⠀⠀⠀⠀⠀⡄⠀⣿⡀⠘⠀⡘⠀⠘⠃⠀⡇⠀⠇⠀⢸⠀⠀⠃⠀⡇⠀⠀⢸⠇⠀⡘⠛⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
// ⣿⣶⣿⣷⣾⣷⣆⣀⣶⣶⣀⣷⣶⣿⣷⣾⣾⣷⣶⣶⣷⣶⣶⣿⣷⣾⣶⣾⣶⣿⣷⣶⣿⣷⣶⣶⣿⣿⣶⣾⣶⣷⣶⣷⣶⣿⣿⣶⣾⣿⣶⣶⣷⣶⣷⣶⣶⣾⣿⣀⣶⣶⣀⣷⣾⣶⣾⣶⣾⣯⣼⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠏
// ⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡏⠀
// ⠉⠉⠉⠉⠉⠉⠹⠋⠉⠙⠏⠉⠉⠉⡏⠉⠉⠟⠉⠙⢿⠋⠉⠙⣿⣿⠉⠉⠉⠙⠉⠉⠉⠉⠉⠉⠉⠉⢻⠉⠉⢿⡏⠉⠉⢹⡏⠉⠉⡏⠉⠉⠉⠉⢹⠉⢹⠉⡏⠉⣿⡏⠉⠉⠉⠉⠉⠉⠏⠉⠉⠉⠉⠉⠋⠉⠉⠹⡏⠉⠉⡏⠉⠀⠀
// ⠀⢸⠀⠀⠀⠀⠀⠀⢸⠀⠀⠀⠀⠀⡇⠀⠻⠀⢸⣀⡀⠀⢀⣀⣿⣿⠀⠀⠀⠀⠀⠘⡇⠀⣿⠀⠀⠀⠈⠀⠀⢸⡇⠀⠀⠈⠀⠀⠀⡇⠀⠀⠀⠀⠀⠀⢸⠀⠀⠀⣿⡇⠀⡇⠀⠀⠀⠀⠀⠀⠛⠀⠀⠀⠀⠀⠃⢀⠇⠀⠀⡇⠀⠂⠀
// ⠀⢸⠀⠀⠀⢰⣾⠀⠸⠀⠀⠀⠀⠀⡇⠀⣶⠀⢸⠉⠁⠀⠈⠉⣿⡿⠀⢘⠀⢀⠀⢰⡇⠀⣿⠀⠀⣶⡇⠀⠀⢸⠀⢀⠀⢸⠀⠘⠀⠁⠀⠀⠀⠀⠀⠀⢸⠀⠀⠀⣿⡇⠀⡇⠀⠀⠀⣶⡇⠀⣶⠀⠀⣶⡇⠀⡆⠀⠀⠐⠀⠃⠀⡆⠀
// ⣀⣸⣀⣀⣀⣸⣿⣄⣀⣠⣆⣀⣀⠀⢀⣀⣀⣆⣀⣠⣾⣄⣀⣠⣿⡇⢀⣀⡀⢘⣀⣀⣀⣀⣿⣀⣀⣿⣃⣀⣁⡀⠀⣀⣀⠀⣀⣸⣀⣀⣀⣀⠀⣀⣀⣀⣸⣀⣀⣀⣿⣇⣀⣃⣀⣀⣀⣿⣇⣀⣀⣀⣀⣿⣇⣀⣀⣠⣀⣸⡀⣀⣀⣇⣀
// ⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣶⣾⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⣾⣿⣷⣾⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⣶⣿⣿⣶⣿⣿⣿⣿⣿⣿⣶⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣱⣿⣿⣿⣿
// ⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
// ⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡿⠿⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
// ⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣶⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠉⠀⠀⠀⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
// ⣻⠿⡻⠿⠻⢟⣻⢿⠛⣿⣿⣟⢻⣏⡿⡿⣟⣿⠿⢿⣿⣻⣻⣿⣿⡧⡿⣿⣛⣻⡿⠟⢩⣾⠿⠿⠿⣟⣹⣿⠻⠿⠞⢼⢽⡋⠿⠿⠾⢻⠿⠿⣽⠻⡿⠿⣻⢟⣛⣟⣟⣷⠀⠀⠀⡆⠸⡿⢻⢿⡿⠋⠉⠀⠈⠻⣿⣿⡿⣫⠒⠛⠻⠿⠿
// ⠀⠁⠀⠀⠁⠀⠀⠁⢸⢣⡇⠈⠀⠀⠀⠀⠁⠈⠀⠀⠀⣠⠷⣄⢹⠃⣨⡼⠟⠉⢀⡴⠓⠁⠀⠀⠈⠁⠀⠀⠀⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠀⠀⠈⣄⣨⠾⠚⠋⢉⡉⠆⠀⢠⠀⠀⠷⡴⠋⠀⠀⡀⠀⠀⠀⠈⢳⡀⠀⠀⠀⠀⠀⠀
// ⠀⠀⠀⠀⠀⠀⠀⠀⡏⢸⡇⠀⠀⠀⠀⠀⠀⠀⠀⢀⡜⠁⠀⠈⣿⠋⠁⠀⠀⠴⢯⣄⣀⡀⠀⠀⠀⠀⠀⠀⠀⣀⣠⡶⠂⠀⠀⠀⠀⠀⠀⠀⢀⡠⠞⠉⠀⠀⠀⠒⠂⠀⠉⠀⠂⠓⢄⣀⣀⣀⣀⣀⠨⡴⢤⣀⠀⠀⠹⡄⠀⠀⠀⠀⠀
// ⠀⠀⠘⢦⠀⠀⠀⠀⣇⠈⡇⠀⠀⠀⠀⠀⠀⣀⣤⠏⠀⠀⠀⠀⠉⡆⠀⠀⠀⠀⠀⠀⠈⠉⠛⠒⠒⠒⠒⢚⣫⠝⠉⠀⠀⠀⠀⠀⠀⠀⠀⡠⠊⠀⠀⠀⠀⠀⠌⠀⠀⠀⠀⢀⡄⠀⠜⠋⡄⠈⠐⠨⢢⡈⠢⡈⠉⠒⠒⠘⠀⠀⠀⠀⠀
// ⠀⠀⠀⠀⠑⢄⡀⠀⠈⠳⠬⣦⣄⣀⡠⠖⠋⠁⠀⠀⠀⠀⠀⠀⠀⢸⡀⠀⠀⠀⠀⠀⠀⠈⢢⠘⢿⣟⠉⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⠞⠀⠀⠀⠀⠀⠀⡈⠀⠀⠀⢀⠔⡙⠀⠌⢠⠀⠉⠆⠀⠀⠀⠉⠀⠈⢆⠀⠀⠀⠀⠀⠀⠀⠀
// ⠀⠀⠀⠀⠀⠀⠉⠒⠤⠤⣤⢤⡼⠛⠀⠀⠀⠀⠀⠀⢠⠀⠀⠀⠀⠈⡎⢢⠀⠀⢠⠀⠀⠀⠀⠳⡈⢮⠳⡄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠎⠀⠀⠀⠀⠀⢰⠰⠀⠀⠠⡤⠆⠰⢃⠎⠀⡈⠀⢸⠈⡄⠀⢀⠀⠀⠀⠀⢣⡀⠀⠀⠀⠀⠀⠀
// ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⠋⠀⠀⠀⠀⠀⠀⠀⠀⡼⠀⠀⠀⠀⠀⣿⡀⠳⡒⠚⣖⠦⠄⠀⠀⠱⡌⢦⠈⢦⠀⠀⠀⠀⠀⠀⠀⠸⠀⠀⠀⠀⠀⠀⢸⡇⠀⢀⠊⠀⠠⡡⠁⠀⢀⠁⠀⠸⠀⠘⡐⡈⡀⠀⠀⠀⠀⢳⣄⠀⠀⠀⠀⠀
// ⠀⠀⠀⠀⠀⠀⠀⠀⠀⢰⠁⠀⠀⠀⠀⠀⠀⠀⠀⡰⠃⡆⢠⠀⠀⠀⢸⢇⠀⠘⡄⠸⡄⠀⠀⠀⣄⢱⠈⢇⠀⠱⡀⠀⠀⠀⠀⠀⡇⠀⠀⠀⠀⠀⠀⠘⠁⡐⠁⠀⢠⠏⠀⠀⠀⠌⠀⠀⠀⠀⠀⢡⠈⡇⠀⠀⠀⠀⠈⡎⢆⠀⠀⠀⠀
// ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠇⠀⠀⡔⠀⠀⠀⠀⣠⢶⠁⠀⡇⢸⡄⠀⠀⢸⢸⠀⠀⠘⣆⢳⠀⠀⠀⣿⢦⢧⠈⣆⠀⠁⠀⠀⠀⠀⢰⠀⡄⠀⠀⠀⠀⠀⠀⡗⠀⠀⠀⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡆⡇⠀⠀⠀⠀⠀⠸⠈⢆⠀⠀⠀
// ⠀⠀⠀⣀⣀⣀⣀⡀⢸⠀⠀⢰⠃⠀⠀⠠⠞⢡⠇⠀⠀⢳⢸⢳⠀⠀⢸⠘⣆⣀⣀⣘⣎⣆⠀⠀⣿⣌⣾⡆⠘⡄⠀⠀⠀⠀⠀⢸⠀⡇⠀⢰⠀⠀⠀⠀⣿⣶⣶⣶⣶⡶⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠰⡁⠀⠀⠀⠀⠀⠀⠇⠘⡄⠀⠀
// ⠀⠉⠁⠀⠀⠀⠀⠀⢹⠁⠀⡞⠀⠀⠀⠀⢀⡎⠀⠀⢀⣸⣾⠈⣇⠀⢸⠀⠡⣶⣶⣿⣿⣿⡄⢠⣿⣿⣾⣧⠀⠘⢆⠀⠀⠀⠀⢸⠀⡇⠀⢸⠀⠀⠀⠀⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢠⣈⠂⢀⠀⡇⠀⠀⠀⡀⠀⠀⢰⠀⠘⡀⠀
// ⠀⠀⠀⠀⠀⠀⠀⠀⠈⡆⠀⡇⠀⠀⠀⠀⣼⣠⠴⢚⣩⣴⣿⠀⠘⡄⠘⠀⠀⠈⠉⠛⠛⠻⣧⢸⣿⣿⣿⢿⣦⡀⠈⢦⠀⠀⠀⢸⢀⡇⠀⡁⡇⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠛⢿⣷⣦⡄⡇⠀⠀⠀⠇⠀⢇⠘⠀⠀⠀⠀
// ⠀⠀⠀⠀⠀⠀⠀⢀⠤⠃⢰⠇⠀⠀⠀⢰⣿⣷⣾⣿⣿⠿⠉⠀⠀⠘⡇⠀⠀⠀⠀⠀⠀⠀⠘⠏⢿⣿⣿⣇⠉⠙⠓⠤⢕⣄⠀⢸⢸⢰⠀⠣⣁⠀⠀⠀⡂⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠉⠛⠁⠃⠀⠀⢸⠀⢸⠈⡄⡆⠀⠀⠀
// ⠀⠀⠀⠀⠀⠀⠉⠓⠒⡶⢼⠀⢠⠀⢀⣿⣿⣿⣿⠟⠁⠀⠀⠀⠀⠀⠀⣀⠄⠂⠉⠐⡄⠀⠀⠀⢸⡇⠏⢿⣆⠀⠀⠀⠀⠀⠀⢸⠸⠘⡄⠀⠸⠀⠀⠀⡇⠀⠀⠀⠀⢀⠂⢱⠀⠀⠀⠀⠀⠀⠀⠀⣼⠀⠀⢀⡇⠀⡈⠀⢡⡇⠀⠀⠀
// ⠀⠀⠀⠀⠀⠀⠀⠀⠈⠀⢸⢠⢿⡀⡼⣿⣿⣿⣧⠀⠀⠀⠀⢀⠔⠂⠉⠀⠀⠀⠀⠀⡁⠀⠀⢀⣾⡇⢸⠀⠙⢆⠀⠀⠀⠀⠀⠈⡆⠀⢃⠀⠀⡇⠀⠀⣇⠀⠀⠀⠀⠀⠑⡎⠀⠀⠀⠀⠀⠀⠀⡸⡇⠀⠀⡜⡄⢀⠃⠀⠸⠁⠀⠀⠄
// ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡿⠀⡇⡇⢿⣿⣿⣿⡆⠀⠀⠀⡇⠀⠀⠀⠀⠀⢀⡀⢀⠁⣠⣴⣿⣿⢰⢸⠀⠀⠀⠀⠀⠀⠀⠀⠀⠃⠀⠘⡀⢰⢱⠀⠀⠇⠈⠒⡤⢀⡀⠀⠀⠀⢀⣀⣀⡀⠤⠔⣡⠇⠀⡐⠀⡇⠸⠀⠀⠀⠀⠀⠀⠀
// ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠃⠁⢱⡇⢸⡘⢿⣿⣿⡀⠀⠀⠣⡀⠂⠈⠉⠉⠀⢀⡟⣾⣿⣿⣿⠃⠸⣌⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢣⠜⠀⠆⢸⠅⠀⢀⡇⠀⠀⠈⣹⠉⠁⠀⢀⢴⠀⡰⢹⠀⡐⠁⠀⣇⠇⠀⠀⠀⠀⠀⠀⠀
// ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣄⡏⠁⠀⣇⢎⠻⣿⣷⣶⣤⣤⣤⡤⠤⠤⠐⣶⣿⡇⣿⣿⣿⠏⠀⡇⣿⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠇⠀⠘⡸⣸⣾⣿⠇⠀⠀⠀⢿⣿⣦⡔⠋⢸⡜⠀⡆⠔⠀⠀⠀⡟⠀⠀⠀⠀⠀⠀⠀⠀
// ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣸⠋⠇⠀⡄⣸⡘⡄⠘⢿⣿⣿⣿⣿⣷⠀⠀⠀⢿⣿⣧⣿⣿⡟⠀⢰⠇⢻⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⠟⠛⠛⡇⠠⠀⡀⠀⠈⣻⣿⣷⡄⠏⠀⢰⠊⠀⠀⠀⠀⠁⠀⠀⠀⠀⠀⠀⠀⠀
// ⠀⠀⠀⠀⠀⠀⠀⠀⣀⠀⠜⠁⠀⢠⢸⢀⣿⣧⢱⡀⠀⠙⢿⣿⣿⠟⠀⠀⠀⠈⣿⠛⢿⣿⡇⢠⣿⠀⠘⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⡞⠁⠀⠀⢰⡇⠀⠀⠀⠑⣰⠿⠿⠿⠿⣆⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
// ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠸⡇⢸⣿⣿⣎⣷⡄⠀⠘⣿⣿⣦⡀⠔⠂⠉⢻⡀⠀⢹⣧⣿⣿⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣰⠫⣀⠀⠀⠀⠸⡇⠀⠀⠀⣰⠃⠀⠀⣠⠔⣫⢧⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
}

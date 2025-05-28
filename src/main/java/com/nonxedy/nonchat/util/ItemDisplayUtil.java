package com.nonxedy.nonchat.util;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for creating item displays in chat
 * Extracts and formats item information for chat display
 */
public class ItemDisplayUtil {

    /**
     * Creates a component with item display attached
     * @param item The item to display
     * @param format The format string to use
     * @return Component with hover event containing item information
     */
    public static Component createItemComponent(ItemStack item, String format) {
        if (item == null) {
            return Component.text("No item");
        }
        
        String itemName = getItemName(item);
        String formattedMessage = format.replace("{item}", itemName);
        
        Component component = ColorUtil.parseComponent(formattedMessage);
        return component.hoverEvent(createItemHoverEvent(item));
    }
    
    /**
     * Creates a hover event containing item information
     * This method is public to allow direct access from ItemDetector
     * @param item The item to create hover event for
     * @return HoverEvent with item information
     */
    public static HoverEvent<Component> createItemHoverEvent(ItemStack item) {
        Component hoverText = createItemHoverText(item);
        return HoverEvent.showText(hoverText);
    }
    
    /**
     * Creates a component containing item information
     * @param item The item to create component for
     * @return Component with item information
     */
    private static Component createItemHoverText(ItemStack item) {
        List<Component> lines = new ArrayList<>();
        
        // Add item name with proper color
        String itemName = getItemName(item);
        NamedTextColor nameColor = determineRarityColor(item);
        
        lines.add(Component.text(itemName)
            .color(nameColor)
            .decoration(TextDecoration.ITALIC, false)
            .decoration(TextDecoration.BOLD, false));
        
        // Add enchantment information if item is enchanted
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            // Add a blank line after name
            lines.add(Component.text(""));
            
            // Get all enchantments
            item.getEnchantments().forEach((enchantment, level) -> {
                // Format enchantment name
                String enchName = formatEnchantmentName(enchantment);
                String enchLevel = formatEnchantmentLevel(level);
                
                // Add enchantment line
                lines.add(Component.text(enchName + " " + enchLevel)
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            });
        }
        
        // Add lore if exists
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            
            if (lore != null && !lore.isEmpty()) {
                // Add a blank line after name
                lines.add(Component.text(""));
                
                // Add each lore line
                for (String loreLine : lore) {
                    // Clean up legacy color codes
                    String cleanLine = loreLine;
                    Component loreComponent = ColorUtil.parseComponent(cleanLine);
                    lines.add(loreComponent);
                }
            }
        }
        
        // Combine all lines with newlines
        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            result = result.append(lines.get(i));
            
            // Add newline after each line except the last
            if (i < lines.size() - 1) {
                result = result.append(Component.newline());
            }
        }
        
        return result;
    }
    
    /**
     * Gets the display name of an item
     * @param item The item to get name for
     * @return Item display name or material name if no display name
     */
    public static String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return PlainTextComponentSerializer.plainText()
                .serialize(item.getItemMeta().displayName());
        } else {
            return formatMaterialName(item.getType().name());
        }
    }
    
    /**
     * Formats a material name to be more readable
     * @param materialName The material name to format
     * @return Formatted material name
     */
    private static String formatMaterialName(String materialName) {
        return materialName.replace('_', ' ').toLowerCase();
    }
    
    /**
     * Formats an enchantment name to be more readable
     * @param enchantment The enchantment to format
     * @return Formatted enchantment name
     */
    private static String formatEnchantmentName(Enchantment enchantment) {
        String name = enchantment.getKey().getKey();
        // Replace underscores with spaces
        name = name.replace('_', ' ');
        
        // Capitalize first letter of each word
        StringBuilder formattedName = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                formattedName.append(c);
            } else {
                if (capitalizeNext) {
                    formattedName.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    formattedName.append(c);
                }
            }
        }
        
        return formattedName.toString();
    }
    
    /**
     * Formats an enchantment level using Roman numerals
     * @param level The enchantment level
     * @return Formatted level as Roman numeral
     */
    private static String formatEnchantmentLevel(int level) {
        switch (level) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            default: return String.valueOf(level);
        }
    }
    
    /**
     * Determines color based on item rarity
     * @param item The item to determine color for
     * @return NamedTextColor based on item rarity
     */
    private static NamedTextColor determineRarityColor(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            return NamedTextColor.AQUA; // Enchanted items are aqua
        }
        
        String materialName = item.getType().name();
        
        // Check for special materials
        if (materialName.contains("NETHERITE")) {
            return NamedTextColor.DARK_PURPLE;
        } else if (materialName.contains("DIAMOND")) {
            return NamedTextColor.AQUA;
        } else if (materialName.contains("GOLD") || materialName.contains("GILDED")) {
            return NamedTextColor.GOLD;
        } else if (materialName.contains("IRON")) {
            return NamedTextColor.WHITE;
        } else if (materialName.contains("STONE") || materialName.contains("CHAIN")) {
            return NamedTextColor.GRAY;
        } else if (materialName.contains("WOOD") || materialName.contains("LEATHER")) {
            return NamedTextColor.YELLOW;
        }
        
        return NamedTextColor.WHITE; // Default color
    }
    
    /**
     * Extracts item information for display
     * @param player The player to get held item from
     * @return The formatted item text for display
     */
    public static Component getHeldItemInfo(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return createItemComponent(item, "&6[Item: &r{item}&6]");
    }
}

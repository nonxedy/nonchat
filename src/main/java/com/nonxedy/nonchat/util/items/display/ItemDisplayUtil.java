package com.nonxedy.nonchat.util.items.display;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nonxedy.nonchat.util.core.colors.ColorUtil;
import com.nonxedy.nonchat.util.items.localization.ItemLocalizationUtil;
import com.nonxedy.nonchat.util.lang.TranslationUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Utility class for creating item displays in chat with client-side localization
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
        
        Component itemComponent = ItemLocalizationUtil.createTranslatableItemComponent(item);
        return itemComponent.hoverEvent(createItemHoverEvent(item));
    }
    
    /**
     * Creates a formatted item component with brackets for chat display
     * Uses client-side translation for proper localization
     * @param item The item to display
     * @return Component with item name in brackets and hover event
     */
    public static Component createBracketedItemComponent(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            // Use translation utility for "No item" text
            Component noItemComponent = TranslationUtil.getNoItemComponent();
            return Component.text("[")
                .append(noItemComponent)
                .append(Component.text("]"))
                .color(NamedTextColor.GRAY);
        }
        
        // Check if item has custom display name with formatting
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            // Use the original item display name component (preserving custom colors)
            Component displayNameComponent = item.getItemMeta().displayName();
            
            // Create bracketed component with custom name
            Component itemComponent = Component.text("[")
                .append(displayNameComponent)
                .append(Component.text("]"))
                .hoverEvent(createItemHoverEvent(item));
            
            return itemComponent;
        } else {
            // Use translatable component for automatic client-side localization
            Component translatableComponent = ItemLocalizationUtil.createTranslatableItemComponent(item);
            
            Component itemComponent = Component.text("[")
                .append(translatableComponent)
                .append(Component.text("]"))
                .color(NamedTextColor.WHITE)
                .hoverEvent(createItemHoverEvent(item));
            
            return itemComponent;
        }
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
     * Creates a component containing item information with client-side localization
     * @param item The item to create component for
     * @return Component with item information
     */
    private static Component createItemHoverText(ItemStack item) {
        List<Component> lines = new ArrayList<>();
        
        // Add item name - preserve original formatting if it has custom display name
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            // Use the original display name component with all formatting
            Component displayNameComponent = item.getItemMeta().displayName()
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, false);
            lines.add(displayNameComponent);
        } else {
            // Use translatable component for automatic client-side localization
            Component translatableComponent = ItemLocalizationUtil.createTranslatableItemComponent(item)
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, false);
            lines.add(translatableComponent);
        }
        
        // Add enchantment information if item is enchanted
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            // Add a blank line after name
            lines.add(Component.text(""));
            
            // Get all enchantments with translatable names
            item.getEnchantments().forEach((enchantment, level) -> {
                // Use translatable component for enchantment name
                Component enchantmentComponent = ItemLocalizationUtil.getLocalizedEnchantmentComponent(enchantment);
                String enchLevel = formatEnchantmentLevel(level);
                
                // Add enchantment line
                Component enchantmentLine = enchantmentComponent
                    .append(Component.text(" " + enchLevel))
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false);
                
                lines.add(enchantmentLine);
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
                    Component loreComponent = ColorUtil.parseComponent(loreLine);
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
     * Gets the display name of an item (fallback method)
     * @param item The item to get name for
     * @return Item display name or formatted material name if no display name
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
     * Formats a material name to be more readable (fallback)
     * @param materialName The material name to format
     * @return Formatted material name
     */
    private static String formatMaterialName(String materialName) {
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            // Capitalize first letter of each word
            if (words[i].length() > 0) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1));
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Formats an enchantment level using Roman numerals
     * @param level The enchantment level
     * @return Formatted level as Roman numeral
     */
    private static String formatEnchantmentLevel(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(level);
        };
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

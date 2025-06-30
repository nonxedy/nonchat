package com.nonxedy.nonchat.util.items.localization;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

/**
 * Utility class for localizing item and enchantment names using Minecraft's translation system
 * Based on InteractiveChat's approach
 */
public class ItemLocalizationUtil {
    
    /**
     * Gets the localized name of a material using Minecraft's translation keys
     * @param material The material to localize
     * @param language The language code ("en" or "ru")
     * @return Localized material name component
     */
    public static Component getLocalizedMaterialComponent(Material material) {
        String translationKey = getTranslationKey(material);
        return Component.translatable(translationKey);
    }
    
    /**
     * Gets the localized name of a material as string (fallback)
     * @param material The material to localize
     * @param language The language code ("en" or "ru")
     * @return Localized material name
     */
    public static String getLocalizedMaterialName(Material material, String language) {
        // For now, return formatted name as fallback
        // The actual translation will happen on the client side
        return formatMaterialName(material.name());
    }
    
    /**
     * Gets the localized name of an enchantment using Minecraft's translation keys
     * @param enchantment The enchantment to localize
     * @return Localized enchantment component
     */
    public static Component getLocalizedEnchantmentComponent(Enchantment enchantment) {
        String translationKey = "enchantment.minecraft." + enchantment.getKey().getKey();
        return Component.translatable(translationKey);
    }
    
    /**
     * Gets the localized name of an enchantment as string (fallback)
     * @param enchantment The enchantment to localize
     * @param language The language code ("en" or "ru")
     * @return Localized enchantment name
     */
    public static String getLocalizedEnchantmentName(Enchantment enchantment, String language) {
        // For now, return formatted name as fallback
        return formatEnchantmentName(enchantment.getKey().getKey());
    }
    
    /**
     * Creates a translatable component for an item
     * This will be automatically translated by the client
     * @param item The item to create component for
     * @return Translatable component
     */
    public static Component createTranslatableItemComponent(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Component.translatable("item.minecraft.air");
        }
        
        // Check if item has custom display name
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().displayName();
        }
        
        // Use Minecraft's translation key
        String translationKey = getTranslationKey(item.getType());
        return Component.translatable(translationKey);
    }
    
    /**
     * Gets the appropriate translation key for a material
     * @param material The material
     * @return Translation key string
     */
    private static String getTranslationKey(Material material) {
        String materialName = material.name().toLowerCase();
        
        // Check if it's a block or item
        if (material.isBlock()) {
            return "block.minecraft." + materialName;
        } else {
            return "item.minecraft." + materialName;
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
     * Formats an enchantment name to be more readable (fallback)
     * @param enchantmentName The enchantment name to format
     * @return Formatted enchantment name
     */
    private static String formatEnchantmentName(String enchantmentName) {
        String name = enchantmentName.replace('_', ' ');
        
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
}

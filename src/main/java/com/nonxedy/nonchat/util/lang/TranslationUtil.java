package com.nonxedy.nonchat.util.lang;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;

/**
 * Utility for handling translations in the plugin
 */
public class TranslationUtil {
    
    /**
     * Gets a translatable "No item" component based on plugin language setting
     * @return Translatable component for "No item"
     */
    public static Component getNoItemComponent() {
        String language = getPluginLanguage();
        
        if ("ru".equalsIgnoreCase(language)) {
            // For Russian, we can use a custom text since Minecraft doesn't have this translation
            return Component.text("Нет предмета");
        } else {
            // For English and other languages, use a generic text
            return Component.text("No item");
        }
    }
    
    /**
     * Gets the current language setting from the plugin
     * @return Language code
     */
    private static String getPluginLanguage() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("nonchat");
            if (plugin != null) {
                // Try direct config access first
                String language = plugin.getConfig().getString("language", "en");
                if (language != null) {
                    return language;
                }
                
                // Fallback to reflection if needed
                Object configService = plugin.getClass().getMethod("getConfigService").invoke(plugin);
                Object config = configService.getClass().getMethod("getConfig").invoke(configService);
                return (String) config.getClass().getMethod("getLanguage").invoke(config);
            }
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to get plugin language: {0}", e.getMessage());
        }
        return "en";
    }
}
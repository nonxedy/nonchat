package com.nonxedy.nonchat.util.lang;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.nonxedy.nonchat.config.DeathConfig;

import net.kyori.adventure.text.Component;

/**
 * Utility class for localizing entity names using Minecraft's translation system
 * Provides client-side translation for entity names in death messages
 * Supports custom entity names (name tags) and custom type names from configuration
 */
public class EntityLocalizationUtil {
    
    /**
     * Gets a translatable component for an entity's name
     * This will be automatically translated by the client based on their language settings
     * 
     * @param entity The entity to get the name for
     * @param deathConfig Death configuration for custom name settings
     * @return Component with the entity's name
     */
    public static Component getTranslatableEntityName(Entity entity, DeathConfig deathConfig) {
        if (entity == null) {
            return Component.text("Unknown");
        }
        
        // Players should use their actual name, not translation
        if (entity instanceof Player) {
            return Component.text(((Player) entity).getName());
        }
        
        // Get priority setting
        String priority = deathConfig != null ? deathConfig.getEntityNamePriority() : "nametag";
        
        // Get both custom name and custom type name
        Component customNameTag = null;
        Component customTypeName = null;
        
        // Check for entity's custom name (name tag)
        if (deathConfig != null && deathConfig.useCustomEntityNames()) {
            Component nameTag = entity.customName();
            if (nameTag != null) {
                customNameTag = nameTag;
            }
        }
        
        // Check for custom type name from configuration
        if (deathConfig != null) {
            String typeNameStr = deathConfig.getCustomEntityTypeName(entity.getType().name());
            if (typeNameStr != null && !typeNameStr.isEmpty()) {
                customTypeName = Component.text(typeNameStr);
            }
        }
        
        // Apply priority logic
        if ("custom-type".equalsIgnoreCase(priority)) {
            // Priority: custom-type-names > name tag > translation
            if (customTypeName != null) {
                return customTypeName;
            }
            if (customNameTag != null) {
                return customNameTag;
            }
        } else {
            // Priority: name tag > custom-type-names > translation (default)
            if (customNameTag != null) {
                return customNameTag;
            }
            if (customTypeName != null) {
                return customTypeName;
            }
        }
        
        // Use Minecraft's translatable entity name as fallback
        return getTranslatableEntityName(entity.getType());
    }
    
    /**
     * Gets a translatable component for an entity type
     * 
     * @param entityType The entity type to get the name for
     * @return Translatable component with the entity type's name
     */
    public static Component getTranslatableEntityName(EntityType entityType) {
        if (entityType == null) {
            return Component.text("Unknown");
        }
        
        String translationKey = getEntityTranslationKey(entityType);
        return Component.translatable(translationKey);
    }
    
    /**
     * Gets the Minecraft translation key for an entity type
     * Format: entity.minecraft.<entity_type>
     * 
     * @param entityType The entity type
     * @return Translation key string
     */
    private static String getEntityTranslationKey(EntityType entityType) {
        if (entityType == null) {
            return "entity.minecraft.unknown";
        }
        
        // Convert entity type to lowercase for translation key
        String entityName = entityType.name().toLowerCase();
        
        return "entity.minecraft." + entityName;
    }
    
    /**
     * Gets a fallback formatted name for an entity (used when translation is not available)
     * This is a backup method in case translation fails
     * 
     * @param entityType The entity type
     * @return Formatted entity name
     */
    public static String getFormattedEntityName(EntityType entityType) {
        if (entityType == null) {
            return "Unknown";
        }
        
        String name = entityType.name().toLowerCase().replace('_', ' ');
        
        // Capitalize first letter of each word
        StringBuilder formatted = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                formatted.append(c);
            } else {
                if (capitalizeNext) {
                    formatted.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    formatted.append(c);
                }
            }
        }
        
        return formatted.toString();
    }
}

package com.nonxedy.nonchat.core;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import org.bukkit.event.entity.EntityDamageEvent;

import com.nonxedy.nonchat.config.DeathConfig;
import com.nonxedy.nonchat.config.DeathMessageLoader;
import com.nonxedy.nonchat.util.death.DeathMessage;

/**
 * Manages the cache of death messages and handles message selection
 * Provides efficient random variant selection and cache management
 */
public class DeathMessageManager {
    private final Map<EntityDamageEvent.DamageCause, List<DeathMessage>> messageCache;
    private final DeathMessageLoader loader;
    private final DeathConfig deathConfig;
    private final Logger logger;

    /**
     * Creates a new DeathMessageManager
     * @param dataFolder Plugin data folder
     * @param logger Plugin logger
     * @param deathConfig Death configuration instance
     */
    public DeathMessageManager(File dataFolder, Logger logger, DeathConfig deathConfig) {
        this.messageCache = new EnumMap<>(EntityDamageEvent.DamageCause.class);
        this.loader = new DeathMessageLoader(dataFolder, logger, deathConfig);
        this.deathConfig = deathConfig;
        this.logger = logger;
    }

    /**
     * Loads all death messages from configuration files
     * Populates the message cache with data from deaths.yml
     */
    public void loadMessages() {
        try {
            // Clear existing cache before loading
            clearCache();
            
            // Load messages from configuration
            Map<EntityDamageEvent.DamageCause, List<DeathMessage>> loadedMessages = null;
            try {
                loadedMessages = loader.loadAllMessages();
            } catch (Exception e) {
                logger.severe("Failed to load death messages from configuration: " + e.getMessage());
                if (deathConfig.isDebugEnabled()) {
                    e.printStackTrace();
                }
                // Use empty map as fallback
                loadedMessages = new EnumMap<>(EntityDamageEvent.DamageCause.class);
            }
            
            // Validate loaded messages
            if (loadedMessages == null) {
                logger.warning("Death message loader returned null, using empty message cache");
                loadedMessages = new EnumMap<>(EntityDamageEvent.DamageCause.class);
            }
            
            // Populate cache with loaded messages
            messageCache.putAll(loadedMessages);
            
            // Log statistics
            if (deathConfig.isDebugEnabled()) {
                logStatistics();
            }
            
            // Warn if no messages were loaded
            if (messageCache.isEmpty()) {
                logger.warning("No death messages loaded. Death message system will use fallback behavior.");
                logger.warning("Check deaths.yml for configuration errors or create a new file by deleting the existing one.");
            }
            
        } catch (Exception e) {
            logger.severe("Critical error in loadMessages(): " + e.getMessage());
            if (deathConfig.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Selects a random message variant for the given death cause
     * Uses ThreadLocalRandom for efficient random selection
     * 
     * @param cause The damage cause
     * @return DeathMessage object, or null if no custom message exists
     * @deprecated Use {@link #selectMessage(EntityDamageEvent.DamageCause, boolean, com.nonxedy.nonchat.util.death.DamageType)} instead
     */
    @Deprecated
    public DeathMessage selectMessage(EntityDamageEvent.DamageCause cause) {
        return selectMessage(cause, false, null);
    }

    /**
     * Selects a random message variant for the given death cause
     * Supports both standard and indirect death messages with damage type variants
     * 
     * @param cause The damage cause
     * @param isIndirect Whether this is an indirect kill
     * @param damageType The type of damage that caused the indirect kill (can be null)
     * @return DeathMessage object, or null if no custom message exists
     */
    public DeathMessage selectMessage(EntityDamageEvent.DamageCause cause, boolean isIndirect, 
                                     com.nonxedy.nonchat.util.death.DamageType damageType) {
        try {
            if (cause == null) {
                if (deathConfig.isDebugEnabled()) {
                    logger.warning("Attempted to select message for null death cause");
                }
                return null;
            }
            
            // Try indirect message selection first if applicable
            if (isIndirect) {
                DeathMessage indirectMessage = selectIndirectMessage(cause, damageType);
                if (indirectMessage != null) {
                    return indirectMessage;
                }
                // If no indirect message found, fall through to standard selection
                if (deathConfig.isDebugEnabled()) {
                    logger.fine("No indirect message found for cause " + cause.name() + 
                               ", falling back to standard message");
                }
            }
            
            // Standard message selection
            return selectStandardMessage(cause);
            
        } catch (Exception e) {
            logger.warning("Unexpected error in selectMessage() for cause " + (cause != null ? cause.name() : "null") + ": " + e.getMessage());
            if (deathConfig.isDebugEnabled()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Selects an indirect death message variant based on damage type
     * Implements fallback logic: damage-type-specific → generic indirect → null
     * 
     * @param cause The death cause
     * @param damageType The type of damage that caused the indirect kill
     * @return DeathMessage object with indirect variant, or null if none found
     */
    private DeathMessage selectIndirectMessage(EntityDamageEvent.DamageCause cause, 
                                              com.nonxedy.nonchat.util.death.DamageType damageType) {
        try {
            if (cause == null) {
                return null;
            }
            
            List<DeathMessage> messages = messageCache.get(cause);
            if (messages == null || messages.isEmpty()) {
                return null;
            }
            
            // Filter to enabled messages that have indirect variants
            List<DeathMessage> indirectMessages = new ArrayList<>();
            for (DeathMessage message : messages) {
                try {
                    if (message != null && message.isEnabled() && message.hasIndirectVariants()) {
                        // Prefer messages with damage-type-specific variants
                        if (damageType != null && message.hasVariantForDamageType(damageType)) {
                            indirectMessages.add(message);
                        } else if (message.getGenericIndirectMessage() != null) {
                            // Add messages with generic indirect variants as fallback
                            indirectMessages.add(message);
                        }
                    }
                } catch (Exception e) {
                    logger.warning("Error checking indirect variants for cause " + cause.name() + ": " + e.getMessage());
                }
            }
            
            if (indirectMessages.isEmpty()) {
                if (deathConfig.isDebugEnabled()) {
                    logger.fine("No indirect message variants found for cause: " + cause.name());
                }
                return null;
            }
            
            // Select random variant
            try {
                int randomIndex = ThreadLocalRandom.current().nextInt(indirectMessages.size());
                return indirectMessages.get(randomIndex);
            } catch (Exception e) {
                logger.warning("Error selecting random indirect message for cause " + cause.name() + ": " + e.getMessage());
                return indirectMessages.get(0);
            }
            
        } catch (Exception e) {
            logger.warning("Unexpected error in selectIndirectMessage() for cause " + 
                          (cause != null ? cause.name() : "null") + ": " + e.getMessage());
            if (deathConfig.isDebugEnabled()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Selects a standard death message (non-indirect)
     * Refactored from original selectMessage logic
     * 
     * @param cause The death cause
     * @return DeathMessage object, or null if no custom message exists
     */
    private DeathMessage selectStandardMessage(EntityDamageEvent.DamageCause cause) {
        try {
            if (cause == null) {
                if (deathConfig.isDebugEnabled()) {
                    logger.warning("Attempted to select message for null death cause");
                }
                return null;
            }
            
            List<DeathMessage> messages = messageCache.get(cause);
            
            // No custom messages for this cause
            if (messages == null || messages.isEmpty()) {
                if (deathConfig.isDebugEnabled()) {
                    logger.fine("No custom messages configured for death cause: " + cause.name());
                }
                return null;
            }
            
            // Filter to only enabled messages
            List<DeathMessage> enabledMessages = new ArrayList<>();
            for (DeathMessage message : messages) {
                try {
                    if (message != null && message.isEnabled()) {
                        enabledMessages.add(message);
                    }
                } catch (Exception e) {
                    logger.warning("Error checking if message is enabled for cause " + cause.name() + ": " + e.getMessage());
                }
            }
            
            // No enabled messages for this cause
            if (enabledMessages.isEmpty()) {
                if (deathConfig.isDebugEnabled()) {
                    logger.fine("No enabled messages for death cause: " + cause.name());
                }
                return null;
            }
            
            // Select random variant using ThreadLocalRandom for better performance
            try {
                int randomIndex = ThreadLocalRandom.current().nextInt(enabledMessages.size());
                return enabledMessages.get(randomIndex);
            } catch (Exception e) {
                logger.warning("Error selecting random message for cause " + cause.name() + ": " + e.getMessage());
                // Fallback to first message
                return enabledMessages.get(0);
            }
            
        } catch (Exception e) {
            logger.warning("Unexpected error in selectStandardMessage() for cause " + 
                          (cause != null ? cause.name() : "null") + ": " + e.getMessage());
            if (deathConfig.isDebugEnabled()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Checks if a death cause has custom messages configured
     * @param cause The damage cause
     * @return true if custom messages exist and at least one is enabled
     */
    public boolean hasCustomMessages(EntityDamageEvent.DamageCause cause) {
        if (cause == null) {
            return false;
        }
        
        List<DeathMessage> messages = messageCache.get(cause);
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        
        // Check if at least one message is enabled
        for (DeathMessage message : messages) {
            if (message.isEnabled()) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Gets the number of message variants for a death cause
     * Only counts enabled messages
     * 
     * @param cause The damage cause
     * @return Number of enabled variants
     */
    public int getMessageCount(EntityDamageEvent.DamageCause cause) {
        if (cause == null) {
            return 0;
        }
        
        List<DeathMessage> messages = messageCache.get(cause);
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        
        // Count only enabled messages
        int count = 0;
        for (DeathMessage message : messages) {
            if (message.isEnabled()) {
                count++;
            }
        }
        
        return count;
    }

    /**
     * Clears the message cache
     * Should be called before reloading messages
     */
    public void clearCache() {
        messageCache.clear();
        
        if (deathConfig.isDebugEnabled()) {
            logger.info("Death message cache cleared");
        }
    }

    /**
     * Gets statistics about loaded death messages
     * Returns a map of death cause to enabled message count
     * 
     * @return Map of death cause to message count
     */
    public Map<EntityDamageEvent.DamageCause, Integer> getStatistics() {
        Map<EntityDamageEvent.DamageCause, Integer> stats = new EnumMap<>(EntityDamageEvent.DamageCause.class);
        
        for (Map.Entry<EntityDamageEvent.DamageCause, List<DeathMessage>> entry : messageCache.entrySet()) {
            EntityDamageEvent.DamageCause cause = entry.getKey();
            int enabledCount = getMessageCount(cause);
            
            if (enabledCount > 0) {
                stats.put(cause, enabledCount);
            }
        }
        
        return stats;
    }

    /**
     * Logs statistics about loaded death messages
     * Shows message counts per cause for debugging
     */
    private void logStatistics() {
        Map<EntityDamageEvent.DamageCause, Integer> stats = getStatistics();
        
        if (stats.isEmpty()) {
            logger.info("No death messages loaded");
            return;
        }
        
        logger.info("Death message statistics:");
        int totalMessages = 0;
        
        for (Map.Entry<EntityDamageEvent.DamageCause, Integer> entry : stats.entrySet()) {
            int count = entry.getValue();
            logger.info("  " + entry.getKey() + ": " + count + " variant(s)");
            totalMessages += count;
        }
        
        logger.info("Total: " + totalMessages + " message variants across " + stats.size() + " causes");
    }
}

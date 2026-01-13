package com.nonxedy.nonchat.core;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.nonxedy.nonchat.config.DeathConfig;
import com.nonxedy.nonchat.config.DeathMessageLoader;
import com.nonxedy.nonchat.util.core.debugging.Debugger;
import com.nonxedy.nonchat.util.death.DamageType;
import com.nonxedy.nonchat.util.death.DeathMessage;

/**
 * Manages the cache of death messages and handles message selection
 * Provides efficient random variant selection and cache management
 */
public class DeathMessageManager {
    private final Map<String, List<DeathMessage>> messageCache;
    private final DeathMessageLoader loader;
    private final DeathConfig deathConfig;
    private final Debugger debugger;

    /**
     * Creates a new DeathMessageManager
     * @param dataFolder Plugin data folder
     * @param debugger Debug logger instance
     * @param deathConfig Death configuration instance
     */
    public DeathMessageManager(File dataFolder, Debugger debugger, DeathConfig deathConfig) {
        this.messageCache = new HashMap<>();
        this.loader = new DeathMessageLoader(dataFolder, debugger, deathConfig);
        this.deathConfig = deathConfig;
        this.debugger = debugger;
    }

    /**
     * Loads all death messages from configuration files
     * Populates the message cache with data from deaths.yml
     */
    public void loadMessages() {
        try {
            clearCache();
            
            Map<String, List<DeathMessage>> loadedMessages = null;
            try {
                loadedMessages = loader.loadAllMessages();
            } catch (Exception e) {
                if (debugger != null) {
                    debugger.error("DeathMessageManager", "Failed to load death messages from configuration: " + e.getMessage(), e);
                }
                loadedMessages = new HashMap<>();
            }

            if (loadedMessages == null) {
                if (debugger != null) {
                    debugger.warn("DeathMessageManager", "Death message loader returned null, using empty message cache");
                }
                loadedMessages = new HashMap<>();
            }

            messageCache.putAll(loadedMessages);

            if (deathConfig.isDebugEnabled() && debugger != null) {
                logStatistics();
            }

            if (messageCache.isEmpty()) {
                if (debugger != null) {
                    debugger.warn("DeathMessageManager", "No death messages loaded. Death message system will use fallback behavior.");
                    debugger.warn("DeathMessageManager", "Check deaths.yml for configuration errors or create a new file by deleting the existing one.");
                }
            }

        } catch (Exception e) {
            if (debugger != null) {
                debugger.error("DeathMessageManager", "Critical error in loadMessages(): " + e.getMessage(), e);
            }
        }
    }

    /**
     * Selects a random message variant for the given death cause key
     * Supports both standard and indirect death messages with damage type variants
     * 
     * @param causeKey The death cause key (normalized)
     * @param isIndirect Whether this is an indirect kill
     * @param damageType The type of damage that caused the indirect kill (can be null)
     * @return DeathMessage object, or null if no custom message exists
     */
    public DeathMessage selectMessage(String causeKey, boolean isIndirect, DamageType damageType) {
        try {
            if (causeKey == null || causeKey.isEmpty()) {
                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.warn("DeathMessageManager", "Attempted to select message for null or empty death cause key");
                }
                return null;
            }

            String normalizedKey = causeKey.toLowerCase().replace('-', '_');

            if (isIndirect) {
                DeathMessage indirectMessage = selectIndirectMessage(normalizedKey, damageType);
                if (indirectMessage != null) {
                    return indirectMessage;
                }
                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.debug("DeathMessageManager", "No indirect message found for cause " + normalizedKey +
                               ", falling back to standard message");
                }
            }

            return selectStandardMessage(normalizedKey);

        } catch (Exception e) {
            if (debugger != null) {
                debugger.error("DeathMessageManager", "Unexpected error in selectMessage() for cause " +
                              (causeKey != null ? causeKey : "null") + ": " + e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * Selects an indirect death message variant based on damage type
     * Implements fallback logic: damage-type-specific → generic indirect → null
     * 
     * @param causeKey The death cause key (normalized)
     * @param damageType The type of damage that caused the indirect kill
     * @return DeathMessage object with indirect variant, or null if none found
     */
    private DeathMessage selectIndirectMessage(String causeKey, DamageType damageType) {
        try {
            if (causeKey == null || causeKey.isEmpty()) {
                return null;
            }
            
            List<DeathMessage> messages = messageCache.get(causeKey);
            if (messages == null || messages.isEmpty()) {
                return null;
            }
            
            List<DeathMessage> indirectMessages = messages.stream()
                .filter(message -> message != null && message.isEnabled() && message.hasIndirectVariants())
                .filter(message -> {
                    if (damageType != null && message.hasVariantForDamageType(damageType)) {
                        return true;
                    }
                    return message.getGenericIndirectMessage() != null;
                })
                .collect(Collectors.toList());
            
            if (indirectMessages.isEmpty()) {
                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.debug("DeathMessageManager", "No indirect message variants found for cause: " + causeKey);
                }
                return null;
            }

            return selectRandomVariant(indirectMessages);

        } catch (Exception e) {
            if (debugger != null) {
                debugger.error("DeathMessageManager", "Unexpected error in selectIndirectMessage() for cause " +
                              (causeKey != null ? causeKey : "null") + ": " + e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * Selects a standard death message (non-indirect)
     * Refactored from original selectMessage logic
     * 
     * @param causeKey The death cause key (normalized)
     * @return DeathMessage object, or null if no custom message exists
     */
    private DeathMessage selectStandardMessage(String causeKey) {
        try {
            if (causeKey == null || causeKey.isEmpty()) {
                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.warn("DeathMessageManager", "Attempted to select message for null or empty death cause key");
                }
                return null;
            }

            List<DeathMessage> messages = messageCache.get(causeKey);

            if (messages == null || messages.isEmpty()) {
                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.debug("DeathMessageManager", "No custom messages configured for death cause: " + causeKey);
                }
                return null;
            }

            List<DeathMessage> enabledMessages = messages.stream()
                .filter(message -> message != null && message.isEnabled())
                .collect(Collectors.toList());

            if (enabledMessages.isEmpty()) {
                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.debug("DeathMessageManager", "No enabled messages for death cause: " + causeKey);
                }
                return null;
            }

            return selectRandomVariant(enabledMessages);

        } catch (Exception e) {
            if (debugger != null) {
                debugger.error("DeathMessageManager", "Unexpected error in selectStandardMessage() for cause " +
                              (causeKey != null ? causeKey : "null") + ": " + e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * Selects a random variant from a list of death messages
     * Unified helper method to avoid code duplication
     *
     * @param messages List of death messages to select from
     * @return Random DeathMessage from the list, or first element on error
     */
    private DeathMessage selectRandomVariant(List<DeathMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        try {
            int randomIndex = ThreadLocalRandom.current().nextInt(messages.size());
            return messages.get(randomIndex);
        } catch (Exception e) {
            if (debugger != null) {
                debugger.warn("DeathMessageManager", "Error selecting random variant, using first: " + e.getMessage());
            }
            return messages.get(0);
        }
    }

    /**
     * Checks if a death cause has custom messages configured
     * @param causeKey The death cause key (normalized)
     * @return true if custom messages exist and at least one is enabled
     */
    public boolean hasCustomMessages(String causeKey) {
        if (causeKey == null || causeKey.isEmpty()) {
            return false;
        }
        
        String normalizedKey = causeKey.toLowerCase().replace('-', '_');
        List<DeathMessage> messages = messageCache.get(normalizedKey);
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
     * @param causeKey The death cause key (normalized)
     * @return Number of enabled variants
     */
    public int getMessageCount(String causeKey) {
        if (causeKey == null || causeKey.isEmpty()) {
            return 0;
        }
        
        String normalizedKey = causeKey.toLowerCase().replace('-', '_');
        List<DeathMessage> messages = messageCache.get(normalizedKey);
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

        if (deathConfig.isDebugEnabled() && debugger != null) {
            debugger.info("DeathMessageManager", "Death message cache cleared");
        }
    }

    /**
     * Gets statistics about loaded death messages
     * Returns a map of death cause key to enabled message count
     * 
     * @return Map of death cause key to message count
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        
        for (Map.Entry<String, List<DeathMessage>> entry : messageCache.entrySet()) {
            String causeKey = entry.getKey();
            int enabledCount = getMessageCount(causeKey);
            
            if (enabledCount > 0) {
                stats.put(causeKey, enabledCount);
            }
        }
        
        return stats;
    }

    /**
     * Logs statistics about loaded death messages
     * Shows message counts per cause for debugging
     */
    private void logStatistics() {
        Map<String, Integer> stats = getStatistics();

        if (stats.isEmpty()) {
            if (debugger != null) {
                debugger.info("DeathMessageManager", "No death messages loaded");
            }
            return;
        }

        if (debugger != null) {
            debugger.info("DeathMessageManager", "Death message statistics:");
            int totalMessages = 0;

            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                int count = entry.getValue();
                debugger.info("DeathMessageManager", "  " + entry.getKey() + ": " + count + " variant(s)");
                totalMessages += count;
            }

            debugger.info("DeathMessageManager", "Total: " + totalMessages + " message variants across " + stats.size() + " causes");
        }
    }
}

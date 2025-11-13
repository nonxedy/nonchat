package com.nonxedy.nonchat.service;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.config.DeathConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.core.DeathMessageManager;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;
import com.nonxedy.nonchat.util.death.DeathMessage;
import com.nonxedy.nonchat.util.integration.external.IntegrationUtil;

import net.kyori.adventure.text.Component;

/**
 * Main service coordinator for death message system
 * Handles death event processing, message selection, and formatting
 */
public class DeathMessageService {
    private final DeathMessageManager messageManager;
    private final DeathConfig deathConfig;
    private final Logger logger;
    private final com.nonxedy.nonchat.util.death.DeathPlaceholderProcessor placeholderProcessor;

    /**
     * Creates a new DeathMessageService
     * @param plugin Plugin instance
     * @param deathConfig Death configuration instance
     * @param messages Plugin messages instance (reserved for future use)
     */
    public DeathMessageService(Nonchat plugin, DeathConfig deathConfig, PluginMessages messages) {
        this.deathConfig = deathConfig;
        this.logger = plugin.getLogger();
        this.messageManager = new DeathMessageManager(plugin.getDataFolder(), logger, deathConfig);
        this.placeholderProcessor = new com.nonxedy.nonchat.util.death.DeathPlaceholderProcessor(deathConfig);
        
        // Load messages on initialization
        loadMessages();
    }

    /**
     * Handles a player death event completely (message + actions)
     * This is the main entry point for death processing
     * 
     * @param event The player death event
     */
    public void handleDeath(PlayerDeathEvent event) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Check if death message system is enabled
            if (!deathConfig.isEnabled()) {
                if (deathConfig.isDebugEnabled()) {
                    logger.info("Death message system is disabled");
                }
                return;
            }

            // Get the player who died
            Player player = event.getEntity();
            
            // Extract death cause from event
            EntityDamageEvent.DamageCause cause = extractDeathCause(event);
            if (cause == null) {
                if (deathConfig.isDebugEnabled()) {
                    logger.info("Could not determine death cause for player: " + player.getName());
                }
                return;
            }

            // Extract killer information (may be null)
            Entity killer = extractKiller(event);

            // Check if custom messages exist for this cause
            if (!messageManager.hasCustomMessages(cause)) {
                if (deathConfig.isDebugEnabled()) {
                    logger.info("No custom messages for death cause: " + cause);
                }
                // Don't set message, let fallback handle it
                return;
            }

            // Select a random message variant
            DeathMessage deathMessage = messageManager.selectMessage(cause);
            if (deathMessage == null) {
                if (deathConfig.isDebugEnabled()) {
                    logger.info("Failed to select message for cause: " + cause);
                }
                return;
            }

            // Format the message with death-specific placeholders
            String formattedMessage = deathMessage.format(player, killer, event, placeholderProcessor);

            // Apply PlaceholderAPI processing
            formattedMessage = applyPlaceholderAPI(player, formattedMessage);

            // Apply color formatting using ColorUtil
            Component finalComponent = ColorUtil.parseComponent(formattedMessage);

            // Set the death message on the event
            event.deathMessage(finalComponent);

            if (deathConfig.isDebugEnabled()) {
                logger.info("Death message for " + player.getName() + " (" + cause + "): " + formattedMessage);
            }
            
            // Track total death processing time (message selection + formatting)
            long processingTime = System.currentTimeMillis() - startTime;
            
            if (deathConfig.isDebugEnabled()) {
                logger.info("Total death processing time for " + player.getName() + ": " + processingTime + "ms");
            }
            
            // Log warning if death processing exceeds performance target (100ms total)
            if (processingTime > 100) {
                logger.warning("Death processing took " + processingTime + "ms for " + player.getName() + 
                             " (target: <100ms) - this may indicate performance issues");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling death: " + e.getMessage(), e);
        }
    }

    /**
     * Gets a formatted death message for the given death event
     * Returns null to use default/fallback behavior
     * 
     * @param event The player death event
     */
    public Component getDeathMessage(PlayerDeathEvent event) {
        try {
            // Check if death message system is enabled
            if (!deathConfig.isEnabled()) {
                if (deathConfig.isDebugEnabled()) {
                    logger.info("Death message system is disabled");
                }
                return null;
            }

            // Get the player who died
            Player player = event.getEntity();
            
            // Extract death cause from event
            EntityDamageEvent.DamageCause cause = extractDeathCause(event);
            if (cause == null) {
                if (deathConfig.isDebugEnabled()) {
                    logger.info("Could not determine death cause for player: " + player.getName());
                }
                return null;
            }

            // Extract killer information (may be null)
            Entity killer = extractKiller(event);

            // Check if custom messages exist for this cause
            if (!messageManager.hasCustomMessages(cause)) {
                if (deathConfig.isDebugEnabled()) {
                    logger.info("No custom messages for death cause: " + cause);
                }
                // Return null to use fallback if enabled
                return deathConfig.useFallback() ? null : Component.empty();
            }

            // Select a random message variant
            DeathMessage deathMessage = messageManager.selectMessage(cause);
            if (deathMessage == null) {
                if (deathConfig.isDebugEnabled()) {
                    logger.info("Failed to select message for cause: " + cause);
                }
                return deathConfig.useFallback() ? null : Component.empty();
            }

            // Format the message with death-specific placeholders
            String formattedMessage = deathMessage.format(player, killer, event, placeholderProcessor);

            // Apply PlaceholderAPI processing
            formattedMessage = applyPlaceholderAPI(player, formattedMessage);

            // Apply color formatting using ColorUtil
            Component finalComponent = ColorUtil.parseComponent(formattedMessage);

            if (deathConfig.isDebugEnabled()) {
                logger.info("Death message for " + player.getName() + " (" + cause + "): " + formattedMessage);
            }

            return finalComponent;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing death message: " + e.getMessage(), e);
            // Return null to use fallback on error
            return null;
        }
    }

    /**
     * Extracts the death cause from the death event
     * @param event The player death event
     * @return The damage cause, or null if not available
     */
    private EntityDamageEvent.DamageCause extractDeathCause(PlayerDeathEvent event) {
        try {
            EntityDamageEvent damageEvent = event.getEntity().getLastDamageCause();
            if (damageEvent != null) {
                return damageEvent.getCause();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error extracting death cause: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Extracts killer information from the death event
     * Handles both player killers and entity killers (mobs, projectiles, etc.)
     * @param event The player death event
     * @return The killer entity, or null if no killer
     */
    private Entity extractKiller(PlayerDeathEvent event) {
        try {
            Player victim = event.getEntity();
            
            // First try to get player killer (only works if killer is a player)
            Player playerKiller = victim.getKiller();
            if (playerKiller != null) {
                return playerKiller;
            }
            
            // If no player killer, check the damage event for entity killer
            EntityDamageEvent damageEvent = victim.getLastDamageCause();
            if (damageEvent instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
                org.bukkit.event.entity.EntityDamageByEntityEvent entityDamageEvent = 
                    (org.bukkit.event.entity.EntityDamageByEntityEvent) damageEvent;
                
                // Get the damager entity (could be mob, projectile, etc.)
                Entity damager = entityDamageEvent.getDamager();
                
                if (deathConfig.isDebugEnabled()) {
                    logger.info("Killer entity found: " + (damager != null ? damager.getType().name() : "null"));
                }
                
                return damager;
            }
            
            if (deathConfig.isDebugEnabled()) {
                logger.info("No killer found for death event (environmental death)");
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error extracting killer: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Applies PlaceholderAPI processing to the message
     * @param player The player who died
     * @param message The message to process
     * @return Message with placeholders replaced
     */
    private String applyPlaceholderAPI(Player player, String message) {
        try {
            return IntegrationUtil.processPlaceholders(player, message);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error processing PlaceholderAPI placeholders: " + e.getMessage(), e);
            // Return original message if PlaceholderAPI processing fails
            return message;
        }
    }

    /**
     * Loads all death messages from configuration
     */
    private void loadMessages() {
        try {
            messageManager.loadMessages();
            logger.info("Death messages loaded successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load death messages: " + e.getMessage(), e);
        }
    }

    /**
     * Reloads all death message configurations
     * Clears cache and reloads from files
     * Maintains previous configuration if reload fails
     */
    public void reload() {
        // Store previous state in case reload fails
        Map<EntityDamageEvent.DamageCause, Integer> previousStats = null;
        boolean hadPreviousConfig = false;
        
        try {
            // Get current statistics before reload
            previousStats = getStatistics();
            hadPreviousConfig = !previousStats.isEmpty();
            
            // Reload death config
            deathConfig.reload();
            
            // Reload messages
            messageManager.loadMessages();
            
            // Get new statistics
            Map<EntityDamageEvent.DamageCause, Integer> newStats = getStatistics();
            int totalMessages = newStats.values().stream().mapToInt(Integer::intValue).sum();
            int totalCauses = newStats.size();

            // Log success with statistics
            logger.info("Death messages reloaded successfully: " + totalMessages + " message variants across " + totalCauses + " death causes");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to reload death messages: " + e.getMessage(), e);
            
            // If we had a previous configuration, it's still in memory
            if (hadPreviousConfig && previousStats != null) {
                int previousTotal = previousStats.values().stream().mapToInt(Integer::intValue).sum();
                logger.warning("Maintaining previous configuration with " + previousTotal + " message variants");
            } else {
                logger.warning("No previous configuration available, death message system may be unavailable");
            }
            
            // Re-throw to notify caller of failure
            throw new RuntimeException("Failed to reload death messages", e);
        }
    }

    /**
     * Gets statistics about loaded death messages
     * @return Map of death cause to message count
     */
    public Map<EntityDamageEvent.DamageCause, Integer> getStatistics() {
        return messageManager.getStatistics();
    }
}

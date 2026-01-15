package com.nonxedy.nonchat.service;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.config.DeathConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.core.DeathMessageManager;
import com.nonxedy.nonchat.core.IndirectDeathTracker;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;
import com.nonxedy.nonchat.util.core.debugging.Debugger;
import com.nonxedy.nonchat.util.death.DamageRecord;
import com.nonxedy.nonchat.util.death.DamageType;
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
    private final Debugger debugger;
    private final IndirectDeathTracker indirectDeathTracker;

    /**
     * Creates a new DeathMessageService
     * @param plugin Plugin instance
     * @param deathConfig Death configuration instance
     * @param messages Plugin messages instance (reserved for future use)
     * @param indirectDeathTracker Indirect death tracker instance
     * @param debugger Debug logger instance
     */
    public DeathMessageService(Nonchat plugin, DeathConfig deathConfig, PluginMessages messages, 
                              IndirectDeathTracker indirectDeathTracker, Debugger debugger) {
        this.deathConfig = deathConfig;
        this.debugger = debugger;
        this.messageManager = new DeathMessageManager(plugin.getDataFolder(), debugger, deathConfig);
        this.indirectDeathTracker = indirectDeathTracker;
        
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
            // Process death and get formatted component
            DeathMessageResult result = processDeathEvent(event);
            
            if (result == null) {
                // No custom message available, let fallback handle it
                return;
            }

            // Set the death message on the event
            event.deathMessage(result.component);

            if (deathConfig.isDebugEnabled() && debugger != null) {
                String messageType = result.isIndirect ? "indirect" : "standard";
                debugger.info("DeathMessageService", "Death message for " + result.player.getName() + " (" + result.causeKey + ", " + messageType + "): " + result.formattedMessage);
            }

            // Track total death processing time (message selection + formatting)
            long processingTime = System.currentTimeMillis() - startTime;

            if (deathConfig.isDebugEnabled() && debugger != null) {
                debugger.info("DeathMessageService", "Total death processing time for " + result.player.getName() + ": " + processingTime + "ms" +
                          (result.isIndirect ? " (with indirect tracking)" : ""));
            }

            // Log warning if death processing exceeds performance target (100ms total)
            if (processingTime > 100 && debugger != null) {
                debugger.warn("DeathMessageService", "Death processing took " + processingTime + "ms for " + result.player.getName() +
                             " (target: <100ms) - this may indicate performance issues");
            }

        } catch (Exception e) {
            if (debugger != null) {
                debugger.error("DeathMessageService", "Error handling death: " + e.getMessage(), e);
            }
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
            // Process death and get formatted component
            DeathMessageResult result = processDeathEvent(event);
            
            if (result == null) {
                // Return null to use fallback if enabled, empty component otherwise
                return deathConfig.useFallback() ? null : Component.empty();
            }

            if (deathConfig.isDebugEnabled() && debugger != null) {
                debugger.info("DeathMessageService", "Death message for " + result.player.getName() + " (" + result.causeKey + "): " + result.formattedMessage);
            }

            return result.component;

        } catch (Exception e) {
            if (debugger != null) {
                debugger.error("DeathMessageService", "Error processing death message: " + e.getMessage(), e);
            }
            // Return null to use fallback on error
            return null;
        }
    }

    /**
     * Internal class to hold death message processing results
     */
    private static class DeathMessageResult {
        final Player player;
        final String causeKey;
        final boolean isIndirect;
        final String formattedMessage;
        final Component component;
        
        DeathMessageResult(Player player, String causeKey, boolean isIndirect, String formattedMessage, Component component) {
            this.player = player;
            this.causeKey = causeKey;
            this.isIndirect = isIndirect;
            this.formattedMessage = formattedMessage;
            this.component = component;
        }
    }

    /**
     * Processes a death event and returns the formatted death message result
     * This method contains the shared logic between handleDeath() and getDeathMessage()
     * 
     * @param event The player death event
     * @return DeathMessageResult containing all processed information, or null if no custom message available
     */
    private DeathMessageResult processDeathEvent(PlayerDeathEvent event) {
        // Check if death message system is enabled
        if (!deathConfig.isEnabled()) {
            if (deathConfig.isDebugEnabled() && debugger != null) {
                debugger.info("DeathMessageService", "Death message system is disabled");
            }
            return null;
        }

        // Get the player who died
        Player player = event.getEntity();
        
        // Extract death cause from event
        String causeKey = extractDeathCauseKey(event);
        if (causeKey == null) {
            if (deathConfig.isDebugEnabled() && debugger != null) {
                debugger.info("DeathMessageService", "Could not determine death cause for player: " + player.getName());
            }
            return null;
        }

        // Extract killer information (may be null for environmental deaths)
        Entity killer = extractKiller(event);
        
        // For environmental deaths, ignore Bukkit's automatic killer assignment
        // We want to use our own indirect tracking system instead
        boolean isEnvironmental = isEnvironmentalDeath(causeKey);
        if (isEnvironmental && killer instanceof Player) {
            if (deathConfig.isDebugEnabled() && debugger != null) {
                debugger.info("DeathMessageService", "Ignoring Bukkit's automatic killer assignment for environmental death (cause: " + causeKey + ")");
            }
            killer = null;
        }

        if (deathConfig.isDebugEnabled() && debugger != null) {
            debugger.info("DeathMessageService", "Death analysis for " + player.getName() + ":");
            debugger.info("DeathMessageService", "  - Cause: " + causeKey);
            debugger.info("DeathMessageService", "  - Killer: " + (killer != null ? killer.getType().name() : "null"));
            debugger.info("DeathMessageService", "  - Is environmental: " + isEnvironmental);
            debugger.info("DeathMessageService", "  - Tracking enabled: " + deathConfig.isIndirectTrackingEnabled());
        }
        
        // Check for indirect death attribution
        boolean isIndirect = false;
        DamageType damageType = null;
        DamageRecord lastDamager = null;
        
        if (deathConfig.isIndirectTrackingEnabled() && killer == null && isEnvironmental) {
            // Query IndirectDeathTracker for recent damager
            lastDamager = indirectDeathTracker.getLastDamager(player);

            if (deathConfig.isDebugEnabled() && debugger != null) {
                debugger.info("DeathMessageService", "  - Checking for indirect death (lastDamager: " + (lastDamager != null ? "found" : "null") + ")");
            }

            if (lastDamager != null) {
                // Check if damager is still valid (within tracking window)
                long trackingWindowMs = deathConfig.getTrackingWindow() * 1000L;
                if (lastDamager.isValid(trackingWindowMs)) {
                    isIndirect = true;
                    damageType = lastDamager.getDamageType();

                    // Get the indirect killer player (may be offline) - must be on main thread
                    if (lastDamager.getDamagerUUID() != null) {
                        killer = Bukkit.getPlayer(lastDamager.getDamagerUUID());
                        if (killer == null && deathConfig.isDebugEnabled() && debugger != null) {
                            debugger.debug("DeathMessageService", "Indirect killer is offline: " + lastDamager.getDamagerName());
                        }
                    } else if (deathConfig.isDebugEnabled() && debugger != null) {
                        debugger.warn("DeathMessageService", "Damage record has null UUID for damager: " + lastDamager.getDamagerName());
                    }

                    if (deathConfig.isDebugEnabled() && debugger != null) {
                        debugger.info("DeathMessageService", "Indirect death detected for " + player.getName() +
                                  " - Killer: " + lastDamager.getDamagerName() +
                                  " - Damage Type: " + damageType.name() +
                                  " - Age: " + lastDamager.getAge() + "ms");
                    }
                } else {
                    if (deathConfig.isDebugEnabled() && debugger != null) {
                        debugger.info("DeathMessageService", "Damage record found for " + player.getName() +
                                  " but expired (age: " + lastDamager.getAge() + "ms, window: " + trackingWindowMs + "ms)");
                    }
                }
            } else {
                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.info("DeathMessageService", "No recent damager found for environmental death of " + player.getName() +
                              " (cause: " + causeKey + ")");
                }
            }
        } else if (deathConfig.isDebugEnabled() && debugger != null) {
            debugger.info("DeathMessageService", "  - Skipping indirect check: tracking=" + deathConfig.isIndirectTrackingEnabled() +
                      ", killer==null=" + (killer == null) +
                      ", isEnvironmental=" + isEnvironmentalDeath(causeKey));
        }

        // Check if custom messages exist for this cause
        if (!messageManager.hasCustomMessages(causeKey)) {
            if (deathConfig.isDebugEnabled() && debugger != null) {
                debugger.info("DeathMessageService", "No custom messages for death cause: " + causeKey);
            }
            return null;
        }

        // Select a random message variant (with indirect support)
        DeathMessage deathMessage = messageManager.selectMessage(causeKey, isIndirect, damageType);
        if (deathMessage == null) {
            if (deathConfig.isDebugEnabled() && debugger != null) {
                debugger.info("DeathMessageService", "Failed to select message for cause: " + causeKey +
                          " (indirect: " + isIndirect + ", damageType: " + damageType + ")");
            }
            return null;
        }

        // Get the appropriate message text based on indirect status and damage type
        String messageText = deathMessage.getMessage(isIndirect, damageType);
        
        // Format the message with death-specific placeholders
        String formattedMessage = formatDeathMessage(messageText, player, killer, lastDamager, event);

        // Apply PlaceholderAPI processing
        formattedMessage = applyPlaceholderAPI(player, formattedMessage);

        // Apply color formatting using ColorUtil
        Component finalComponent = ColorUtil.parseComponent(formattedMessage);

        return new DeathMessageResult(player, causeKey, isIndirect, formattedMessage, finalComponent);
    }

    /**
     * Extracts the death cause key from the death event
     * @param event The player death event
     * @return The damage cause key (normalized), or null if not available
     */
    private String extractDeathCauseKey(PlayerDeathEvent event) {
        try {
            EntityDamageEvent damageEvent = event.getEntity().getLastDamageCause();
            if (damageEvent != null) {
                EntityDamageEvent.DamageCause cause = damageEvent.getCause();
                if (cause != null) {
                    // Normalize to lowercase with underscores
                    return cause.name().toLowerCase();
                }
            }
        } catch (Exception e) {
            if (debugger != null) {
                debugger.warn("DeathMessageService", "Error extracting death cause: " + e.getMessage());
            }
        }
        return null;
    }
    
    /**
     * Determines if a death cause is environmental and eligible for indirect attribution
     * @param causeKey The death cause key (normalized) to check
     * @return true if the cause is environmental and configured for tracking
     */
    private boolean isEnvironmentalDeath(String causeKey) {
        if (causeKey == null || causeKey.isEmpty()) {
            return false;
        }
        
        // Get the list of tracked causes from configuration
        List<String> trackedCauses = deathConfig.getTrackedCauses();
        
        // Normalize the cause key and check if it's in the tracked list
        String normalizedKey = causeKey.toLowerCase().replace('-', '_');
        
        // Check both the normalized key and uppercase version (for backward compatibility)
        return trackedCauses.contains(normalizedKey) || trackedCauses.contains(causeKey.toUpperCase());
    }

    /**
     * Extracts killer information from the death event
     * Handles both player killers and entity killers (mobs, projectiles, etc.)
     * For projectiles, extracts the shooter instead of returning the projectile entity
     * @param event The player death event
     * @return The killer entity (player or mob), or null if no killer or environmental death
     */
    private Entity extractKiller(PlayerDeathEvent event) {
        try {
            Player victim = event.getEntity();
            if (victim == null) {
                return null;
            }
            
            // First try to get player killer (only works if killer is a player)
            Player playerKiller = victim.getKiller();
            if (playerKiller != null) {
                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.info("DeathMessageService", "Direct player killer found: " + playerKiller.getName());
                }
                return playerKiller;
            }

            // If no player killer, check the damage event for entity killer
            EntityDamageEvent damageEvent = victim.getLastDamageCause();
            if (damageEvent instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
                org.bukkit.event.entity.EntityDamageByEntityEvent entityDamageEvent =
                    (org.bukkit.event.entity.EntityDamageByEntityEvent) damageEvent;

                // Get the damager entity (could be mob, projectile, etc.)
                Entity damager = entityDamageEvent.getDamager();

                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.info("DeathMessageService", "Damager entity found: " + (damager != null ? damager.getType().name() : "null"));
                }

                if (damager == null) {
                    if (deathConfig.isDebugEnabled() && debugger != null) {
                        debugger.debug("DeathMessageService", "Damager is null, treating as environmental death");
                    }
                    return null;
                }
                
                // Extract actual killer from projectiles
                if (damager instanceof org.bukkit.entity.Projectile) {
                    org.bukkit.entity.Projectile projectile = (org.bukkit.entity.Projectile) damager;
                    org.bukkit.projectiles.ProjectileSource shooter = projectile.getShooter();
                    
                    if (shooter == null) {
                        if (deathConfig.isDebugEnabled() && debugger != null) {
                            debugger.debug("DeathMessageService", "Projectile has null shooter (environmental)");
                        }
                        return null;
                    }

                    if (shooter instanceof Player) {
                        if (deathConfig.isDebugEnabled() && debugger != null) {
                            debugger.info("DeathMessageService", "Projectile shooter is player: " + ((Player) shooter).getName());
                        }
                        return (Player) shooter;
                    } else if (shooter instanceof Entity) {
                        if (deathConfig.isDebugEnabled() && debugger != null) {
                            debugger.info("DeathMessageService", "Projectile shooter is entity: " + ((Entity) shooter).getType().name());
                        }
                        return (Entity) shooter;
                    }
                    // If shooter is not an entity, return null (environmental)
                    if (deathConfig.isDebugEnabled() && debugger != null) {
                        debugger.info("DeathMessageService", "Projectile has no valid shooter (environmental)");
                    }
                    return null;
                }
                
                // Extract actual killer from TNT
                if (damager instanceof org.bukkit.entity.TNTPrimed) {
                    org.bukkit.entity.TNTPrimed tnt = (org.bukkit.entity.TNTPrimed) damager;
                    Entity source = tnt.getSource();
                    if (source != null) {
                        if (deathConfig.isDebugEnabled() && debugger != null) {
                            debugger.info("DeathMessageService", "TNT source: " + source.getType().name());
                        }
                        return source;
                    }
                    // TNT with no source is environmental
                    if (deathConfig.isDebugEnabled() && debugger != null) {
                        debugger.info("DeathMessageService", "TNT has no source (environmental)");
                    }
                    return null;
                }

                // For direct entity attacks (mobs, players), return the damager
                if (deathConfig.isDebugEnabled() && damager != null && debugger != null) {
                    debugger.info("DeathMessageService", "Direct entity killer: " + damager.getType().name());
                }
                return damager;
            }

            if (deathConfig.isDebugEnabled() && debugger != null) {
                debugger.info("DeathMessageService", "No killer found for death event (environmental death)");
            }
            
        } catch (Exception e) {
            if (debugger != null) {
                debugger.warn("DeathMessageService", "Error extracting killer: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Formats a death message with player and killer placeholders
     * @param messageText The raw message text with placeholders
     * @param victim The player who died
     * @param killer The killer entity (may be null)
     * @param lastDamager The last damager record for indirect kills (may be null)
     * @param event The death event
     * @return Formatted message with placeholders replaced
     */
    private String formatDeathMessage(String messageText, Player victim, Entity killer, 
                                     DamageRecord lastDamager, PlayerDeathEvent event) {
        if (messageText == null) {
            return "";
        }
        
        String formatted = messageText;
        
        // Replace victim placeholders
        formatted = formatted.replace("%player_name%", victim.getName());
        formatted = formatted.replace("{victim}", victim.getName());
        formatted = formatted.replace("{player}", victim.getName());
        
        // Replace killer placeholders
        String killerName = deathConfig.getUnknownPlayerPlaceholder();
        if (killer instanceof Player) {
            killerName = ((Player) killer).getName();
        } else if (lastDamager != null) {
            // Use cached killer name for indirect kills when killer is offline
            killerName = lastDamager.getDamagerName();
        } else if (killer != null) {
            // For non-player entities
            killerName = killer.getName();
        }
        
        formatted = formatted.replace("{killer_name}", killerName);
        formatted = formatted.replace("{killer}", killerName);
        formatted = formatted.replace("%killer_name%", killerName);
        
        // Replace coordinates if enabled
        if (deathConfig.showCoordinates()) {
            formatted = formatted.replace("{x}", String.valueOf(victim.getLocation().getBlockX()));
            formatted = formatted.replace("{y}", String.valueOf(victim.getLocation().getBlockY()));
            formatted = formatted.replace("{z}", String.valueOf(victim.getLocation().getBlockZ()));
            formatted = formatted.replace("{world}", victim.getWorld().getName());
        }
        
        return formatted;
    }
    
    /**
     * Applies PlaceholderAPI processing to the message
     * @param player The player who died
     * @param message The message to process
     * @return Message with placeholders replaced, or original message if PlaceholderAPI is unavailable
     */
    private String applyPlaceholderAPI(Player player, String message) {
        // Check if PlaceholderAPI is available before attempting to process
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            if (deathConfig.isDebugEnabled() && debugger != null) {
                debugger.debug("DeathMessageService", "PlaceholderAPI not available, skipping placeholder processing");
            }
            return message;
        }

        try {
            return IntegrationUtil.processPlaceholders(player, message);
        } catch (Exception e) {
            if (debugger != null) {
                debugger.warn("DeathMessageService", "Error processing PlaceholderAPI placeholders: " + e.getMessage());
            }
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
            if (debugger != null) {
                debugger.info("DeathMessageService", "Death messages loaded successfully");
            }
        } catch (Exception e) {
            if (debugger != null) {
                debugger.error("DeathMessageService", "Failed to load death messages: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Reloads all death message configurations
     * Clears cache and reloads from files
     * Maintains previous configuration if reload fails
     * Reinitializes tracking system with updated configuration
     */
    public void reload() {
        // Store previous state in case reload fails
        Map<String, Integer> previousStats = null;
        boolean hadPreviousConfig = false;
        
        try {
            // Get current statistics before reload
            previousStats = getStatistics();
            hadPreviousConfig = !previousStats.isEmpty();
            
            // Clear indirect death tracking data
            if (indirectDeathTracker != null) {
                indirectDeathTracker.clearAll();
                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.info("DeathMessageService", "Cleared indirect death tracking data");
                }
            }

            // Reload death config (this will reload tracking window and minimum damage settings)
            deathConfig.reload();

            // Reload messages
            messageManager.loadMessages();

            // Get new statistics
            Map<String, Integer> newStats = getStatistics();
            int totalMessages = newStats.values().stream().mapToInt(Integer::intValue).sum();
            int totalCauses = newStats.size();

            // Log success with statistics
            if (debugger != null) {
                debugger.info("DeathMessageService", "Death messages reloaded successfully: " + totalMessages + " message variants across " + totalCauses + " death causes");

                // Log indirect tracking status with updated configuration values
                if (deathConfig.isIndirectTrackingEnabled()) {
                    debugger.info("DeathMessageService", "Indirect death tracking is enabled (window: " + deathConfig.getTrackingWindow() +
                              "s, min damage: " + deathConfig.getMinimumDamage() + " hearts)");

                    // Log tracked damage types
                    StringBuilder trackedTypes = new StringBuilder("Tracking damage types: ");
                    if (deathConfig.isTrackMelee()) trackedTypes.append("melee ");
                    if (deathConfig.isTrackProjectile()) trackedTypes.append("projectile ");
                    if (deathConfig.isTrackExplosion()) trackedTypes.append("explosion");
                    debugger.info("DeathMessageService", trackedTypes.toString().trim());

                    // Log tracked causes
                    List<String> trackedCauses = deathConfig.getTrackedCauses();
                    debugger.info("DeathMessageService", "Tracking " + trackedCauses.size() + " environmental death causes: " +
                              String.join(", ", trackedCauses));
                } else {
                    debugger.info("DeathMessageService", "Indirect death tracking is disabled");
                }
            }
            
        } catch (Exception e) {
            if (debugger != null) {
                debugger.error("DeathMessageService", "Failed to reload death messages: " + e.getMessage(), e);

                // If we had a previous configuration, it's still in memory
                if (hadPreviousConfig && previousStats != null) {
                    int previousTotal = previousStats.values().stream().mapToInt(Integer::intValue).sum();
                    debugger.warn("DeathMessageService", "Maintaining previous configuration with " + previousTotal + " message variants");
                } else {
                    debugger.warn("DeathMessageService", "No previous configuration available, death message system may be unavailable");
                }
            }

            // Re-throw to notify caller of failure
            throw new RuntimeException("Failed to reload death messages", e);
        }
    }

    /**
     * Gets statistics about loaded death messages
     * @return Map of death cause key to message count
     */
    public Map<String, Integer> getStatistics() {
        return messageManager.getStatistics();
    }
    
    /**
     * Gets statistics about the indirect death tracking cache
     * @return Map containing cache statistics (size, hit rate, evictions, etc.)
     */
    public Map<String, Object> getTrackingStatistics() {
        if (indirectDeathTracker == null) {
            return Map.of("error", "Indirect death tracker not initialized");
        }
        return indirectDeathTracker.getStatistics();
    }
    
    /**
     * Logs detailed tracking statistics to the console
     * Useful for debugging and monitoring cache performance
     */
    public void logTrackingStatistics() {
        if (!deathConfig.isIndirectTrackingEnabled()) {
            if (debugger != null) {
                debugger.info("DeathMessageService", "[IndirectDeath] Tracking is disabled");
            }
            return;
        }

        Map<String, Object> stats = getTrackingStatistics();
        DecimalFormat percentFormat = new DecimalFormat("0.00");

        String statsMessage = String.format(
            "=== Indirect Death Tracking Statistics ===%n" +
            "Cache Size: %s entries%n" +
            "Hit Rate: %s%%%n" +
            "Miss Rate: %s%%%n" +
            "Total Hits: %s%n" +
            "Total Misses: %s%n" +
            "Evictions: %s%n" +
            "Configuration:%n" +
            "  - Tracking Window: %s seconds%n" +
            "  - Minimum Damage: %s hearts%n" +
            "  - Track Melee: %s%n" +
            "  - Track Projectile: %s%n" +
            "  - Track Explosion: %s%n" +
            "  - Tracked Causes: %s%n" +
            "==========================================",
            stats.get("size"),
            percentFormat.format(((Number) stats.get("hitRate")).doubleValue() * 100),
            percentFormat.format(((Number) stats.get("missRate")).doubleValue() * 100),
            stats.get("hitCount"),
            stats.get("missCount"),
            stats.get("evictionCount"),
            deathConfig.getTrackingWindow(),
            deathConfig.getMinimumDamage(),
            deathConfig.isTrackMelee(),
            deathConfig.isTrackProjectile(),
            deathConfig.isTrackExplosion(),
            String.join(", ", deathConfig.getTrackedCauses())
        );

        if (debugger != null) {
            debugger.info("DeathMessageService", statsMessage);
        }
    }
}

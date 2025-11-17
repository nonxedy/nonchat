package com.nonxedy.nonchat.listener;

import com.nonxedy.nonchat.config.DeathConfig;
import com.nonxedy.nonchat.core.IndirectDeathTracker;
import com.nonxedy.nonchat.util.death.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.logging.Logger;

/**
 * Listens to damage events and records them for indirect death tracking.
 * Uses MONITOR priority to observe final damage after other plugins have processed it.
 */
public class DamageTrackingListener implements Listener {
    
    private final IndirectDeathTracker tracker;
    private final Logger logger;
    private final DeathConfig deathConfig;
    private final double minimumDamage;
    
    /**
     * Creates a new damage tracking listener.
     * 
     * @param tracker The indirect death tracker to record damage events
     * @param logger Logger for debug output
     * @param deathConfig Death configuration for master toggle check
     * @param minimumDamage Minimum damage threshold (in hearts) to record
     */
    public DamageTrackingListener(IndirectDeathTracker tracker, Logger logger, DeathConfig deathConfig, double minimumDamage) {
        this.tracker = tracker;
        this.logger = logger;
        this.deathConfig = deathConfig;
        this.minimumDamage = minimumDamage;
    }
    
    /**
     * Handles entity damage events to track potential indirect kills.
     * Uses MONITOR priority to observe final damage after other plugins.
     * 
     * @param event The entity damage event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Fast path: Skip if tracking is disabled (master toggle check)
        if (!deathConfig.isIndirectTrackingEnabled()) {
            return;
        }
        
        // Only track damage to players
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        
        // Check minimum damage threshold (convert to hearts: 1 heart = 2 damage)
        double damageInHearts = event.getFinalDamage() / 2.0;
        if (damageInHearts < minimumDamage) {
            if (deathConfig.isDebugEnabled()) {
                logger.fine(String.format(
                    "[IndirectDeath] Damage below threshold: %s -> %s (%.1f hearts < %.1f hearts minimum)",
                    event.getDamager().getType().name(),
                    victim.getName(),
                    damageInHearts,
                    minimumDamage
                ));
            }
            return;
        }
        
        // Extract the actual damager (handle projectiles, TNT, etc.)
        Player damager = extractDamager(event.getDamager());
        if (damager == null) {
            if (deathConfig.isDebugEnabled()) {
                logger.fine(String.format(
                    "[IndirectDeath] No player damager found for damage to %s (damager type: %s)",
                    victim.getName(),
                    event.getDamager().getType().name()
                ));
            }
            return;
        }
        
        // Don't track self-damage
        if (victim.getUniqueId().equals(damager.getUniqueId())) {
            if (deathConfig.isDebugEnabled()) {
                logger.fine(String.format(
                    "[IndirectDeath] Ignoring self-damage: %s",
                    victim.getName()
                ));
            }
            return;
        }
        
        // Classify the damage type
        DamageType damageType = classifyDamageType(event);
        
        // Record the damage event
        tracker.recordDamage(victim, damager, damageType);
        
        // Debug logging
        if (deathConfig.isDebugEnabled()) {
            logger.info(String.format(
                "[IndirectDeath] Recorded %s damage: %s -> %s (%.1f hearts, cause: %s)",
                damageType.name(),
                damager.getName(),
                victim.getName(),
                damageInHearts,
                event.getCause().name()
            ));
        }
    }
    
    /**
     * Extracts the actual player damager from the damage source.
     * Handles direct attacks, projectiles, and explosions.
     * 
     * @param damager The entity that caused the damage
     * @return The player responsible for the damage, or null if not player-caused
     */
    private Player extractDamager(Entity damager) {
        // Direct player attack
        if (damager instanceof Player) {
            return (Player) damager;
        }
        
        // Projectile (arrow, snowball, trident, etc.)
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        
        // TNT explosion
        if (damager instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) damager;
            Entity source = tnt.getSource();
            if (source instanceof Player) {
                return (Player) source;
            }
        }
        
        // Other entity types not tracked
        return null;
    }
    
    /**
     * Classifies the type of damage based on the damage source and cause.
     * 
     * @param event The damage event
     * @return The classified damage type
     */
    private DamageType classifyDamageType(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        EntityDamageEvent.DamageCause cause = event.getCause();
        
        // Projectile damage
        if (damager instanceof Projectile) {
            return DamageType.PROJECTILE;
        }
        
        // Explosion damage
        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || 
            cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
            damager instanceof TNTPrimed) {
            return DamageType.EXPLOSION;
        }
        
        // Melee attack (direct entity attack)
        if (damager instanceof Player && 
            (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK || 
             cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)) {
            return DamageType.MELEE;
        }
        
        // Knockback-focused damage (high knockback component)
        // This is a heuristic - if the damage is low but there's knockback
        if (damager instanceof Player) {
            double damage = event.getFinalDamage();
            // If damage is less than 2 hearts but there's knockback, classify as knockback
            if (damage < 4.0) {
                return DamageType.KNOCKBACK;
            }
            // Otherwise, default to melee for player attacks
            return DamageType.MELEE;
        }
        
        // Fallback for unclassified damage
        return DamageType.UNKNOWN;
    }
}

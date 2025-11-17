package com.nonxedy.nonchat.core;

import com.nonxedy.nonchat.util.death.DamageRecord;
import com.nonxedy.nonchat.util.death.DamageType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages temporary storage of damage events to track potential indirect kills.
 * Uses Caffeine cache for automatic expiration and efficient memory management.
 */
public class IndirectDeathTracker {
    
    private final Cache<UUID, DamageRecord> damageCache;
    
    /**
     * Creates a new IndirectDeathTracker with default configuration:
     * - 10 second expiration window
     * - Maximum 1000 entries
     * - Expire after write policy
     * - Statistics recording enabled
     */
    public IndirectDeathTracker() {
        this.damageCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(1000)
                .recordStats()
                .build();
    }
    
    /**
     * Creates a new IndirectDeathTracker with custom expiration window.
     * 
     * @param expirationSeconds Number of seconds before entries expire
     */
    public IndirectDeathTracker(int expirationSeconds) {
        this.damageCache = Caffeine.newBuilder()
                .expireAfterWrite(expirationSeconds, TimeUnit.SECONDS)
                .maximumSize(1000)
                .recordStats()
                .build();
    }
    
    /**
     * Records a damage event for potential indirect death attribution.
     * 
     * @param victim The player who received damage
     * @param damager The player who caused the damage
     * @param type The type of damage inflicted
     */
    public void recordDamage(Player victim, Player damager, DamageType type) {
        if (victim == null || damager == null || type == null) {
            return;
        }
        
        DamageRecord record = new DamageRecord(
                damager.getUniqueId(),
                damager.getName(),
                type,
                System.currentTimeMillis()
        );
        
        damageCache.put(victim.getUniqueId(), record);
    }
    
    /**
     * Retrieves the last damager for a victim if the record is still valid.
     * Returns null if no recent damage was recorded or if the record has expired.
     * 
     * @param victim The player to check for recent damage
     * @return The damage record if found and valid, null otherwise
     */
    @Nullable
    public DamageRecord getLastDamager(Player victim) {
        if (victim == null) {
            return null;
        }
        
        return damageCache.getIfPresent(victim.getUniqueId());
    }
    
    /**
     * Clears tracking data for a specific player.
     * Should be called when a player logs out.
     * 
     * @param player The player whose tracking data should be cleared
     */
    public void clearPlayer(Player player) {
        if (player == null) {
            return;
        }
        
        damageCache.invalidate(player.getUniqueId());
    }
    
    /**
     * Clears all tracking data from the cache.
     * Should be called on plugin reload or disable.
     */
    public void clearAll() {
        damageCache.invalidateAll();
    }
    
    /**
     * Gets statistics about the cache for debugging and monitoring.
     * 
     * @return Map containing cache statistics (size, hit rate, evictions, etc.)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("size", damageCache.estimatedSize());
        stats.put("hitRate", damageCache.stats().hitRate());
        stats.put("missRate", damageCache.stats().missRate());
        stats.put("evictionCount", damageCache.stats().evictionCount());
        stats.put("hitCount", damageCache.stats().hitCount());
        stats.put("missCount", damageCache.stats().missCount());
        
        return stats;
    }
}

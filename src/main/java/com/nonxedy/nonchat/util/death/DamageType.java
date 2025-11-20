package com.nonxedy.nonchat.util.death;

import lombok.Getter;

/**
 * Represents the type of damage that was inflicted on a player.
 * Used to classify damage events for indirect death attribution.
 */
@Getter
public enum DamageType {
    /**
     * Direct melee attack (sword, axe, fist, etc.)
     */
    MELEE("melee"),
    
    /**
     * Projectile damage (arrow, snowball, trident, etc.)
     */
    PROJECTILE("projectile"),
    
    /**
     * Explosion damage (TNT, creeper, fireball, etc.)
     */
    EXPLOSION("explosion"),
    
    /**
     * Knockback-focused damage
     */
    KNOCKBACK("knockback"),
    
    /**
     * Unknown or unclassified damage type
     */
    UNKNOWN("unknown");
    
    private final String configKey;
    
    DamageType(String configKey) {
        this.configKey = configKey;
    }
}

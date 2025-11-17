package com.nonxedy.nonchat.util.death;

import lombok.Getter;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a death message with support for standard and indirect variants
 * Handles message selection based on damage type for indirect kills
 */
@Getter
public class DeathMessage {
    private final EntityDamageEvent.DamageCause cause;
    private final String standardMessage;
    private final boolean enabled;
    private final Map<DamageType, String> indirectVariants;
    private final String genericIndirectMessage;

    /**
     * Creates a standard death message (no indirect variants)
     * @param cause The death cause
     * @param message The message text
     * @param enabled Whether this message is enabled
     */
    public DeathMessage(EntityDamageEvent.DamageCause cause, String message, boolean enabled) {
        this.cause = cause;
        this.standardMessage = message;
        this.enabled = enabled;
        this.indirectVariants = new EnumMap<>(DamageType.class);
        this.genericIndirectMessage = null;
    }

    /**
     * Creates a death message with indirect variants
     * @param cause The death cause
     * @param standardMessage The standard message text
     * @param enabled Whether this message is enabled
     * @param indirectVariants Map of damage type to indirect message variants
     * @param genericIndirectMessage Generic indirect message (fallback)
     */
    public DeathMessage(EntityDamageEvent.DamageCause cause, String standardMessage, boolean enabled,
                       Map<DamageType, String> indirectVariants, String genericIndirectMessage) {
        this.cause = cause;
        this.standardMessage = standardMessage;
        this.enabled = enabled;
        this.indirectVariants = indirectVariants != null ? new EnumMap<>(indirectVariants) : new EnumMap<>(DamageType.class);
        this.genericIndirectMessage = genericIndirectMessage;
    }

    /**
     * Gets the appropriate message based on whether it's an indirect kill
     * @param isIndirect Whether this is an indirect kill
     * @param damageType The type of damage that caused the indirect kill (can be null)
     * @return The appropriate message text
     */
    public String getMessage(boolean isIndirect, DamageType damageType) {
        if (!isIndirect) {
            return standardMessage;
        }

        // Try to get damage-type-specific indirect message
        if (damageType != null && indirectVariants.containsKey(damageType)) {
            return indirectVariants.get(damageType);
        }

        // Fall back to generic indirect message
        if (genericIndirectMessage != null) {
            return genericIndirectMessage;
        }

        // Final fallback to standard message
        return standardMessage;
    }

    /**
     * Checks if this message has indirect variants configured
     * @return true if indirect variants exist
     */
    public boolean hasIndirectVariants() {
        return !indirectVariants.isEmpty() || genericIndirectMessage != null;
    }

    /**
     * Checks if this message has a specific damage type variant
     * @param damageType The damage type to check
     * @return true if a variant exists for this damage type
     */
    public boolean hasVariantForDamageType(DamageType damageType) {
        return damageType != null && indirectVariants.containsKey(damageType);
    }
}

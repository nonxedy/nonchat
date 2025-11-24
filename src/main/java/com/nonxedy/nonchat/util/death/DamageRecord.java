package com.nonxedy.nonchat.util.death;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Records information about a damage event for indirect death tracking.
 * Stores who caused the damage, when it occurred, and how it was inflicted.
 */
@Getter
@AllArgsConstructor
public class DamageRecord {
    /**
     * UUID of the player who caused the damage
     */
    private final UUID damagerUUID;
    
    /**
     * Cached name of the damager (for offline scenarios)
     */
    private final String damagerName;
    
    /**
     * Type of damage that was inflicted
     */
    private final DamageType damageType;
    
    /**
     * Timestamp when the damage occurred (milliseconds since epoch)
     */
    private final long timestamp;
    
    /**
     * Checks if this damage record is still valid based on the tracking window.
     * 
     * @param trackingWindowMs Tracking window in milliseconds
     * @return true if the record is within the tracking window, false otherwise
     */
    public boolean isValid(long trackingWindowMs) {
        return (System.currentTimeMillis() - timestamp) <= trackingWindowMs;
    }
    
    /**
     * Gets the age of this damage record in milliseconds.
     * 
     * @return Age in milliseconds since the damage occurred
     */
    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }
}

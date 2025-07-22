package com.nonxedy.nonchat.api;

/**
 * Interface for managing notification settings
 */
public interface INotificationSettings {
    
    /**
     * Checks if undelivered message notifications are enabled
     * @return true if notifications are enabled
     */
    boolean isUndeliveredMessageNotificationEnabled();
    
    /**
     * Sets undelivered message notification enabled state
     * @param enabled New enabled state
     */
    void setUndeliveredMessageNotificationEnabled(boolean enabled);
}
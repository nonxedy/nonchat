package com.nonxedy.nonchat.util.chat.filters;

/**
 * Filters excessive capitalization in chat messages
 * Controls and processes uppercase character usage
 */
public class CapsFilter {
    private final boolean enabled;
    private final int maxCapsPercentage;
    private final int minLength;

    // Constructor to initialize filter settings
    public CapsFilter(boolean enabled, int maxCapsPercentage, int minLength) {
        this.enabled = enabled;
        this.maxCapsPercentage = maxCapsPercentage;
        this.minLength = minLength;
    }

    /**
     * Determines if a message should be filtered for excessive caps
     * @param message The message to check
     * @return true if message exceeds caps limit, false otherwise
     */
    public boolean shouldFilter(String message) {
        // First strictly check if filter is disabled
        if (!this.enabled) {
            return false;
        }
        
        // Then check message length
        if (message.length() < this.minLength) {
            return false;
        }

        int capsCount = 0;
        for (char c : message.toCharArray()) {
            if (Character.isUpperCase(c)) {
                capsCount++;
            }
        }

        double percentage = (double) capsCount / message.length() * 100;
        return percentage > this.maxCapsPercentage;
    }

    /**
     * Converts a message to lowercase to reduce capitalization
     * @param message The message to filter
     * @return Filtered message in lowercase
     */
    public String filterMessage(String message) {
        return message.toLowerCase();
    }

    /**
     * Checks if caps filtering is enabled
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Gets the maximum percentage of caps allowed
     * @return Maximum caps percentage
     */
    public int getMaxCapsPercentage() {
        return maxCapsPercentage;
    }
    
    /**
     * Gets the minimum message length for caps checking
     * @return Minimum message length
     */
    public int getMinLength() {
        return minLength;
    }
}

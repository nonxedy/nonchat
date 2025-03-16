package com.nonxedy.nonchat.util;

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
        if (!enabled || message.length() < minLength) {
            return false;
        }

        int capsCount = 0;
        for (char c : message.toCharArray()) {
            if (Character.isUpperCase(c)) {
                capsCount++;
            }
        }

        double percentage = (double) capsCount / message.length() * 100;
        return percentage > maxCapsPercentage;
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
}

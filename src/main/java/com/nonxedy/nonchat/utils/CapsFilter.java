package com.nonxedy.nonchat.utils;

public class CapsFilter {
    private final boolean enabled;
    private final int maxCapsPercentage;
    private final int minLength;

    public CapsFilter(boolean enabled, int maxCapsPercentage, int minLength) {
        this.enabled = enabled;
        this.maxCapsPercentage = maxCapsPercentage;
        this.minLength = minLength;
    }

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

    public String filterMessage(String message) {
        return message.toLowerCase();
    }

    public boolean isEnabled() {
        return enabled;
    }
}

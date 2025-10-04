package com.nonxedy.nonchat.util.chat.filters;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Handles message filtering by checking for banned words and regex patterns
 * Provides functionality to block messages containing prohibited content
 */
@Data
@AllArgsConstructor
public class WordBlocker {
    /** List of words that are not allowed in messages */
    private List<String> bannedWords;

    /** List of regex patterns for advanced filtering */
    private List<String> bannedPatterns;

    /**
     * Checks if a message is allowed by scanning for banned words and patterns
     * @param message The message to check
     * @return true if message is allowed, false if it contains banned content
     */
    public boolean isMessageAllowed(String message) {
        String lowerMessage = message.toLowerCase();

        // Check banned words (case-insensitive)
        for (String word : bannedWords) {
            if (lowerMessage.contains(word.toLowerCase())) {
                return false;
            }
        }

        // Check regex patterns (case-insensitive)
        for (String pattern : bannedPatterns) {
            try {
                Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                if (regex.matcher(message).find()) {
                    return false;
                }
            } catch (PatternSyntaxException e) {
                // Log invalid regex pattern but don't crash
                System.err.println("Invalid regex pattern in banned patterns: " + pattern);
            }
        }

        return true;
    }

    /**
     * Checks if a message is allowed by scanning for banned words (legacy method)
     * @param message The message to check
     * @return true if message is allowed, false if it contains banned words
     */
    public boolean isMessageAllowedLegacy(String message) {
        for (String word : bannedWords) {
            if (message.toLowerCase().contains(word.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}

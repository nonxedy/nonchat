package com.nonxedy.nonchat.util.chat.filters;

import java.util.List;

/**
 * Handles message filtering by checking for banned words
 * Provides functionality to block messages containing prohibited content
 */
public class WordBlocker {
    /** List of words that are not allowed in messages */
    private List<String> bannedWords;

    // Constructor to initialize banned words
    public WordBlocker(List<String> bannedWords) {
        this.bannedWords = bannedWords;
    }

    /**
     * Checks if a message is allowed by scanning for banned words
     * @param message The message to check
     * @return true if message is allowed, false if it contains banned words
     */
    public boolean isMessageAllowed(String message) {
        for (String word : bannedWords) {
            if (message.toLowerCase().contains(word.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
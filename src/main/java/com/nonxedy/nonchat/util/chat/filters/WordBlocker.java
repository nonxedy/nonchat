package com.nonxedy.nonchat.util.chat.filters;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Handles message filtering by checking for banned words
 * Provides functionality to block messages containing prohibited content
 */
@Data
@AllArgsConstructor
public class WordBlocker {
    /** List of words that are not allowed in messages */
    private List<String> bannedWords;

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

package com.nonxedy.nonchat.utils;

import java.util.List;

// WordBlocker class to check if a message contains banned words
public class WordBlocker {
    private List<String> bannedWords;

    // Constructor to initialize banned words
    public WordBlocker(List<String> bannedWords) {
        this.bannedWords = bannedWords;
    }

    // Method to check if a message is allowed (does not contain banned words)
    public boolean isMessageAllowed(String message) {
        for (String word : bannedWords) {
            if (message.toLowerCase().contains(word.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
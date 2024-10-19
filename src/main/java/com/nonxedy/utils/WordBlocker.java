package com.nonxedy.utils;

import java.util.List;

public class WordBlocker {
    private List<String> bannedWords;

    public WordBlocker(List<String> bannedWords) {
        this.bannedWords = bannedWords;
    }

    public boolean isMessageAllowed(String message) {
        for (String word : bannedWords) {
            if (message.toLowerCase().contains(word.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
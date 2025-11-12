package com.nonxedy.nonchat.util.chat.filters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Stores message history for a player
 * Tracks messages with timestamps for spam detection
 */
@Data
@AllArgsConstructor
public class MessageHistory {
    private UUID playerUuid;
    private List<MessageEntry> messages;

    public MessageHistory(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.messages = new ArrayList<>();
    }

    /**
     * Adds a new message to the history
     * @param message The message content
     * @param timestamp The timestamp when the message was sent
     */
    public void addMessage(String message, long timestamp) {
        messages.add(new MessageEntry(message, timestamp));
    }

    /**
     * Gets all messages within a time window
     * @param timeWindowSeconds Time window in seconds
     * @return List of messages within the time window
     */
    public List<MessageEntry> getMessagesInTimeWindow(int timeWindowSeconds) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - (timeWindowSeconds * 1000L);
        List<MessageEntry> result = new ArrayList<>();

        for (MessageEntry entry : messages) {
            if (entry.getTimestamp() >= windowStart) {
                result.add(entry);
            }
        }

        return result;
    }

    /**
     * Cleans up old messages outside the maximum time window
     * @param maxTimeWindowSeconds Maximum time window to keep messages for
     */
    public void cleanupOldMessages(int maxTimeWindowSeconds) {
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - (maxTimeWindowSeconds * 1000L);

        messages.removeIf(entry -> entry.getTimestamp() < cutoffTime);
    }

    /**
     * Gets the count of messages within a time window
     * @param timeWindowSeconds Time window in seconds
     * @return Number of messages in the time window
     */
    public int getMessageCountInTimeWindow(int timeWindowSeconds) {
        return getMessagesInTimeWindow(timeWindowSeconds).size();
    }

    /**
     * Represents a single message entry with timestamp
     */
    @Data
    @AllArgsConstructor
    public static class MessageEntry {
        private String message;
        private long timestamp;
    }
}


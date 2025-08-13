package com.nonxedy.nonchat.util.core.broadcast;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Manages automated broadcast messages with timing control
 * Handles configuration and delivery of scheduled announcements
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastMessage {
    private boolean enabled;
    private String message;
    private int interval;
}

package com.nonxedy.nonchat.utils;

import com.nonxedy.nonchat.config.PluginMessages;

import net.kyori.adventure.text.Component;

// MessageFormatter class handles formatting of plugin messages with color support
public class MessageFormatter {
    
    // Reference to plugin messages configuration
    private final PluginMessages messages;

    // Constructor initializes the formatter with plugin messages
    public MessageFormatter(PluginMessages messages) {
        this.messages = messages;
    }

    // Formats a message by key with optional arguments
    // key - message identifier in config
    // args - variable arguments to insert into the message
    // returns formatted Component with colors parsed
    public Component format(String key, Object... args) {
        // Get raw message string from config using key
        String message = messages.getString(key);
        // Return empty component if message not found
        if (message == null) {
            return Component.empty();
        }

        // Insert arguments into message template
        message = String.format(message, args);
        // Parse colors and return as Component
        return ColorUtil.parseComponent(message);
    }
}

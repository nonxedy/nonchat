package com.nonxedy.nonchat.utils;

import com.nonxedy.nonchat.config.PluginMessages;

import net.kyori.adventure.text.Component;

public class MessageFormatter {
    
    private final PluginMessages messages;

    public MessageFormatter(PluginMessages messages) {
        this.messages = messages;
    }

    public Component format(String key, Object... args) {
        String message = messages.getString(key);
        if (message == null) {
            return Component.empty();
        }

        message = String.format(message, args);
        return ColorUtil.parseComponent(message);
    }
}

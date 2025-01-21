package com.nonxedy.nonchat.config;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.utils.LanguageManager;
import com.nonxedy.nonchat.utils.MessageFormatter;

import net.kyori.adventure.text.Component;

// Main class for handling plugin messages and configurations
public class PluginMessages {
    private final MessageFormatter formatter;
    private final LanguageManager languageManager;
    private final nonchat plugin;

    // Constructor initializes the messages system
    public PluginMessages(nonchat plugin) {
        this.plugin = plugin;
        this.languageManager = new LanguageManager(plugin.getDataFolder());
        this.formatter = new MessageFormatter(this);
        loadLanguage();
    }

    // Loads language from configuration
    public void loadLanguage() {
        String lang = plugin.getConfig().getString("language", "en");
        languageManager.setLanguage(lang);
    }

    // Reloads configuration from file
    public void reloadConfig() {
        loadLanguage();
    }

    // Gets raw string from configuration
    public String getString(String path) {
        return languageManager.getMessage(path);
    }

    // Formats message with provided arguments using MessageFormatter
    public Component getFormatted(String path, Object... args) {
        return formatter.format(path, args);
    }
}

package com.nonxedy.nonchat.service;

import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.api.IConfigurable;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.util.lang.LanguageManager;

public class ConfigService implements IConfigurable {
    private final Nonchat plugin;
    private final PluginConfig config;
    private final PluginMessages messages;
    private final LanguageManager languageManager;

    public ConfigService(Nonchat plugin) {
        this.plugin = plugin;
        this.config = new PluginConfig();
        this.languageManager = new LanguageManager(plugin.getDataFolder());
        this.messages = new PluginMessages(plugin);
        load();
        this.messages.loadLanguage();
    }

    @Override
    public void load() {
        config.loadConfig();
        messages.loadLanguage();
    }

    @Override
    public void save() {
        config.saveConfig();
    }

    @Override
    public void reload() {
        //plugin.reloadConfig();
        config.reloadConfig();
        messages.reloadConfig();
    }

    public PluginConfig getConfig() {
        return config;
    }

    public PluginMessages getMessages() {
        return messages;
    }

    public String getMessage(String key) {
        return messages.getString(key);
    }

    public void logCommand(String command, String[] args) {
        if (config.isDebug()) {
            plugin.logCommand(command, args);
        }
    }

    public void logError(String error) {
        if (config.isDebug()) {
            plugin.logError(error);
        }
    }

    @Override
    public String getString(String path) {
        return config.getString(path);
    }

    @Override
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    @Override
    public int getInt(String path) {
        return config.getInt(path);
    }

    @Override
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    @Override
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    @Override
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    @Override
    public void set(String path, Object value) {
        config.set(path, value);
    }

    @Override
    public boolean contains(String path) {
        return config.contains(path);
    }

    @Override
    public ConfigurationSection getSection(String path) {
        return config.getConfigurationSection(path);
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        return config.getKeys(deep);
    }
}

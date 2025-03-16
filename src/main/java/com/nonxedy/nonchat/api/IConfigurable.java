package com.nonxedy.nonchat.api;

import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

public interface IConfigurable {
    void load();
    void save();
    void reload();
    
    String getString(String path);
    String getString(String path, String defaultValue);
    
    int getInt(String path);
    int getInt(String path, int defaultValue);
    
    boolean getBoolean(String path);
    boolean getBoolean(String path, boolean defaultValue);
    
    List<String> getStringList(String path);
    
    void set(String path, Object value);
    boolean contains(String path);
    
    ConfigurationSection getSection(String path);
    Set<String> getKeys(boolean deep);
}

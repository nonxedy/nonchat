package com.nonxedy.nonchat.utils;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Debugger {

    private File file;
    private FileConfiguration config;

    public Debugger() {
        file = new File("plugins/nonchat", "debug.yml");
        if (!file.exists()) {
            createDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void createDefaultConfig() {
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();

            config = new YamlConfiguration();
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String message) {
        try {
            config.set("log", config.getString("log") + "\n" + message);
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
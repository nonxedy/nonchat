package com.nonxedy.nonchat.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

// Language manager class for handling language files and translations
public class LanguageManager {

    // Initialize class variables
    private final File langsFolder;
    private FileConfiguration currentLang;
    private final Map<String, FileConfiguration> loadedLanguages;
    
    // Constructor initializes language folder and loads default languages
    public LanguageManager(File dataFolder) {
        this.langsFolder = new File(dataFolder, "langs");
        this.loadedLanguages = new HashMap<>();
        setupLanguages();
    }
    
    // Create default language files if they don't exist
    private void setupLanguages() {
        if (!langsFolder.exists()) {
            langsFolder.mkdirs();
        }
        
        // Create default language files if they don't exist
        createLanguageFile("messages_en.yml");
        createLanguageFile("messages_ru.yml");
        
        // Load all language files
        for (File file : langsFolder.listFiles()) {
            if (file.getName().startsWith("messages_") && file.getName().endsWith(".yml")) {
                String langCode = file.getName().replace("messages_", "").replace(".yml", "");
                loadedLanguages.put(langCode, YamlConfiguration.loadConfiguration(file));
            }
        }
    }
    
    // Create a new language file if it doesn't exist
    private void createLanguageFile(String filename) {
        File langFile = new File(langsFolder, filename);
        if (!langFile.exists()) {
            // Copy default language file from resources to the language folder
            try (InputStream in = getClass().getResourceAsStream("/langs/" + filename)) {
                Files.copy(in, langFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Set the current language and load its configuration
    public void setLanguage(String lang) {
        currentLang = loadedLanguages.getOrDefault(lang, loadedLanguages.get("en"));
    }

    // Get a message from the current language configuration
    public String getMessage(String key) {
        return ColorUtil.parseColor(currentLang.getString(key, "Missing message: " + key));
    }
}

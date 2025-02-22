package com.nonxedy.nonchat.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

// Manages language files and provides methods to load and get translations
public class LanguageManager {

    // Directory containing all language files
    private final File langsFolder;
    // Currently active language configuration
    private FileConfiguration currentLang;
    // Map of language codes to their corresponding configurations
    private final Map<String, FileConfiguration> loadedLanguages;
    
    /**
     * Initializes the language manager and sets up default languages
     * @param dataFolder The plugin's data folder where language files will be stored
     */
    public LanguageManager(File dataFolder) {
        this.langsFolder = new File(dataFolder, "langs");
        this.loadedLanguages = new HashMap<>();
        setupLanguages();
    }
    
    /**
     * Creates the language directory and default language files
     * Loads all available language files into memory
     */
    private void setupLanguages() {
        if (!langsFolder.exists()) {
            langsFolder.mkdirs();
        }
        
        // Create default language files if they don't exist
        createLanguageFile("messages_en.yml");
        createLanguageFile("messages_ru.yml");
        
        // Load all language files from the directory
        for (File file : langsFolder.listFiles()) {
            if (file.getName().startsWith("messages_") && file.getName().endsWith(".yml")) {
                String langCode = file.getName().replace("messages_", "").replace(".yml", "");
                loadedLanguages.put(langCode, YamlConfiguration.loadConfiguration(file));
            }
        }
    }
    
    /**
     * Creates a new language file by copying it from plugin resources
     * @param filename Name of the language file to create
     */
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
    
    /**
     * Sets the active language for message retrieval
     * Falls back to English if specified language is not found
     * @param lang Language code to set as active
     */
    public void setLanguage(String lang) {
        currentLang = loadedLanguages.getOrDefault(lang, loadedLanguages.get("en"));
    }

    /**
     * Retrieves a colored message from the current language configuration
     * @param key The message key to retrieve
     * @return The colored message string, or an error message if key not found
     */
    public String getMessage(String key) {
        return ColorUtil.parseColor(currentLang.getString(key, "Missing message: " + key));
    }
}

package com.nonxedy.nonchat.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.configuration.ConfigurationSection;

import com.nonxedy.nonchat.util.core.colors.ColorUtil;
import com.nonxedy.nonchat.util.core.debugging.Debugger;
import com.nonxedy.nonchat.util.death.DamageType;
import com.nonxedy.nonchat.util.death.DeathMessage;

/**
 * Loads and validates death messages from deaths.yml
 */
public class DeathMessageLoader {
    private final File deathsFile;
    private final Debugger debugger;
    private final DeathConfig deathConfig;

    /**
     * Creates a new DeathMessageLoader
     * @param dataFolder Plugin data folder
     * @param debugger Debug logger instance
     * @param deathConfig Death configuration instance
     */
    public DeathMessageLoader(File dataFolder, Debugger debugger, DeathConfig deathConfig) {
        this.deathsFile = new File(dataFolder, "deaths.yml");
        this.debugger = debugger;
        this.deathConfig = deathConfig;
    }

    /**
     * Loads all death messages from deaths.yml
     * @return Map of death cause key to list of death messages
     */
    public Map<String, List<DeathMessage>> loadAllMessages() {
        Map<String, List<DeathMessage>> messages = new HashMap<>();
        
        try {
            // Check if file exists
            if (!deathsFile.exists()) {
                if (debugger != null) {
                    debugger.warn("DeathMessageLoader", "deaths.yml not found at " + deathsFile.getAbsolutePath() + ". Creating default file...");
                }
                createDefaultFile();
                // Reload config after creating default file
                deathConfig.reload();
            }

            // Validate configuration is loaded
            if (deathConfig.getConfig() == null) {
                if (debugger != null) {
                    debugger.error("DeathMessageLoader", "Death configuration is null, cannot load messages", null);
                }
                return messages;
            }

            ConfigurationSection messagesSection = deathConfig.getConfig().getConfigurationSection("messages");
            if (messagesSection == null) {
                if (debugger != null) {
                    debugger.warn("DeathMessageLoader", "No 'messages' section found in deaths.yml at " + deathsFile.getAbsolutePath());
                    debugger.warn("DeathMessageLoader", "Please ensure the file has a 'messages:' section with death cause configurations");
                }
                return messages;
            }

            int totalLoaded = 0;
            int totalSkipped = 0;
            int unknownCauses = 0;
            int emptyVariants = 0;

            // Iterate through each death cause in the config
            for (String causeKey : messagesSection.getKeys(false)) {
                try {
                    // Normalize the cause key (lowercase with underscores)
                    String normalizedKey = causeKey.toLowerCase().replace('-', '_');

                    ConfigurationSection causeSection = messagesSection.getConfigurationSection(causeKey);
                    if (causeSection == null) {
                        if (debugger != null) {
                            debugger.warn("DeathMessageLoader", "Invalid configuration structure for death cause '" + causeKey + "' in deaths.yml");
                        }
                        totalSkipped++;
                        continue;
                    }

                    boolean enabled = causeSection.getBoolean("enabled", true);
                    List<String> variants = null;

                    try {
                        variants = causeSection.getStringList("variants");
                    } catch (Exception e) {
                        if (debugger != null) {
                            debugger.warn("DeathMessageLoader", "Failed to read variants for death cause '" + causeKey + "': " + e.getMessage());
                        }
                        totalSkipped++;
                        continue;
                    }

                    if (variants == null || variants.isEmpty()) {
                        if (deathConfig.isDebugEnabled() && debugger != null) {
                            debugger.warn("DeathMessageLoader", "No message variants found for death cause: " + causeKey + " in " + deathsFile.getAbsolutePath());
                        }
                        totalSkipped++;
                        continue;
                    }

                    // Load indirect variants if they exist
                    Map<DamageType, List<String>> indirectVariantsByType = 
                        loadIndirectVariants(causeSection, causeKey);

                    // Create DeathMessage objects for each variant
                    List<DeathMessage> deathMessages = new ArrayList<>();
                    int variantIndex = 0;
                    for (String variant : variants) {
                        variantIndex++;
                        // Validate that message variant is not empty
                        if (validateMessage(variant)) {
                            // Translate color codes from & to §
                            String translatedVariant = ColorUtil.parseColor(variant);
                            
                            // Create message with indirect variants if available
                            if (!indirectVariantsByType.isEmpty()) {
                                // Select one random indirect variant per damage type for this standard variant
                                Map<DamageType, String> indirectVariants = 
                                    selectIndirectVariants(indirectVariantsByType);
                                String genericIndirect = indirectVariants.get(DamageType.UNKNOWN);
                                indirectVariants.remove(DamageType.UNKNOWN);
                                
                                // Translate indirect variants
                                Map<DamageType, String> translatedIndirectVariants = 
                                    new HashMap<>();
                                for (Map.Entry<DamageType, String> entry : indirectVariants.entrySet()) {
                                    translatedIndirectVariants.put(entry.getKey(), ColorUtil.parseColor(entry.getValue()));
                                }
                                String translatedGenericIndirect = genericIndirect != null ? ColorUtil.parseColor(genericIndirect) : null;
                                
                                deathMessages.add(new DeathMessage(normalizedKey, translatedVariant, enabled, translatedIndirectVariants, translatedGenericIndirect));
                            } else {
                                deathMessages.add(new DeathMessage(normalizedKey, translatedVariant, enabled));
                            }
                            totalLoaded++;
                        } else {
                            if (deathConfig.isDebugEnabled() && debugger != null) {
                                debugger.warn("DeathMessageLoader", "Empty or null message variant #" + variantIndex + " found for cause '" + causeKey + "' in " + deathsFile.getAbsolutePath() + " - skipping");
                            }
                            emptyVariants++;
                            totalSkipped++;
                        }
                    }

                    if (!deathMessages.isEmpty()) {
                        messages.put(normalizedKey, deathMessages);
                    } else {
                        if (debugger != null) {
                            debugger.warn("DeathMessageLoader", "Death cause '" + causeKey + "' has no valid message variants after validation");
                        }
                    }

                } catch (Exception e) {
                    if (debugger != null) {
                        debugger.error("DeathMessageLoader", "Error processing death cause '" + causeKey + "' in deaths.yml: " + e.getMessage(), e);
                    }
                    totalSkipped++;
                }
            }

            if (debugger != null) {
                debugger.info("DeathMessageLoader", "Loaded " + totalLoaded + " death message variants across " + messages.size() + " causes from " + deathsFile.getName());
                if (totalSkipped > 0) {
                    debugger.info("DeathMessageLoader", "Skipped " + totalSkipped + " invalid entries (" + unknownCauses + " unknown causes, " + emptyVariants + " empty variants)");
                }
            }

        } catch (Exception e) {
            if (debugger != null) {
                debugger.error("DeathMessageLoader", "Critical error loading death messages from " + deathsFile.getAbsolutePath() + ": " + e.getMessage(), e);
            }
        }

        return messages;
    }

    /**
     * Creates deaths.yml with default examples if it doesn't exist
     */
    public void createDefaultFile() {
        FileWriter writer = null;
        try {
            // Create parent directory if it doesn't exist
            File parentDir = deathsFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    if (debugger != null) {
                        debugger.error("DeathMessageLoader", "Failed to create parent directory for deaths.yml at " + parentDir.getAbsolutePath(), null);
                    }
                    return;
                }
            }

            // Create the file with default content
            if (deathsFile.createNewFile()) {
                if (debugger != null) {
                    debugger.info("DeathMessageLoader", "Creating default deaths.yml file at " + deathsFile.getAbsolutePath());
                }

                // Write default content
                writer = new FileWriter(deathsFile);
                writer.write(getDefaultFileContent());
                writer.flush();

                if (debugger != null) {
                    debugger.info("DeathMessageLoader", "Default deaths.yml created successfully with example messages for all death causes");
                }
            } else {
                if (debugger != null) {
                    debugger.info("DeathMessageLoader", "deaths.yml already exists at " + deathsFile.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            if (debugger != null) {
                debugger.error("DeathMessageLoader", "Failed to create default deaths.yml at " + deathsFile.getAbsolutePath() + ": " + e.getMessage(), e);
                debugger.error("DeathMessageLoader", "Please check file permissions and disk space", null);
            }
        } catch (Exception e) {
            if (debugger != null) {
                debugger.error("DeathMessageLoader", "Unexpected error creating default deaths.yml: " + e.getMessage(), e);
            }
        } finally {
            // Ensure writer is closed even if an error occurs
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    if (debugger != null) {
                        debugger.warn("DeathMessageLoader", "Failed to close file writer for deaths.yml: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Gets the default content for deaths.yml
     * @return Default YAML content as string
     */
    private String getDefaultFileContent() {
        try {
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("deaths.yml");
            if (resourceStream == null) {
                if (debugger != null) {
                    debugger.error("DeathMessageLoader", "Could not find deaths.yml in plugin resources", null);
                }
                return "";
            }

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            reader.close();
            return content.toString();

        } catch (IOException e) {
            if (debugger != null) {
                debugger.error("DeathMessageLoader", "Failed to read deaths.yml from plugin resources: " + e.getMessage(), e);
            }
            return "";
        }
    }

    /**
     * Loads indirect message variants from configuration
     * Supports damage-type-specific variants and generic indirect messages
     * 
     * @param causeSection Configuration section for the death cause
     * @param causeKey The death cause key for logging
     * @return Map of damage type to list of indirect message variants
     */
    private Map<DamageType, List<String>> loadIndirectVariants(
            ConfigurationSection causeSection, String causeKey) {
        Map<DamageType, List<String>> indirectVariants = 
            new HashMap<>();
        
        try {
            ConfigurationSection indirectSection = causeSection.getConfigurationSection("indirect-variants");
            if (indirectSection == null) {
                // No indirect variants configured
                return indirectVariants;
            }
            
            // Load generic indirect message (fallback)
            if (indirectSection.contains("generic")) {
                List<String> genericVariants = indirectSection.getStringList("generic");
                if (genericVariants != null && !genericVariants.isEmpty()) {
                    List<String> validGeneric = new ArrayList<>();
                    for (String variant : genericVariants) {
                        if (validateMessage(variant)) {
                            validGeneric.add(variant);
                        }
                    }
                    if (!validGeneric.isEmpty()) {
                        indirectVariants.put(DamageType.UNKNOWN, validGeneric);
                    }
                }
            }
            
            // Load damage-type-specific variants
            for (DamageType damageType : DamageType.values()) {
                if (damageType == DamageType.UNKNOWN) {
                    continue; // Already handled as generic
                }
                
                String configKey = damageType.getConfigKey();
                if (indirectSection.contains(configKey)) {
                    List<String> typeVariants = indirectSection.getStringList(configKey);
                    if (typeVariants != null && !typeVariants.isEmpty()) {
                        List<String> validVariants = new ArrayList<>();
                        for (String variant : typeVariants) {
                            if (validateMessage(variant)) {
                                validVariants.add(variant);
                            }
                        }
                        if (!validVariants.isEmpty()) {
                            indirectVariants.put(damageType, validVariants);
                        }
                    }
                }
            }
            
            if (deathConfig.isDebugEnabled() && !indirectVariants.isEmpty() && debugger != null) {
                debugger.debug("DeathMessageLoader", "Loaded " + indirectVariants.size() + " indirect variant types for cause: " + causeKey);
            }

            // Validate that at least one indirect variant exists
            if (indirectVariants.isEmpty() && indirectSection != null) {
                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.warn("DeathMessageLoader", "indirect-variants section exists for '" + causeKey + "' but no valid variants were loaded");
                }
            }

        } catch (Exception e) {
            if (debugger != null) {
                debugger.error("DeathMessageLoader", "Error loading indirect variants for cause '" + causeKey + "': " + e.getMessage(), e);
            }
        }
        
        return indirectVariants;
    }

    /**
     * Selects one random indirect variant per damage type
     * Creates a map of damage type to a single selected message
     * 
     * @param indirectVariantsByType Map of damage type to list of variants
     * @return Map of damage type to selected message
     */
    private Map<DamageType, String> selectIndirectVariants(
            Map<DamageType, List<String>> indirectVariantsByType) {
        Map<DamageType, String> selected = 
            new HashMap<>();
        
        try {
            for (Map.Entry<DamageType, List<String>> entry : indirectVariantsByType.entrySet()) {
                List<String> variants = entry.getValue();
                if (variants != null && !variants.isEmpty()) {
                    // Select random variant from the list
                    int randomIndex = ThreadLocalRandom.current().nextInt(variants.size());
                    selected.put(entry.getKey(), variants.get(randomIndex));
                }
            }
        } catch (Exception e) {
            if (debugger != null) {
                debugger.error("DeathMessageLoader", "Error selecting indirect variants: " + e.getMessage(), e);
            }
        }
        
        return selected;
    }

    /**
     * Validates a death message configuration
     * Checks for null, empty, or whitespace-only messages
     * 
     * @param message The message to validate
     * @return true if valid, false otherwise
     */
    private boolean validateMessage(String message) {
        try {
            // Check if message is null
            if (message == null) {
                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.warn("DeathMessageLoader", "Null message variant detected during validation");
                }
                return false;
            }

            // Trim whitespace and check if empty
            String trimmed = message.trim();
            if (trimmed.isEmpty()) {
                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.warn("DeathMessageLoader", "Empty or whitespace-only message variant detected during validation");
                }
                return false;
            }

            // Additional validation: check minimum length
            if (trimmed.length() < 3) {
                if (deathConfig.isDebugEnabled() && debugger != null) {
                    debugger.warn("DeathMessageLoader", "Message variant too short (less than 3 characters): '" + trimmed + "'");
                }
                return false;
            }

            return true;

        } catch (Exception e) {
            if (debugger != null) {
                debugger.warn("DeathMessageLoader", "Error validating message variant: " + e.getMessage());
            }
            return false;
        }
    }
}

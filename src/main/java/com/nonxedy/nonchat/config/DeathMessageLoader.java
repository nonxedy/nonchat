package com.nonxedy.nonchat.config;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nonxedy.nonchat.util.death.DeathMessage;

/**
 * Loads and validates death messages from deaths.yml
 */
public class DeathMessageLoader {
    private final File deathsFile;
    private final Logger logger;
    private final DeathConfig deathConfig;

    /**
     * Creates a new DeathMessageLoader
     * @param dataFolder Plugin data folder
     * @param logger Plugin logger
     * @param deathConfig Death configuration instance
     */
    public DeathMessageLoader(File dataFolder, Logger logger, DeathConfig deathConfig) {
        this.deathsFile = new File(dataFolder, "deaths.yml");
        this.logger = logger;
        this.deathConfig = deathConfig;
    }

    /**
     * Loads all death messages from deaths.yml
     * @return Map of death cause to list of death messages
     */
    public Map<EntityDamageEvent.DamageCause, List<DeathMessage>> loadAllMessages() {
        Map<EntityDamageEvent.DamageCause, List<DeathMessage>> messages = new EnumMap<>(EntityDamageEvent.DamageCause.class);
        
        try {
            // Check if file exists
            if (!deathsFile.exists()) {
                logger.warning("deaths.yml not found at " + deathsFile.getAbsolutePath() + ". Creating default file...");
                createDefaultFile();
                // Reload config after creating default file
                deathConfig.reload();
            }

            // Validate configuration is loaded
            if (deathConfig.getConfig() == null) {
                logger.severe("Death configuration is null, cannot load messages");
                return messages;
            }

            ConfigurationSection messagesSection = deathConfig.getConfig().getConfigurationSection("messages");
            if (messagesSection == null) {
                logger.warning("No 'messages' section found in deaths.yml at " + deathsFile.getAbsolutePath());
                logger.warning("Please ensure the file has a 'messages:' section with death cause configurations");
                return messages;
            }

            int totalLoaded = 0;
            int totalSkipped = 0;
            int unknownCauses = 0;
            int emptyVariants = 0;

            // Iterate through each death cause in the config
            for (String causeKey : messagesSection.getKeys(false)) {
                try {
                    // Validate death cause against Minecraft's DamageCause enum
                    EntityDamageEvent.DamageCause cause;
                    try {
                        cause = EntityDamageEvent.DamageCause.valueOf(causeKey.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        logger.warning("Unknown death cause '" + causeKey + "' in deaths.yml at " + deathsFile.getAbsolutePath());
                        logger.warning("Valid death causes are: " + getValidDeathCauses());
                        unknownCauses++;
                        totalSkipped++;
                        continue;
                    }

                    ConfigurationSection causeSection = messagesSection.getConfigurationSection(causeKey);
                    if (causeSection == null) {
                        logger.warning("Invalid configuration structure for death cause '" + causeKey + "' in deaths.yml");
                        totalSkipped++;
                        continue;
                    }

                    boolean enabled = causeSection.getBoolean("enabled", true);
                    List<String> variants = null;
                    
                    try {
                        variants = causeSection.getStringList("variants");
                    } catch (Exception e) {
                        logger.warning("Failed to read variants for death cause '" + causeKey + "': " + e.getMessage());
                        totalSkipped++;
                        continue;
                    }

                    if (variants == null || variants.isEmpty()) {
                        if (deathConfig.isDebugEnabled()) {
                            logger.warning("No message variants found for death cause: " + causeKey + " in " + deathsFile.getAbsolutePath());
                        }
                        totalSkipped++;
                        continue;
                    }

                    // Load indirect variants if they exist
                    Map<com.nonxedy.nonchat.util.death.DamageType, List<String>> indirectVariantsByType = 
                        loadIndirectVariants(causeSection, causeKey);

                    // Create DeathMessage objects for each variant
                    List<DeathMessage> deathMessages = new ArrayList<>();
                    int variantIndex = 0;
                    for (String variant : variants) {
                        variantIndex++;
                        // Validate that message variant is not empty
                        if (validateMessage(variant)) {
                            // Create message with indirect variants if available
                            if (!indirectVariantsByType.isEmpty()) {
                                // Select one random indirect variant per damage type for this standard variant
                                Map<com.nonxedy.nonchat.util.death.DamageType, String> indirectVariants = 
                                    selectIndirectVariants(indirectVariantsByType);
                                String genericIndirect = indirectVariants.get(com.nonxedy.nonchat.util.death.DamageType.UNKNOWN);
                                indirectVariants.remove(com.nonxedy.nonchat.util.death.DamageType.UNKNOWN);
                                
                                deathMessages.add(new DeathMessage(cause, variant, enabled, indirectVariants, genericIndirect));
                            } else {
                                deathMessages.add(new DeathMessage(cause, variant, enabled));
                            }
                            totalLoaded++;
                        } else {
                            if (deathConfig.isDebugEnabled()) {
                                logger.warning("Empty or null message variant #" + variantIndex + " found for cause '" + causeKey + "' in " + deathsFile.getAbsolutePath() + " - skipping");
                            }
                            emptyVariants++;
                            totalSkipped++;
                        }
                    }

                    if (!deathMessages.isEmpty()) {
                        messages.put(cause, deathMessages);
                    } else {
                        logger.warning("Death cause '" + causeKey + "' has no valid message variants after validation");
                    }
                    
                } catch (Exception e) {
                    logger.warning("Error processing death cause '" + causeKey + "' in deaths.yml: " + e.getMessage());
                    if (deathConfig.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                    totalSkipped++;
                }
            }

            logger.info("Loaded " + totalLoaded + " death message variants across " + messages.size() + " causes from " + deathsFile.getName());
            if (totalSkipped > 0) {
                logger.info("Skipped " + totalSkipped + " invalid entries (" + unknownCauses + " unknown causes, " + emptyVariants + " empty variants)");
            }

        } catch (Exception e) {
            logger.severe("Critical error loading death messages from " + deathsFile.getAbsolutePath() + ": " + e.getMessage());
            if (deathConfig.isDebugEnabled()) {
                e.printStackTrace();
            }
        }

        return messages;
    }



    /**
     * Gets a comma-separated list of valid death causes for error messages
     * @return String of valid death causes
     */
    private String getValidDeathCauses() {
        StringBuilder causes = new StringBuilder();
        EntityDamageEvent.DamageCause[] allCauses = EntityDamageEvent.DamageCause.values();
        for (int i = 0; i < allCauses.length; i++) {
            if (i > 0) causes.append(", ");
            causes.append(allCauses[i].name());
            // Limit output to avoid too long messages
            if (i >= 9) {
                causes.append(", ... (and ").append(allCauses.length - 10).append(" more)");
                break;
            }
        }
        return causes.toString();
    }

    /**
     * Creates deaths.yml with default examples if it doesn't exist
     */
    public void createDefaultFile() {
        java.io.FileWriter writer = null;
        try {
            // Create parent directory if it doesn't exist
            File parentDir = deathsFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    logger.severe("Failed to create parent directory for deaths.yml at " + parentDir.getAbsolutePath());
                    return;
                }
            }

            // Create the file with default content
            if (deathsFile.createNewFile()) {
                logger.info("Creating default deaths.yml file at " + deathsFile.getAbsolutePath());
                
                // Write default content
                writer = new java.io.FileWriter(deathsFile);
                writer.write(getDefaultFileContent());
                writer.flush();
                
                logger.info("Default deaths.yml created successfully with example messages for all death causes");
            } else {
                logger.info("deaths.yml already exists at " + deathsFile.getAbsolutePath());
            }
        } catch (java.io.IOException e) {
            logger.severe("Failed to create default deaths.yml at " + deathsFile.getAbsolutePath() + ": " + e.getMessage());
            logger.severe("Please check file permissions and disk space");
            if (deathConfig.isDebugEnabled()) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            logger.severe("Unexpected error creating default deaths.yml: " + e.getMessage());
            if (deathConfig.isDebugEnabled()) {
                e.printStackTrace();
            }
        } finally {
            // Ensure writer is closed even if an error occurs
            if (writer != null) {
                try {
                    writer.close();
                } catch (java.io.IOException e) {
                    logger.warning("Failed to close file writer for deaths.yml: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Gets the default content for deaths.yml
     * @return Default YAML content as string
     */
    private String getDefaultFileContent() {
        StringBuilder content = new StringBuilder();
        
        // Header comments
        content.append("#========================================\n");
        content.append("# nonchat - Custom Death Messages\n");
        content.append("#========================================\n");
        content.append("#\n");
        content.append("# This file allows you to customize death messages based on the cause of death.\n");
        content.append("# Each death cause can have multiple message variants that are randomly selected.\n");
        content.append("#\n");
        content.append("# Available Placeholders:\n");
        content.append("#   PlaceholderAPI placeholders:\n");
        content.append("#     %player_name% - The player's name\n");
        content.append("#     %luckperms_prefix% - Player's LuckPerms prefix\n");
        content.append("#     %luckperms_suffix% - Player's LuckPerms suffix\n");
        content.append("#     (Any other PlaceholderAPI placeholder)\n");
        content.append("#\n");
        content.append("#   Death-specific placeholders:\n");
        content.append("#     {death_cause} - The cause of death (e.g., FALL, FIRE)\n");
        content.append("#     {killer_name} - Name of the killer (if applicable)\n");
        content.append("#     {killer_type} - Type of killer (PLAYER, ZOMBIE, etc.)\n");
        content.append("#     {world} - World where death occurred\n");
        content.append("#     {x}, {y}, {z} - Death coordinates\n");
        content.append("#\n");
        content.append("# Color Codes:\n");
        content.append("#   Legacy: Use § followed by color code (e.g., §c for red)\n");
        content.append("#   Hex: Use &#RRGGBB format (e.g., &#FF5252 for red)\n");
        content.append("#\n");
        content.append("#========================================\n\n");
        
        // Settings section
        content.append("# Global settings for the death message system\n");
        content.append("settings:\n");
        content.append("  # Enable or disable custom death messages\n");
        content.append("  enabled: true\n");
        content.append("  \n");
        content.append("  # Use fallback message if no custom message is found for a cause\n");
        content.append("  use-fallback: true\n");
        content.append("  \n");
        content.append("  # Enable debug logging for the death message system\n");
        content.append("  debug: false\n\n");
        
        // Placeholders section
        content.append("# Configurable placeholder values\n");
        content.append("# These values are used when information is not available\n");
        content.append("placeholders:\n");
        content.append("  # Text to show when a value is unknown\n");
        content.append("  unknown: \"?\"\n");
        content.append("  \n");
        content.append("  # Text to show when there is no value (e.g., no killer)\n");
        content.append("  none: \"-\"\n");
        content.append("  \n");
        content.append("  # Text to show for unknown entities\n");
        content.append("  unknown-entity: \"Something\"\n");
        content.append("  \n");
        content.append("  # Text to show for unknown players\n");
        content.append("  unknown-player: \"Someone\"\n\n");
        
        // Messages section
        content.append("# Death messages organized by cause\n");
        content.append("# Each cause can have multiple variants that are randomly selected\n");
        content.append("messages:\n\n");
        
        // Add all death causes with examples
        addDeathCause(content, "FALL", "Fall damage deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7fell from a high place",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7forgot how to fly",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7experienced kinetic energy",
            "§c☠ §f%player_name% §7hit the ground too hard",
            "§c☠ §f%player_name% §7learned that gravity exists"
        );
        
        addDeathCause(content, "FIRE", "Fire damage deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7went up in flames",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7burned to death",
            "§c🔥 §f%player_name% §7became a human torch"
        );
        
        addDeathCause(content, "LAVA", "Lava deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7tried to swim in lava",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7discovered that lava is hot",
            "§c🌋 §f%player_name% §7became one with the lava"
        );
        
        addDeathCause(content, "DROWNING", "Drowning deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7drowned",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7forgot to breathe",
            "§9💧 §f%player_name% §7ran out of air"
        );
        
        addDeathCause(content, "BLOCK_EXPLOSION", "Block explosion deaths (TNT, beds, etc.)",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7blew up",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7went out with a bang",
            "§c💥 §f%player_name% §7was blown to bits"
        );
        
        addDeathCause(content, "ENTITY_EXPLOSION", "Entity explosion deaths (Creepers, etc.)",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7was blown up by {killer_name}",
            "§c💥 §f%player_name% §7exploded thanks to {killer_name}",
            "§c💥 §f%player_name% §7got too close to {killer_name}"
        );
        
        addDeathCause(content, "ENTITY_ATTACK", "Entity attack deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7was slain by {killer_name}",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7was killed by {killer_name}",
            "§c⚔ §f%player_name% §7lost a fight with {killer_name}"
        );
        
        addDeathCause(content, "PROJECTILE", "Projectile deaths (arrows, tridents, etc.)",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7was shot by {killer_name}",
            "§c🏹 §f%player_name% §7was turned into a pincushion by {killer_name}",
            "§c🏹 §f%player_name% §7couldn't dodge {killer_name}'s shot"
        );
        
        addDeathCause(content, "VOID", "Void deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7fell into the void",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7fell out of the world",
            "§5🌌 §f%player_name% §7discovered the void"
        );
        
        addDeathCause(content, "SUFFOCATION", "Suffocation deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7suffocated in a wall",
            "§7⬛ §f%player_name% §7became one with the wall",
            "§7⬛ §f%player_name% §7forgot walls are solid"
        );
        
        addDeathCause(content, "STARVATION", "Starvation deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7starved to death",
            "§e🍖 §f%player_name% §7forgot to eat",
            "§e🍖 §f%player_name% §7died of hunger"
        );
        
        addDeathCause(content, "POISON", "Poison deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7was poisoned",
            "§2☠ §f%player_name% §7succumbed to poison",
            "§2☠ §f%player_name% §7died from poisoning"
        );
        
        addDeathCause(content, "MAGIC", "Magic deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7was killed by magic",
            "§d✨ §f%player_name% §7was killed by magic",
            "§d✨ §f%player_name% §7couldn't handle the magic"
        );
        
        addDeathCause(content, "WITHER", "Wither effect deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7withered away",
            "§8💀 §f%player_name% §7was withered",
            "§8💀 §f%player_name% §7succumbed to the wither effect"
        );
        
        addDeathCause(content, "LIGHTNING", "Lightning deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7was struck by lightning",
            "§e⚡ §f%player_name% §7was electrocuted",
            "§e⚡ §f%player_name% §7became a lightning rod"
        );
        
        addDeathCause(content, "FALLING_BLOCK", "Falling block deaths (anvils, etc.)",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7was squashed by a falling block",
            "§7⬇ §f%player_name% §7was crushed",
            "§7⬇ §f%player_name% §7didn't look up"
        );
        
        addDeathCause(content, "CONTACT", "Contact deaths (cacti, berry bushes, etc.)",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7was pricked to death",
            "§a🌵 §f%player_name% §7discovered cacti are sharp",
            "§a🌵 §f%player_name% §7hugged a cactus"
        );
        
        addDeathCause(content, "FLY_INTO_WALL", "Elytra collision deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7experienced kinetic energy",
            "§7💨 §f%player_name% §7flew into a wall",
            "§7💨 §f%player_name% §7forgot to brake"
        );
        
        addDeathCause(content, "HOT_FLOOR", "Magma block deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7discovered the floor was lava",
            "§c🔥 §f%player_name% §7walked on hot floor",
            "§c🔥 §f%player_name% §7burned their feet"
        );
        
        addDeathCause(content, "CRAMMING", "Entity cramming deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7was squished",
            "§7⬛ §f%player_name% §7was crammed to death",
            "§7⬛ §f%player_name% §7was in a tight spot"
        );
        
        addDeathCause(content, "DRYOUT", "Dryout deaths (fish out of water)",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7dried out",
            "§e☀ §f%player_name% §7needed water",
            "§e☀ §f%player_name% §7couldn't find water"
        );
        
        addDeathCause(content, "FREEZE", "Freezing deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7froze to death",
            "§b❄ §f%player_name% §7became an ice sculpture",
            "§b❄ §f%player_name% §7couldn't handle the cold"
        );
        
        addDeathCause(content, "SONIC_BOOM", "Warden sonic boom deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7was obliterated by a sonic boom",
            "§5💥 §f%player_name% §7was sonic boomed",
            "§5💥 §f%player_name% §7angered the Warden"
        );
        
        addDeathCause(content, "THORNS", "Thorns enchantment deaths",
            "%luckperms_prefix% §f%player_name%§r %luckperms_suffix% §7was killed trying to hurt {killer_name}",
            "§a🌹 §f%player_name% §7was pricked by {killer_name}'s thorns",
            "§a🌹 §f%player_name% §7learned about thorns the hard way"
        );
        
        return content.toString();
    }

    /**
     * Helper method to add a death cause section to the YAML content
     * @param content StringBuilder to append to
     * @param cause Death cause name
     * @param description Description comment
     * @param variants Message variants (3-5 examples)
     */
    private void addDeathCause(StringBuilder content, String cause, String description, String... variants) {
        content.append("  # ").append(description).append("\n");
        content.append("  ").append(cause).append(":\n");
        content.append("    enabled: true\n");
        content.append("    variants:\n");
        
        for (String variant : variants) {
            content.append("      - \"").append(variant).append("\"\n");
        }
        
        content.append("\n");
    }

    /**
     * Loads indirect message variants from configuration
     * Supports damage-type-specific variants and generic indirect messages
     * 
     * @param causeSection Configuration section for the death cause
     * @param causeKey The death cause key for logging
     * @return Map of damage type to list of indirect message variants
     */
    private Map<com.nonxedy.nonchat.util.death.DamageType, List<String>> loadIndirectVariants(
            ConfigurationSection causeSection, String causeKey) {
        Map<com.nonxedy.nonchat.util.death.DamageType, List<String>> indirectVariants = 
            new EnumMap<>(com.nonxedy.nonchat.util.death.DamageType.class);
        
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
                        indirectVariants.put(com.nonxedy.nonchat.util.death.DamageType.UNKNOWN, validGeneric);
                    }
                }
            }
            
            // Load damage-type-specific variants
            for (com.nonxedy.nonchat.util.death.DamageType damageType : com.nonxedy.nonchat.util.death.DamageType.values()) {
                if (damageType == com.nonxedy.nonchat.util.death.DamageType.UNKNOWN) {
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
            
            if (deathConfig.isDebugEnabled() && !indirectVariants.isEmpty()) {
                logger.fine("Loaded " + indirectVariants.size() + " indirect variant types for cause: " + causeKey);
            }
            
            // Validate that at least one indirect variant exists
            if (indirectVariants.isEmpty() && indirectSection != null) {
                if (deathConfig.isDebugEnabled()) {
                    logger.warning("indirect-variants section exists for '" + causeKey + "' but no valid variants were loaded");
                }
            }
            
        } catch (Exception e) {
            logger.warning("Error loading indirect variants for cause '" + causeKey + "': " + e.getMessage());
            if (deathConfig.isDebugEnabled()) {
                e.printStackTrace();
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
    private Map<com.nonxedy.nonchat.util.death.DamageType, String> selectIndirectVariants(
            Map<com.nonxedy.nonchat.util.death.DamageType, List<String>> indirectVariantsByType) {
        Map<com.nonxedy.nonchat.util.death.DamageType, String> selected = 
            new EnumMap<>(com.nonxedy.nonchat.util.death.DamageType.class);
        
        try {
            for (Map.Entry<com.nonxedy.nonchat.util.death.DamageType, List<String>> entry : indirectVariantsByType.entrySet()) {
                List<String> variants = entry.getValue();
                if (variants != null && !variants.isEmpty()) {
                    // Select random variant from the list
                    int randomIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(variants.size());
                    selected.put(entry.getKey(), variants.get(randomIndex));
                }
            }
        } catch (Exception e) {
            logger.warning("Error selecting indirect variants: " + e.getMessage());
            if (deathConfig.isDebugEnabled()) {
                e.printStackTrace();
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
                if (deathConfig.isDebugEnabled()) {
                    logger.warning("Null message variant detected during validation");
                }
                return false;
            }
            
            // Trim whitespace and check if empty
            String trimmed = message.trim();
            if (trimmed.isEmpty()) {
                if (deathConfig.isDebugEnabled()) {
                    logger.warning("Empty or whitespace-only message variant detected during validation");
                }
                return false;
            }
            
            // Additional validation: check minimum length
            if (trimmed.length() < 3) {
                if (deathConfig.isDebugEnabled()) {
                    logger.warning("Message variant too short (less than 3 characters): '" + trimmed + "'");
                }
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.warning("Error validating message variant: " + e.getMessage());
            return false;
        }
    }
}

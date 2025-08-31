package com.nonxedy.nonchat.util.integration.external;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.milkbowl.vault.economy.Economy;

// Utility class for managing plugin integrations with LuckPerms, Vault, and PlaceholderAPI
public class IntegrationUtil {
    // Static fields to hold instances of LuckPerms, Vault Economy, and PlaceholderAPI status
    private static LuckPerms luckPerms;
    private static Economy economy;
    private static boolean placeholderAPIEnabled;

    /**
     * Initializes all plugin integrations
     * Should be called when the plugin enables
     */
    public static void setupIntegrations() {
        setupLuckPerms();
        setupEconomy();
        
        try {
            placeholderAPIEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
            if (placeholderAPIEnabled) {
                Bukkit.getLogger().info("[nonchat] PlaceholderAPI integration enabled");
            } else {
                Bukkit.getLogger().info("[nonchat] PlaceholderAPI not available - placeholder features will be disabled");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[nonchat] Error checking PlaceholderAPI: " + e.getMessage());
            placeholderAPIEnabled = false;
        }
    }

    /**
     * Sets up the LuckPerms integration
     * Attempts to get the LuckPerms service provider from Bukkit
     */
    private static void setupLuckPerms() {
        try {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                luckPerms = provider.getProvider();
                Bukkit.getLogger().info("[nonchat] LuckPerms integration enabled");
            } else {
                Bukkit.getLogger().info("[nonchat] LuckPerms not available - permission features will be disabled");
            }
        } catch (NoClassDefFoundError e) {
            Bukkit.getLogger().info("[nonchat] LuckPerms not installed - permission features will be disabled");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[nonchat] Error setting up LuckPerms: " + e.getMessage());
        }
    }

    /**
     * Sets up the Vault Economy integration
     * Attempts to get the Economy service provider from Bukkit
     */
    private static void setupEconomy() {
        try {
            RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (provider != null) {
                economy = provider.getProvider();
                Bukkit.getLogger().info("[nonchat] Vault Economy integration enabled");
            } else {
                Bukkit.getLogger().info("[nonchat] Vault Economy not available - economy features will be disabled");
            }
        } catch (NoClassDefFoundError e) {
            Bukkit.getLogger().info("[nonchat] Vault not installed - economy features will be disabled");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[nonchat] Error setting up Vault Economy: " + e.getMessage());
        }
    }

    /**
     * Gets the prefix for a player from LuckPerms
     * @param player The player to get the prefix for
     * @return The player's prefix or empty string if LuckPerms is not available
     */
    public static String getPlayerPrefix(Player player) {
        try {
            if (luckPerms != null) {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                return user != null ? user.getCachedData().getMetaData().getPrefix() : "";
            }
        } catch (Exception e) {
            Bukkit.getLogger().fine("[nonchat] Error getting player prefix: " + e.getMessage());
        }
        return "";
    }

    /**
     * Gets the player's current balance from Vault Economy
     * @param player The player to get the balance for
     * @return The player's balance formatted to 2 decimal places or "0" if Economy is not available
     */
    public static String getBalance(Player player) {
        try {
            if (economy != null) {
                return String.format("%.2f", economy.getBalance(player));
            }
        } catch (Exception e) {
            Bukkit.getLogger().fine("[nonchat] Error getting player balance: " + e.getMessage());
        }
        return "0";
    }

    /**
     * Gets the player's total play time using PlaceholderAPI
     * @param player The player to get the play time for
     * @return The player's play time or "0h" if PlaceholderAPI is not available
     */
    public static String getPlayTime(Player player) {
        try {
            if (placeholderAPIEnabled) {
                return PlaceholderAPI.setPlaceholders(player, "%statistic_time_played%");
            }
        } catch (Exception e) {
            Bukkit.getLogger().fine("[nonchat] Error getting player play time: " + e.getMessage());
        }
        return "0h";
    }

    /**
    * Process any placeholder using PlaceholderAPI
    * @param player The player to process placeholders for
    * @param text The text containing placeholders
    * @return Processed text with placeholders replaced or original if PlaceholderAPI not available
    */
    public static String processPlaceholders(Player player, String text) {
        try {
            if (placeholderAPIEnabled && player != null && text != null) {
                try {
                    return PlaceholderAPI.setPlaceholders(player, text);
                } catch (Exception e) {
                    Bukkit.getLogger().log(Level.WARNING, "[nonchat] Error processing placeholder: {0}", e.getMessage());
                    return text;
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().fine("[nonchat] PlaceholderAPI not available for processing placeholders");
        }
        return text;
    }
}

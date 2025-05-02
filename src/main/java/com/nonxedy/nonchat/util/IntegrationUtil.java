package com.nonxedy.nonchat.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

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
        placeholderAPIEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /**
     * Sets up the LuckPerms integration
     * Attempts to get the LuckPerms service provider from Bukkit
     */
    private static void setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        }
    }

    /**
     * Sets up the Vault Economy integration
     * Attempts to get the Economy service provider from Bukkit
     */
    private static void setupEconomy() {
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider != null) {
            economy = provider.getProvider();
        }
    }

    /**
     * Gets the prefix for a player from LuckPerms
     * @param player The player to get the prefix for
     * @return The player's prefix or empty string if LuckPerms is not available
     */
    public static String getPlayerPrefix(Player player) {
        if (luckPerms != null) {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            return user != null ? user.getCachedData().getMetaData().getPrefix() : "";
        }
        return "";
    }

    /**
     * Gets the player's current balance from Vault Economy
     * @param player The player to get the balance for
     * @return The player's balance formatted to 2 decimal places or "0" if Economy is not available
     */
    public static String getBalance(Player player) {
        if (economy != null) {
            return String.format("%.2f", economy.getBalance(player));
        }
        return "0";
    }

    /**
     * Gets the player's total play time using PlaceholderAPI
     * @param player The player to get the play time for
     * @return The player's play time or "0h" if PlaceholderAPI is not available
     */
    public static String getPlayTime(Player player) {
        if (placeholderAPIEnabled) {
            return PlaceholderAPI.setPlaceholders(player, "%statistic_time_played%");
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
        if (placeholderAPIEnabled && player != null && text != null) {
            try {
                return PlaceholderAPI.setPlaceholders(player, text);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Error processing placeholder: " + e.getMessage());
                return text;
            }
        }
        return text;
    }
}

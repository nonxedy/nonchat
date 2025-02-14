package com.nonxedy.nonchat.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class IntegrationUtil {
    private static LuckPerms luckPerms;
    private static Economy economy;
    private static boolean placeholderAPIEnabled;

    public static void setupIntegrations() {
        setupLuckPerms();
        setupEconomy();
        placeholderAPIEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    private static void setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        }
    }

    private static void setupEconomy() {
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider != null) {
            economy = provider.getProvider();
        }
    }

    public static String getPlayerPrefix(Player player) {
        if (luckPerms != null) {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            return user != null ? user.getCachedData().getMetaData().getPrefix() : "";
        }
        return "";
    }

    public static String getBalance(Player player) {
        if (economy != null) {
            return String.format("%.2f", economy.getBalance(player));
        }
        return "0";
    }

    public static String getPlayTime(Player player) {
        if (placeholderAPIEnabled) {
            return PlaceholderAPI.setPlaceholders(player, "%statistic_time_played%");
        }
        return "0h";
    }
}

package com.nonxedy.nonchat.util.chat.filters;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import com.nonxedy.nonchat.api.MessageFilter;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

public class AdDetector implements MessageFilter {
    private static final Pattern IP_PORT_PATTERN = Pattern.compile(
            "(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d{1,5})?|" + // IPv4 with optional port
            "\\b(?:play\\.|mc\\.|server\\.)?[\\w-]{2,}\\.[\\w.-]{2,}\\b"); // Common domain patterns

    private final List<String> whitelistedUrls;
    private final float sensitivity;
    private final String punishCommand;

    public AdDetector(List<String> whitelistedUrls, float sensitivity, String punishCommand) {
        this.whitelistedUrls = whitelistedUrls;
        this.sensitivity = Math.max(0f, Math.min(1f, sensitivity));
        this.punishCommand = punishCommand;
    }

    @Override
    public boolean shouldFilter(Player player, String message) {
        if (player.hasPermission("nonchat.ad.bypass")) {
            return false;
        }

        Matcher matcher = IP_PORT_PATTERN.matcher(message.toLowerCase());
        while (matcher.find()) {
            String matched = matcher.group();
            if (!isWhitelisted(matched)) {
                notifyStaff(player, message);
                return true;
            }
        }

        // Additional checks based on sensitivity
        if (sensitivity > 0.5f) {
            if (detectCommonAdTerms(message)) {
                notifyStaff(player, message);
                return true;
            }
        }

        return false;
    }

    private boolean isWhitelisted(String url) {
        for (String whitelisted : whitelistedUrls) {
            if (url.contains(whitelisted.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean detectCommonAdTerms(String message) {
        String lower = message.toLowerCase();
        return lower.contains("join") && lower.contains("server") ||
               lower.contains("ip") && lower.contains("play");
    }

    private void notifyStaff(Player player, String message) {
        String notification = String.format("§c[Anti-Ad] §e%s §7posted advertisement: §f%s", 
                player.getName(), message);
        
        Bukkit.getOnlinePlayers().stream()
            .filter(p -> p.hasPermission("nonchat.ad.notify"))
            .forEach(p -> p.sendMessage(notification));
            
        // Log to console
        Bukkit.getConsoleSender().sendMessage(notification);
        
        // Execute punishment command if configured
        if (punishCommand != null && !punishCommand.isEmpty()) {
            String cmd = punishCommand.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }
}

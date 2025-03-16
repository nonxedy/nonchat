package com.nonxedy.nonchat.command.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.util.ColorUtil;

import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

/**
 * Manages staff communication channel
 * Provides secure chat for server staff members
 */
public class StaffChatCommand implements CommandExecutor, TabCompleter {

    private final nonchat plugin;
    private final PluginMessages messages;
    private final PluginConfig config;

    public StaffChatCommand(nonchat plugin, PluginMessages messages, PluginConfig config) {
        this.plugin = plugin;
        this.messages = messages;
        this.config = config;
    }

    /**
     * Handles staff chat command execution
     * @param sender Command sender
     * @param command Command being executed
     * @param label Command label used
     * @param args Command arguments
     * @return true if command handled successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);

        if (!sender.hasPermission("nonchat.sc")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("You don't have permission to send broadcast.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("invalid-usage-sc")));
            plugin.logError("Invalid usage for staffchat command");
            return true;
        }

        String message = String.join(" ", args);
        broadcastStaffMessage(sender, message);
        return true;
    }

    /**
     * Broadcasts message to staff members
     * @param sender Message sender
     * @param message Message content
     */
    private void broadcastStaffMessage(CommandSender sender, String message) {
        String staffChatFormat = config.getScFormat();
        String staffChatName = config.getStaffChatName();
        
        String formattedMessage = createStaffMessage(sender, staffChatName, staffChatFormat, message);
        Component staffMessage = ColorUtil.parseComponent(formattedMessage);
        
        try {
            plugin.getServer().getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("nonchat.sc"))
                .forEach(player -> player.sendMessage(staffMessage));
        } catch (Exception e) {
            plugin.logError("There was an error sending message in staff chat: " + e.getMessage());
        }
    }

    /**
     * Creates formatted staff message
     * @param sender Message sender
     * @param staffChatName Staff chat identifier
     * @param staffChatFormat Message format template
     * @param message Message content
     * @return Formatted message string
     */
    private String createStaffMessage(CommandSender sender, String staffChatName, String staffChatFormat, String message) {
        String senderName = sender.getName();
        String prefix = "";
        String suffix = "";

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                prefix = user.getCachedData().getMetaData().getPrefix();
                suffix = user.getCachedData().getMetaData().getSuffix();
            }
        } else {
            senderName = "Console";
        }

        return String.format("&b%s &f%s",
            staffChatName,
            staffChatFormat
                .replace("{sender}", senderName)
                .replace("{prefix}", prefix != null ? prefix : "")
                .replace("{suffix}", suffix != null ? suffix : "")
                .replace("{message}", message)
        );
    }

    /**
     * Provides tab completion suggestions
     * @param sender Command sender
     * @param command Command being completed
     * @param label Command label used
     * @param args Current arguments
     * @return List of suggestions
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                        @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("nonchat.sc")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> suggestions = Arrays.asList(
                "message"
            );

            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}

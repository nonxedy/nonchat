package com.nonxedy.nonchat.command;

import java.util.Random;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

public class RollCommand implements CommandExecutor {
    
    private final Random random = new Random();
    private final nonchat plugin;
    private final PluginConfig config;
    private final PluginMessages messages;

    public RollCommand(nonchat plugin, PluginConfig config, PluginMessages messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);

        // Check if command is enabled in config
        if (!config.isRollCommandEnabled()) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("command-disabled")));
            plugin.logError("Player " + sender.getName() + " tried to use disabled roll command");
            return true;
        }

        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("player-only")));
            plugin.logError("Roll command can only be used by players");
            return true;
        }

        // Check permission
        if (!sender.hasPermission("nonchat.roll")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("Player " + sender.getName() + " tried to use roll command without permission");
            return true;
        }

        // Validate arguments
        if (args.length != 1) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("invalid-usage-roll")));
            plugin.logError("Player " + sender.getName() + " tried to use roll command without max number");
            return true;
        }

        try {
            int maxNumber = Integer.parseInt(args[0]);
            if (maxNumber <= 0) {
                sender.sendMessage(ColorUtil.parseComponent(messages.getString("invalid-number")));
                plugin.logError("Player " + sender.getName() + " tried to use roll command with invalid number");
                return true;
            }

            int rolledNumber = random.nextInt(maxNumber) + 1;
            String format = config.getRollFormat()
                .replace("{player}", sender.getName())
                .replace("{number}", String.valueOf(rolledNumber));

            sender.getServer().broadcast(ColorUtil.parseComponent(format));
            plugin.logResponse("Player " + sender.getName() + " used roll command");
            return true;

        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("invalid-number")));
            plugin.logError("Player " + sender.getName() + " tried to use roll command with invalid number");
            return true;
        }
    }
}

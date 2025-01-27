package com.nonxedy.nonchat.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

public class MeCommand implements CommandExecutor {
    private final nonchat plugin;
    private final PluginConfig config;
    private final PluginMessages messages;

    public MeCommand(nonchat plugin, PluginConfig config, PluginMessages messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);

        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("player-only")));
            plugin.logError("Me command can only be used by players");
            return true;
        }

        if (!sender.hasPermission("nonchat.me")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("Player " + sender.getName() + " tried to use me command without permission");
            return true;
        }

        if (!config.isMeCommandEnabled()) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("command-disabled")));
            plugin.logError("Player " + sender.getName() + " tried to use disabled me command");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("invalid-usage-me")));
            plugin.logError("Player " + sender.getName() + " tried to use me command without message");
            return true;
        }

        String message = String.join(" ", args);
        String format = config.getMeFormat()
            .replace("{player}", sender.getName())
            .replace("{message}", message);

        plugin.getServer().broadcast(ColorUtil.parseComponent(format));
        plugin.logResponse("Me command executed by " + sender.getName() + ": " + message);

        return true;
    }
}

package com.nonxedy.nonchat.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

import net.kyori.adventure.text.Component;

public class NhelpCommand implements CommandExecutor {

    private final PluginMessages messages;
    private final nonchat plugin;

    public NhelpCommand(PluginMessages messages, nonchat plugin) {
        this.messages = messages;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);

        if (!sender.hasPermission("nonchat.nhelp")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("Permission denied: nonchat.nhelp");
            return true;
        }

        sendHelpMessage(sender);
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        try {
            Component helpMessage = Component.empty()
                .append(ColorUtil.parseComponent(messages.getString("help")))
                .append(Component.newline())
                .append(getCommandsList());

            sender.sendMessage(helpMessage);
            plugin.logResponse("Help message sent successfully");
        } catch (Exception e) {
            plugin.logError("Failed to send help message: " + e.getMessage());
        }
    }

    private Component getCommandsList() {
        return Component.empty()
            .append(ColorUtil.parseComponent(messages.getString("nreload")))
            .append(Component.newline())
            .append(ColorUtil.parseComponent(messages.getString("help-command")))
            .append(Component.newline())
            .append(ColorUtil.parseComponent(messages.getString("server-command")))
            .append(Component.newline())
            .append(ColorUtil.parseComponent(messages.getString("message-command")))
            .append(Component.newline())
            .append(ColorUtil.parseComponent(messages.getString("broadcast-command")))
            .append(Component.newline())
            .append(ColorUtil.parseComponent(messages.getString("ignore-command")))
            .append(Component.newline())
            .append(ColorUtil.parseComponent(messages.getString("sc-command")))
            .append(Component.newline())
            .append(ColorUtil.parseComponent(messages.getString("spy-command")));
    }
}

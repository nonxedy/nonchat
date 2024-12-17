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

    private PluginMessages messages;
    private nonchat plugin;

    public NhelpCommand(PluginMessages messages, nonchat plugin) {
        this.messages = messages;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);

        if (!sender.hasPermission("nonchat.nhelp")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("You don't have permission to show help.");
            return true;
        }

        Component helpMessage = Component.empty()
            .append(ColorUtil.parseComponent(messages.getString("nreload") + "\n"))
            .append(ColorUtil.parseComponent(messages.getString("help-command") + "\n"))
            .append(ColorUtil.parseComponent(messages.getString("server-command") + "\n"))
            .append(ColorUtil.parseComponent(messages.getString("message-command") + "\n"))
            .append(ColorUtil.parseComponent(messages.getString("broadcast-command") + "\n"))
            .append(ColorUtil.parseComponent(messages.getString("ignore-command") + "\n"))
            .append(ColorUtil.parseComponent(messages.getString("sc-command") + "\n"))
            .append(ColorUtil.parseComponent(messages.getString("spy-command")));

        try {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("help")));
        sender.sendMessage(helpMessage);
        plugin.logResponse("Help shown.");
        } catch (Exception e) {
            plugin.logError("There was an error showing help: " + e.getMessage());
        }
        return true;
    }
}
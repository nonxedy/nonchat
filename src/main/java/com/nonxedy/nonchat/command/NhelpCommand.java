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
            sender.sendMessage(Component.text(ColorUtil.parseColor(messages.getNoPermission())));
            plugin.logError("You don't have permission to show help.");
            return true;
        }

        try {
        sender.sendMessage(Component.text(ColorUtil.parseColor(messages.getHelp())));

        sender.sendMessage(Component.text()
                .append(Component.text(ColorUtil.parseColor(messages.getNreload() + "\n")))
                .append(Component.text(ColorUtil.parseColor(messages.getHelpCommand() + "\n")))
                .append(Component.text(ColorUtil.parseColor(messages.getServerCommand() + "\n")))
                .append(Component.text(ColorUtil.parseColor(messages.getMessageCommand() + "\n")))
                .append(Component.text(ColorUtil.parseColor(messages.getBroadcastCommand() + "\n")))
                .append(Component.text(ColorUtil.parseColor(messages.getIgnoreCommand() + "\n")))
                .append(Component.text(ColorUtil.parseColor(messages.getScCommand() + "\n")))
                .append(Component.text(ColorUtil.parseColor(messages.getSpyCommand() + "\n")))
                .build());
            
        plugin.logResponse("Help shown.");
        } catch (Exception e) {
            plugin.logError("There was an error showing help: " + e.getMessage());
        }
        return true;
    }
}
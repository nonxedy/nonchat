package com.nonxedy.nonchat.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.config.PluginMessages;
import com.nonxedy.nonchat.nonchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

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
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                    .build());
            plugin.logError("You don't have permission to show help.");
            return true;
        }

        try {
        sender.sendMessage(Component.text()
                .append(Component.text(messages.getHelp() + "\n", TextColor.fromHexString("#E088FF")))
                .build());

        sender.sendMessage(Component.text()
                .append(Component.text(messages.getNreload() + "\n", TextColor.fromHexString("#E088FF")))
                .append(Component.text(messages.getHelpCommand() + "\n", TextColor.fromHexString("#E088FF")))
                .append(Component.text(messages.getServerCommand() + "\n", TextColor.fromHexString("#E088FF")))
                .append(Component.text(messages.getMessageCommand() + "\n", TextColor.fromHexString("#E088FF")))
                .append(Component.text(messages.getBroadcastCommand() + "\n", TextColor.fromHexString("#E088FF")))
                .append(Component.text(messages.getIgnoreCommand() + "\n", TextColor.fromHexString("#E088FF")))
                .append(Component.text(messages.getScCommand() + "\n", TextColor.fromHexString("#E088FF")))
                .append(Component.text(messages.getSpyCommand() + "\n", TextColor.fromHexString("#E088FF")))
                .build());
            
        plugin.logResponse("Help shown.");
        } catch (Exception e) {
            plugin.logError("There was an error showing help: " + e.getMessage());
        }
        return true;
    }
}
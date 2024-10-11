package com.hgtoiwr.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.hgtoiwr.config.PluginMessages;
import com.hgtoiwr.nonchat.nonchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class HelpCommand implements CommandExecutor {

    private PluginMessages messages;

    public HelpCommand(PluginMessages messages) {
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        nonchat plugin = (nonchat) Bukkit.getPluginManager().getPlugin("nonchat");
        plugin.logCommand(command.getName(), args);

        if (!sender.hasPermission("nonchat.help")) {
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                    .build());
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
                .build());
            
        plugin.logResponse("Помощь выведена.");
        } catch (Exception e) {
            plugin.logError("Ошибка вывода помощи: " + e.getMessage());
        }
        return true;
    }
}
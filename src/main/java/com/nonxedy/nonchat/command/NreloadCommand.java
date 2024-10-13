package com.nonxedy.nonchat.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.config.PluginMessages;
import com.nonxedy.nonchat.nonchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class NreloadCommand implements CommandExecutor {

    private nonchat nonchat;
    private PluginMessages messages;

    public NreloadCommand(nonchat nonchat, PluginMessages messages) {
        this.nonchat = nonchat;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("nonchat.nreload")) {
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                    .build());
            nonchat.logError("You don't have permission to reload the config.");
            return true;
        }

        sender.sendMessage(Component.text()
                .append(Component.text(messages.getReloading(), TextColor.fromHexString("#E088FF")))
                .build());
        nonchat.logResponse("Reloading...");

        try {
            nonchat.reloadConfig();
            nonchat.reloadDebugger();
            nonchat.stopAutoBroadcastSender();
            nonchat.registerUtils();
            sender.sendMessage(Component.text()
                .append(Component.text(messages.getReloaded(), TextColor.fromHexString("#52FFA6")))
                .build());

            nonchat.logResponse("Config reloaded.");
        } catch (Exception e) {
            sender.sendMessage(Component.text()
                .append(Component.text(messages.getReloadFailed(), TextColor.fromHexString("#FF5252")))
                .build());

            nonchat.logError("There was an error reloading the config: " + e.getMessage());
        }
        return true;
    }
}
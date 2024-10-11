package com.hgtoiwr.nonchat.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.hgtoiwr.config.PluginMessages;
import com.hgtoiwr.nonchat.nonchat;

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
            return true;
        }

        sender.sendMessage(Component.text()
                .append(Component.text(messages.getReloading(), TextColor.fromHexString("#E088FF")))
                .build());

        try {
            nonchat.reloadConfig();
            nonchat.reloadDebugger();
            nonchat.stopAutoBroadcastSender();
            nonchat.registerUtils();
            sender.sendMessage(Component.text()
                .append(Component.text(messages.getReloaded(), TextColor.fromHexString("#52FFA6")))
                .build());
        } catch (Exception e) {
            sender.sendMessage(Component.text()
                .append(Component.text(messages.getReloadFailed(), TextColor.fromHexString("#FF5252")))
                .build());
        }
        return true;
    }
}
package com.hgtoiwr.nonchat.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.hgtoiwr.nonchat.nonchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class NreloadCommand implements CommandExecutor {

    private nonchat nonchat;

    public NreloadCommand(nonchat nonchat) {
        this.nonchat = nonchat;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("nonchat.nreload")) {
            sender.sendMessage(Component.text()
                    .append(Component.text("Недостаточно прав", TextColor.fromHexString("#ADF3FD")))
                    .build());
            return true;
        }

        sender.sendMessage(Component.text()
                .append(Component.text("Перезагрузка...", TextColor.fromHexString("#E088FF")))
                .build());

        try {
            nonchat.reloadConfig();
            sender.sendMessage(Component.text()
                .append(Component.text("Плагин перезагружен!", TextColor.fromHexString("#52FFA6")))
                .build());
        } catch (Exception e) {
            sender.sendMessage(Component.text()
                .append(Component.text("Не удалось перезагрузить плагин!", TextColor.fromHexString("#FF5252")))
                .build());
        }
        return true;
    }
}

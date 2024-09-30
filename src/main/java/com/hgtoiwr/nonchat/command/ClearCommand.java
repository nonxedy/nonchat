package com.hgtoiwr.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class ClearCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("nonchat.clear")) {
            sender.sendMessage(Component.text()
                    .append(Component.text("Недостаточно прав", TextColor.fromHexString("#ADF3FD")))
                    .build());
            return true;
        }

        sender.sendMessage(Component.text()
                .append(Component.text("Очистка чата...", TextColor.fromHexString("#E088FF")))
                .build());

        for (int i = 0; i < 100; i++) {
            Bukkit.broadcast(Component.empty());
        }

        Bukkit.broadcast(Component.text()
                .append(Component.text("Чат очищен", TextColor.fromHexString("#52FFA6")))
                .build());

        return true;
    }
}

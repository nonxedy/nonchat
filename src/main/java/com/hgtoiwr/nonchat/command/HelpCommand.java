package com.hgtoiwr.nonchat.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class HelpCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("nonchat.help")) {
            sender.sendMessage(Component.text()
                    .append(Component.text("Недостаточно прав", TextColor.fromHexString("#ADF3FD")))
                    .build());
            return true;
        }

        sender.sendMessage(Component.text()
                .append(Component.text("nonchat | Команды плагина:\n", TextColor.fromHexString("#E088FF")))
                .build());

        sender.sendMessage(Component.text()
                .append(Component.text("/nreload ", TextColor.fromHexString("#E088FF")))
                .append(Component.text("- перезагрузка плагина\n", TextColor.fromHexString("#FFFFFF")))
                .append(Component.text("/help ", TextColor.fromHexString("#E088FF")))
                .append(Component.text("- список команд\n", TextColor.fromHexString("#FFFFFF")))
                .append(Component.text("/server ", TextColor.fromHexString("#E088FF")))
                .append(Component.text("- информация о сервере\n", TextColor.fromHexString("#FFFFFF")))
                .append(Component.text("/m <игрок> <сообщение> (msg, w, whisper, message) ", TextColor.fromHexString("#E088FF")))
                .append(Component.text("- отправка личных сообщений\n", TextColor.fromHexString("#FFFFFF")))
                .append(Component.text("/bc <сообщение> (broadcast) ", TextColor.fromHexString("#E088FF")))
                .append(Component.text("- отправка сообщений всем игрокам", TextColor.fromHexString("#FFFFFF")))
                .build());

        return true;
    }
}
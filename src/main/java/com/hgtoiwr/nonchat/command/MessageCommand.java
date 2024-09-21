package com.hgtoiwr.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class MessageCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("message") ||
            command.getName().equalsIgnoreCase("msg") ||
            command.getName().equalsIgnoreCase("tell") ||
            command.getName().equalsIgnoreCase("w") ||
            command.getName().equalsIgnoreCase("m") ||
            command.getName().equalsIgnoreCase("whisper")) {
            
            if (!sender.hasPermission("nonchat.message")) {
                sender.sendMessage(Component.text()
                        .append(Component.text("Недостаточно прав", TextColor.fromHexString("#ADF3FD")))
                        .build());
                return true;
            }
            
            if (args.length < 2) {
                sender.sendMessage(Component.text()
                        .append(Component.text("Используйте: /msg <игрок> <сообщение>", TextColor.fromHexString("#ADF3FD")))
                        .build());
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text()
                        .append(Component.text("Игрок не найден.", TextColor.fromHexString("#ADF3FD")))
                        .build());
                return true;
            }

            StringBuilder message = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                message.append(args[i]).append(" ");
            }

            if (sender instanceof Player) {
                Player senderPlayer = (Player) sender;
                senderPlayer.sendMessage(Component.text()
                        .append(Component.text("Вы -> ", TextColor.fromHexString("#E088FF")))
                        .append(Component.text(target.getName(), TextColor.fromHexString("#FFFFFF")))
                        .append(Component.text(": ", TextColor.fromHexString("#E088FF")))
                        .append(Component.text(message.toString().trim(), TextColor.fromHexString("#FFFFFF")))
                        .build());
                Bukkit.getConsoleSender().sendMessage(Component.text()
                        .append(Component.text("[nonchat] ", TextColor.fromHexString("#E088FF")))
                        .append(Component.text(sender.getName(), TextColor.fromHexString("#FFFFFF")))
                        .append(Component.text(" -> ", TextColor.fromHexString("#333333")))
                        .append(Component.text(target.getName(), TextColor.fromHexString("#FFFFFF")))
                        .append(Component.text(": ", TextColor.fromHexString("#333333")))
                        .append(Component.text(message.toString().trim(), TextColor.fromHexString("#FFFFFF")))
                        .build());
            }

            target.sendMessage(Component.text()
                    .append(Component.text(sender.getName() + " -> Вы: ", TextColor.fromHexString("#E088FF")))
                    .append(Component.text(message.toString().trim(), TextColor.fromHexString("#FFFFFF")))
                    .build());
            Bukkit.getConsoleSender().sendMessage(Component.text()
                    .append(Component.text("[nonchat] ", TextColor.fromHexString("#E088FF")))
                    .append(Component.text(sender.getName(), TextColor.fromHexString("#FFFFFF")))
                    .append(Component.text(" -> ", TextColor.fromHexString("#E088FF")))
                    .append(Component.text(target.getName(), TextColor.fromHexString("#FFFFFF")))
                    .append(Component.text(": ", TextColor.fromHexString("#E088FF")))
                    .append(Component.text(message.toString().trim(), TextColor.fromHexString("#FFFFFF")))
                    .build());
            return true;
        }
        return false;
    }
}
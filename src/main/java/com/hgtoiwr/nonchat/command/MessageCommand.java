package com.hgtoiwr.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.hgtoiwr.config.PluginConfig;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class MessageCommand implements CommandExecutor {


    private PluginConfig pluginConfig;

    public MessageCommand(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

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

                String privateChatFormat = pluginConfig.getPrivateChatFormat();
                if (sender instanceof Player) {
                    Player senderPlayer = (Player) sender;
                    senderPlayer.sendMessage(Component.text()
                            .append(Component.text(privateChatFormat.replace("{sender}", sender.getName()).replace("{target}", target.getName()).replace("{message}", message.toString().trim()), TextColor.fromHexString("#FFFFFF")))
                            .build());
                }
        
                target.sendMessage(Component.text()
                        .append(Component.text(privateChatFormat.replace("{sender}", sender.getName()).replace("{target}", "Вы").replace("{message}", message.toString().trim()), TextColor.fromHexString("#FFFFFF")))
                        .build());
            return true;
        }
        return false;
    }
}
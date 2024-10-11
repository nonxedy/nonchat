package com.hgtoiwr.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.hgtoiwr.config.PluginConfig;
import com.hgtoiwr.config.PluginMessages;
import com.hgtoiwr.nonchat.nonchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class MessageCommand implements CommandExecutor {

    private PluginConfig pluginConfig;
    private PluginMessages messages;

    public MessageCommand(PluginConfig pluginConfig, PluginMessages messages) {
        this.pluginConfig = pluginConfig;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        nonchat plugin = (nonchat) Bukkit.getPluginManager().getPlugin("nonchat");
        plugin.logCommand(command.getName(), args);

        if (command.getName().equalsIgnoreCase("message") ||
            command.getName().equalsIgnoreCase("msg") ||
            command.getName().equalsIgnoreCase("tell") ||
            command.getName().equalsIgnoreCase("w") ||
            command.getName().equalsIgnoreCase("m") ||
            command.getName().equalsIgnoreCase("whisper")) {
            
            if (!sender.hasPermission("nonchat.message")) {
                sender.sendMessage(Component.text()
                        .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                        .build());
                return true;
            }
            
            if (args.length < 2) {
                sender.sendMessage(Component.text()
                        .append(Component.text(messages.getInvalidUsageMessage(), TextColor.fromHexString("#ADF3FD")))
                        .build());
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text()
                        .append(Component.text(messages.getPlayerNotFound(), TextColor.fromHexString("#ADF3FD")))
                        .build());
                return true;
            }

            StringBuilder message = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                message.append(args[i]).append(" ");
            }

            String privateChatFormat = pluginConfig.getPrivateChatFormat();
            try {
                if (sender instanceof Player) {
                    Player senderPlayer = (Player) sender;
                    senderPlayer.sendMessage(Component.text()
                            .append(Component.text(privateChatFormat.replace("{sender}", sender.getName()).replace("{target}", target.getName()).replace("{message}", message.toString().trim()), TextColor.fromHexString("#FFFFFF")))
                            .build());
                    plugin.logResponse("Сообщение отправлено");
                }
            } catch (Exception e) {
                plugin.logError("Ошибка отправки сообщения: " + e.getMessage());
            }
        
            target.sendMessage(Component.text()
                    .append(Component.text(privateChatFormat.replace("{sender}", sender.getName()).replace("{target}", "Вы").replace("{message}", message.toString().trim()), TextColor.fromHexString("#FFFFFF")))
                    .build());
            return true;
        }
        return false;
    }
}
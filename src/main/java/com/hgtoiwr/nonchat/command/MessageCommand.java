package com.hgtoiwr.nonchat.command;

import java.util.UUID;

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
    private nonchat plugin;

    public MessageCommand(nonchat plugin, PluginConfig pluginConfig, PluginMessages messages) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
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
                plugin.logError("No permission for message command");
                return true;
            }
            
            if (args.length < 2) {
                sender.sendMessage(Component.text()
                        .append(Component.text(messages.getInvalidUsageMessage(), TextColor.fromHexString("#ADF3FD")))
                        .build());
                plugin.logError("Invalid usage for message command");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text()
                        .append(Component.text(messages.getPlayerNotFound(), TextColor.fromHexString("#ADF3FD")))
                        .build());
                plugin.logError("Player not found for message command");
                return true;
            }

            UUID senderUUID = ((Player) sender).getUniqueId();
            UUID targetUUID = target.getUniqueId();

            if (plugin.ignoredPlayers.containsKey(targetUUID) && plugin.ignoredPlayers.get(targetUUID).contains(senderUUID)) {
                sender.sendMessage(Component.text()
                        .append(Component.text(messages.getIgnoredByTarget(), TextColor.fromHexString("#FF5252")))
                        .build());
                plugin.logError("Target is ignoring sender");
                return true;
            }

            StringBuilder message = new StringBuilder();
            for (int i =  1; i < args.length; i++) {
                message.append(args[i]).append(" ");
            }

            String privateChatFormat = pluginConfig.getPrivateChatFormat();
            try {
                if (sender instanceof Player) {
                    Player senderPlayer = (Player) sender;
                    senderPlayer.sendMessage(Component.text()
                            .append(Component.text(privateChatFormat.replace("{sender}", sender.getName()).replace("{target}", target.getName()).replace("{message}", message.toString().trim()), TextColor.fromHexString("#FFFFFF")))
                            .build());
                    plugin.logResponse("Message sent");
                }
            } catch (Exception e) {
                plugin.logError("Error sending message: " + e.getMessage());
            }
        
            target.sendMessage(Component.text()
                    .append(Component.text(privateChatFormat.replace("{sender}", sender.getName()).replace("{target}", "Вы").replace("{message}", message.toString().trim()), TextColor.fromHexString("#FFFFFF")))
                    .build());
            return true;
        }
        return false;
    }
}
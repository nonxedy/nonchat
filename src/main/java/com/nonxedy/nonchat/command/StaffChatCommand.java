package com.nonxedy.nonchat.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

public class StaffChatCommand implements CommandExecutor {

    private nonchat plugin;
    private PluginMessages messages;
    private PluginConfig config;

    public StaffChatCommand(nonchat plugin, PluginMessages messages, PluginConfig config) {
        this.plugin = plugin;
        this.messages = messages;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);
        
        if (!sender.hasPermission("nonchat.staffchat")) {
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                    .build());
            plugin.logError("You don't have permission to send broadcast.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getInvalidUsageSc(), TextColor.fromHexString("#ADF3FD")))
                    .build());
            plugin.logError("Invalid usage for staffchat command");
            return true;
        }
        
        String message = String.join(" ", args);
        String staffchat = config.getScFormat();
        String staffChatName = config.getStaffChatName();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = LuckPermsProvider.get().getUserManager().getUser (player.getUniqueId());
            String prefix = user.getCachedData().getMetaData().getPrefix();
            String suffix = user.getCachedData().getMetaData().getSuffix();
    
            try {
                plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("nonchat.sc"))
                    .forEach(p -> p.sendMessage(Component.text()
                    .append(Component.text(staffChatName + " ", TextColor.fromHexString("#ADF3FD")))
                    .append(Component.text(staffchat.replace("{sender}", sender.getName())
                            .replace("{prefix}", prefix != null ? prefix : "")
                            .replace("{suffix}", suffix != null ? suffix : "")
                            .replace("{message}", message), TextColor.fromHexString("#FFFFFF")))
                    .build()));
            } catch (Exception e) {
                plugin.logError("There was an error sending message in staff chat: " + e.getMessage());
            }
        } else {
            try {
                plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("nonchat.sc"))
                    .forEach(p -> p.sendMessage(Component.text()
                    .append(Component.text(staffChatName + " ", TextColor.fromHexString("#ADF3FD")))
                    .append(Component.text(staffchat.replace("{sender}", "Console")
                            .replace("{prefix}", "")
                            .replace("{suffix}", "")
                            .replace("{message}", message), TextColor.fromHexString("#FFFFFF")))
                    .build()));
            } catch (Exception e) {
                plugin.logError("There was an error sending message in staff chat: " + e.getMessage());
            }
        }
        return true;
    }
}
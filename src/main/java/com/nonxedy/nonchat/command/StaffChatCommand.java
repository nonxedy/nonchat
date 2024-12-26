package com.nonxedy.nonchat.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

public class StaffChatCommand implements CommandExecutor {

    private final nonchat plugin;
    private final PluginMessages messages;
    private final PluginConfig config;;
    private static final TextColor STAFF_CHAT_COLOR = TextColor.fromHexString("#ADF3FD");
    private static final TextColor MESSAGE_COLOR = TextColor.fromHexString("#FFFFFF");

    public StaffChatCommand(nonchat plugin, PluginMessages messages, PluginConfig config) {
        this.plugin = plugin;
        this.messages = messages;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);

        if (!sender.hasPermission("nonchat.sc")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("You don't have permission to send broadcast.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("invalid-usage-sc")));
            plugin.logError("Invalid usage for staffchat command");
            return true;
        }

        String message = String.join(" ", args);
        broadcastStaffMessage(sender, message);
        return true;
    }

    private void broadcastStaffMessage(CommandSender sender, String message) {
        String staffChatFormat = config.getScFormat();
        String staffChatName = config.getStaffChatName();
        
        Component staffMessage = createStaffMessage(sender, staffChatName, staffChatFormat, message);
        
        try {
            plugin.getServer().getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("nonchat.sc"))
                .forEach(player -> player.sendMessage(staffMessage));
        } catch (Exception e) {
            plugin.logError("There was an error sending message in staff chat: " + e.getMessage());
        }
    }

    private Component createStaffMessage(CommandSender sender, String staffChatName, String staffChatFormat, String message) {
        String senderName = sender.getName();
        String prefix = "";
        String suffix = "";

        Player player = (Player) sender;
        if (sender instanceof Player) {
            User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                prefix = user.getCachedData().getMetaData().getPrefix();
                suffix = user.getCachedData().getMetaData().getSuffix();
            }
        } else {
            senderName = "Console";
        }

        String formattedMessage = staffChatFormat
            .replace("{sender}", senderName)
            .replace("{prefix}", prefix != null ? prefix : "")
            .replace("{suffix}", suffix != null ? suffix : "")
            .replace("{message}", message);

        return Component.text()
            .append(Component.text(staffChatName + " ", STAFF_CHAT_COLOR))
            .append(Component.text(formattedMessage, MESSAGE_COLOR))
            .build();
    }
}

package com.nonxedy.nonchat.command;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.Bukkit;
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

public class MessageCommand implements CommandExecutor {

    private final nonchat plugin;
    private final PluginConfig config;
    private final PluginMessages messages;
    private final SpyCommand spyCommand;

    public MessageCommand(nonchat plugin, PluginConfig config, PluginMessages messages, SpyCommand spyCommand) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.spyCommand = spyCommand;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);

        if (!isMessageCommand(command.getName())) {
            plugin.logError("Invalid command: " + command.getName());
            return false;
        }

        if (!sender.hasPermission("nonchat.message")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("Player " + sender.getName() + " tried to use the message command without permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("invalid-usage-message")));
            plugin.logError("Player " + sender.getName() + " tried to use the message command with invalid arguments.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("player-not-found")));
            plugin.logError("Player " + sender.getName() + " tried to message a player that is not online.");
            return true;
        }

        if (isIgnored(sender, target)) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("ignored-by-target")));
            plugin.logError("Player " + sender.getName() + " tried to message a player that has ignored them.");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        sendPrivateMessage(sender, target, message);
        
        return true;
    }

    private boolean isMessageCommand(String commandName) {
        return commandName.equalsIgnoreCase("message") ||
            commandName.equalsIgnoreCase("msg") ||
            commandName.equalsIgnoreCase("tell") ||
            commandName.equalsIgnoreCase("w") ||
            commandName.equalsIgnoreCase("m") ||
            commandName.equalsIgnoreCase("whisper");
    }

    private boolean isIgnored(CommandSender sender, Player target) {
        if (!(sender instanceof Player)) {
            return false;
        }
        UUID senderUUID = ((Player) sender).getUniqueId();
        return plugin.ignoredPlayers.containsKey(target.getUniqueId()) &&
            plugin.ignoredPlayers.get(target.getUniqueId()).contains(senderUUID);
    }

    private void sendPrivateMessage(CommandSender sender, Player target, String message) {
        String format = config.getPrivateChatFormat();
        String senderName = sender instanceof Player ? sender.getName() : "Console";
    
        Component senderMessage = ColorUtil.parseComponent(
            format.replace("{sender}", senderName)
                .replace("{target}", target.getName())
                .replace("{message}", message)
        );
        sender.sendMessage(senderMessage);
        plugin.logResponse("Message sent to " + sender.getName());
    
        Component targetMessage = ColorUtil.parseComponent(
            format.replace("{sender}", senderName)
                .replace("{target}", "You")
                .replace("{message}", message)
        );
        target.sendMessage(targetMessage);
        plugin.logResponse("Message sent to " + target.getName());
    
        if (spyCommand != null && sender instanceof Player) {
            spyCommand.onPrivateMessage((Player) sender, target, ColorUtil.parseComponent(message));
            plugin.logResponse("Message sent to spy players");
        }
    }
}

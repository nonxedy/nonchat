package com.nonxedy.nonchat.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

import net.kyori.adventure.text.Component;

/**
 * Handles private messaging between players
 * Provides secure player-to-player communication
 */
public class MessageCommand implements CommandExecutor, TabCompleter {

    // Required plugin instances and configurations
    private final nonchat plugin;
    private final PluginConfig config;
    private final PluginMessages messages;
    private final SpyCommand spyCommand;

    // Constructor to initialize all required dependencies
    public MessageCommand(nonchat plugin, PluginConfig config, PluginMessages messages, SpyCommand spyCommand) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.spyCommand = spyCommand;
    }

    /**
     * Handles private message command execution
     * @param sender Command sender
     * @param command Command being executed
     * @param label Command label used
     * @param args Command arguments
     * @return true if command handled successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Log the command execution for debugging
        plugin.logCommand(command.getName(), args);

        // Verify if the command is a valid message command alias
        if (!isMessageCommand(command.getName())) {
            plugin.logError("Invalid command: " + command.getName());
            return false;
        }

        // Check if sender has permission to use private messaging
        if (!sender.hasPermission("nonchat.message")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("Player " + sender.getName() + " tried to use the message command without permission.");
            return true;
        }

        // Validate command arguments (need at least target player and message)
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("invalid-usage-message")));
            plugin.logError("Player " + sender.getName() + " tried to use the message command with invalid arguments.");
            return true;
        }

        // Get target player and verify they are online
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("player-not-found")));
            plugin.logError("Player " + sender.getName() + " tried to message a player that is not online.");
            return true;
        }

        // Check if target player has ignored the sender
        if (isIgnored(sender, target)) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("ignored-by-target")));
            plugin.logError("Player " + sender.getName() + " tried to message a player that has ignored them.");
            return true;
        }

        // Combine all remaining arguments into the message
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        sendPrivateMessage(sender, target, message);
        
        return true;
    }

    /**
     * Checks if command is a valid message command alias
     * @param commandName Command name to check
     * @return true if valid message command
     */
    private boolean isMessageCommand(String commandName) {
        return commandName.equalsIgnoreCase("message") ||
            commandName.equalsIgnoreCase("msg") ||
            commandName.equalsIgnoreCase("tell") ||
            commandName.equalsIgnoreCase("w") ||
            commandName.equalsIgnoreCase("m") ||
            commandName.equalsIgnoreCase("whisper");
    }

    /**
     * Checks if target has ignored sender
     * @param sender Message sender
     * @param target Message recipient
     * @return true if sender is ignored
     */
    private boolean isIgnored(CommandSender sender, Player target) {
        if (!(sender instanceof Player)) {
            return false;
        }
        UUID senderUUID = ((Player) sender).getUniqueId();
        return plugin.ignoredPlayers.containsKey(target.getUniqueId()) &&
            plugin.ignoredPlayers.get(target.getUniqueId()).contains(senderUUID);
    }

    /**
     * Sends private message to sender and recipient
     * @param sender Message sender
     * @param target Message recipient
     * @param message Message content
     */
    private void sendPrivateMessage(CommandSender sender, Player target, String message) {
        // Get the message format from config
        String format = config.getPrivateChatFormat();
        // Handle console messages by using "Console" as sender name
        String senderName = sender instanceof Player ? sender.getName() : "Console";
    
        // Create and send formatted message to sender
        Component senderMessage = ColorUtil.parseComponent(
            format.replace("{sender}", senderName)
                .replace("{target}", target.getName())
                .replace("{message}", message)
        );
        sender.sendMessage(senderMessage);
        plugin.logResponse("Message sent to " + sender.getName());
    
        // Create and send formatted message to target
        Component targetMessage = ColorUtil.parseComponent(
            format.replace("{sender}", senderName)
                .replace("{target}", "You")
                .replace("{message}", message)
        );
        target.sendMessage(targetMessage);
        plugin.logResponse("Message sent to " + target.getName());
    
        // Notify spy players if spy system is enabled and sender is a player
        if (spyCommand != null && sender instanceof Player) {
            spyCommand.onPrivateMessage((Player) sender, target, ColorUtil.parseComponent(message));
            plugin.logResponse("Message sent to spy players");
        }
    }

    /**
     * Provides tab completion suggestions
     * @param sender Command sender
     * @param command Command being completed
     * @param label Command label used
     * @param args Current arguments
     * @return List of suggestions
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                        @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("nonchat.message")) {
            return Collections.emptyList();
        }
    
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equals(sender.getName()))
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
    
        if (args.length == 2) {
            List<String> suggestions = Arrays.asList(
                "message"
            );
            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
    
        return Collections.emptyList();
    }
    
}

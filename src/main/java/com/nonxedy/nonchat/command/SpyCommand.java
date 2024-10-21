package com.nonxedy.nonchat.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.config.PluginMessages;
import com.nonxedy.nonchat.nonchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class SpyCommand implements CommandExecutor {

    private nonchat plugin;
    private PluginMessages messages;
    private boolean isSpying = false;
    private List<Player> spyPlayers = new ArrayList<>();

    public SpyCommand(nonchat plugin, PluginMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);
        if (!sender.hasPermission("nonchat.spy")) {
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                    .build());
            plugin.logError("You don't have permission to send broadcast.");
            return true;
        }
        Player player = (Player) sender;
        
        try {
            if (isSpying) {
                // Disable spy mode
                isSpying = false;
                spyPlayers.remove(player);
                player.sendMessage(Component.text()
                        .append(Component.text("Spy mode disabled.", TextColor.fromHexString("#FF5252")))
                        .build());
                plugin.logResponse("Spy mode disabled.");
            } else {
                // Enable spy mode
                isSpying = true;
                spyPlayers.add(player);
                player.sendMessage(Component.text()
                        .append(Component.text("Spy mode enabled.", TextColor.fromHexString("#52FFA6")))
                        .build());
                plugin.logResponse("Spy mode enabled.");
            }
        } catch (Exception e) {
            plugin.logError("There was an error toggling spy mode: " + e.getMessage());
        }

        return true;
    }

    public void onPrivateMessage(Player sender, Player recipient, String message) {
        if (isSpying) {
            plugin.logResponse("Spy mode is active. Message from " + sender.getName() + " to " + recipient.getName() + ": " + message);
            for (Player spyPlayer : spyPlayers) {
                spyPlayer.sendMessage(Component.text()
                        .append(Component.text(sender.getName() + " ", TextColor.fromHexString("#E088FF")))
                        .append(Component.text("-> ", TextColor.fromHexString("#555555")))
                        .append(Component.text(recipient.getName() + " ", TextColor.fromHexString("#E088FF")))
                        .append(Component.text(message, TextColor.fromHexString("#E088FF")))
                        .build());
            }
        } else {
            plugin.logResponse("Spy mode is not active.");
        }
    }
}

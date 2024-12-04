package com.nonxedy.nonchat.command;

import java.util.ArrayList;
import java.util.List;

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

public class SpyCommand implements CommandExecutor {

    private nonchat plugin;
    private PluginMessages messages;
    private PluginConfig pluginConfig;
    private boolean isSpying = false;
    private List<Player> spyPlayers = new ArrayList<>();

    public SpyCommand(nonchat plugin, PluginMessages messages, PluginConfig pluginConfig) {
        this.plugin = plugin;
        this.messages = messages;
        this.pluginConfig = pluginConfig;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);

        if (!sender.hasPermission("nonchat.spy")) {
            sender.sendMessage(Component.text(ColorUtil.parseColor(messages.getNoPermission())));
            plugin.logError("You don't have permission to send broadcast.");
            return true;
        }
        Player player = (Player) sender;
        
        try {
            if (isSpying) {
                // Disable spy mode
                isSpying = false;
                spyPlayers.remove(player);
                player.sendMessage(Component.text(ColorUtil.parseColor(messages.getSpyModeDisabled())));
                plugin.logResponse("Spy mode disabled.");
            } else {
                // Enable spy mode
                isSpying = true;
                spyPlayers.add(player);
                player.sendMessage(Component.text(ColorUtil.parseColor(messages.getSpyModeEnabled())));
                plugin.logResponse("Spy mode enabled.");
            }
        } catch (Exception e) {
            plugin.logError("There was an error toggling spy mode: " + e.getMessage());
        }

        return true;
    }

    public void onPrivateMessage(Player sender, Player recipient, String message) {
        String spyCommand = pluginConfig.getSpyFormat();
        Player player = (Player) sender;

        if (isSpying) {
            plugin.logResponse(player + " has spy mode is active. Message from " + sender.getName() + " to " + recipient.getName() + ": " + message);
            for (Player spyPlayer : spyPlayers) {
                spyPlayer.sendMessage(Component.text()
                        .append(Component.text(spyCommand.replace("{sender}", sender.getName()).replace("{target}", recipient.getName()).replace("{message}", message)))
                        .build());
            }
        } else {
            plugin.logResponse("Spy mode is not active.");
        }
    }
}

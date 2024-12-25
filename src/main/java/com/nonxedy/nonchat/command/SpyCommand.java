package com.nonxedy.nonchat.command;

import java.util.HashSet;
import java.util.Set;

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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class SpyCommand implements CommandExecutor {

    private final nonchat plugin;
    private final PluginMessages messages;
    private final PluginConfig pluginConfig;
    private final Set<Player> spyPlayers;

    public SpyCommand(nonchat plugin, PluginMessages messages, PluginConfig pluginConfig) {
        this.plugin = plugin;
        this.messages = messages;
        this.pluginConfig = pluginConfig;
        this.spyPlayers = new HashSet<>();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);

        Player player = (Player) sender;
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("only-player-command")));
            return true;
        }

        if (!player.hasPermission("nonchat.spy")) {
            player.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("Player " + player.getName() + " tried to use spy command without permission");
            return true;
        }

        toggleSpyMode(player);
        return true;
    }

    private void toggleSpyMode(Player player) {
        try {
            if (spyPlayers.contains(player)) {
                spyPlayers.remove(player);
                player.sendMessage(ColorUtil.parseComponent(messages.getString("spy-mode-disabled")));
                plugin.logResponse("Spy mode disabled for " + player.getName());
            } else {
                spyPlayers.add(player);
                player.sendMessage(ColorUtil.parseComponent(messages.getString("spy-mode-enabled")));
                plugin.logResponse("Spy mode enabled for " + player.getName());
            }
        } catch (Exception e) {
            plugin.logError("Error toggling spy mode for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ColorUtil.parseComponent(messages.getString("error-occurred")));
        }
    }

    public void onPrivateMessage(Player sender, Player target, Component message) {
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(message);
        String spyFormat = pluginConfig.getSpyFormat()
                .replace("{sender}", sender.getName())
                .replace("{target}", target.getName())
                .replace("{message}", plainMessage);

        for (Player spy : spyPlayers) {
            if (spy != sender && spy != target && spy.isOnline()) {
                spy.sendMessage(ColorUtil.parseComponent(spyFormat));
                plugin.logResponse("Spy " + spy.getName() + " received message: " + spyFormat);
            }
        }
    }

    public boolean isSpying(Player player) {
        return spyPlayers.contains(player);
    }

    public Set<Player> getSpyPlayers() {
        return new HashSet<>(spyPlayers);
    }
}

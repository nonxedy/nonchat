package com.nonxedy.nonchat.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

public class NreloadCommand implements CommandExecutor, TabCompleter {

    private final nonchat plugin;
    private final PluginMessages messages;

    public NreloadCommand(nonchat plugin, PluginMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                        @NotNull String label, @NotNull String[] args) {
        
        if (!hasReloadPermission(sender)) {
            return true;
        }

        performReload(sender);
        return true;
    }

    private boolean hasReloadPermission(CommandSender sender) {
        if (!sender.hasPermission("nonchat.nreload")) {
            sendNoPermissionMessage(sender);
            return false;
        }
        return true;
    }

    private void sendNoPermissionMessage(CommandSender sender) {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
        plugin.logError("Permission denied: nonchat.nreload");
    }

    private void performReload(CommandSender sender) {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("reloading")));
        plugin.logResponse("Initiating config reload...");

        try {
            executeReload();
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("reloaded")));
            plugin.logResponse("Configuration reload successful");
        } catch (Exception e) {
            handleReloadError(sender, e);
        }
    }

    private void executeReload() {
        plugin.reloadConfig();
        plugin.reloadDebugger();
        plugin.stopAutoBroadcastSender();
        plugin.registerUtils();
    }

    private void handleReloadError(CommandSender sender, Exception e) {
        sender.sendMessage(ColorUtil.parseComponent(messages.getString("reload-failed")));
        plugin.logError("Configuration reload failed: " + e.getMessage());
        e.printStackTrace();
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                        @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}

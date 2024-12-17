package com.nonxedy.nonchat.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

public class NreloadCommand implements CommandExecutor {

    private nonchat plugin;
    private PluginMessages messages;

    public NreloadCommand(nonchat plugin, PluginMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);
        
        if (!sender.hasPermission("nonchat.nreload")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("You don't have permission to reload the config.");
            return true;
        }

        sender.sendMessage(ColorUtil.parseComponent(messages.getString("reloading")));
        plugin.logResponse("Reloading...");

        try {
            plugin.reloadConfig();
            plugin.reloadDebugger();
            plugin.stopAutoBroadcastSender();
            plugin.registerUtils();
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("reloaded")));

            plugin.logResponse("Config reloaded.");
        } catch (Exception e) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("reload-failed")));

            plugin.logError("There was an error reloading the config: " + e.getMessage());
        }
        return true;
    }
}
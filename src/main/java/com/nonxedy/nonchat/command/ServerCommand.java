package com.nonxedy.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.format.TextColor;

public class ServerCommand implements CommandExecutor {

    private final PluginMessages messages;
    private final nonchat plugin;
    
    public ServerCommand(PluginMessages messages, nonchat plugin) {
        this.messages = messages;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);
        
        if (!sender.hasPermission("nonchat.server")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            return true;
        }

        try {
            sendServerInfo(sender);
            plugin.logResponse("Server info shown successfully");
        } catch (Exception e) {
            plugin.logError("Failed to show server info: " + e.getMessage());
            sender.sendMessage(Component.text("An error occurred while fetching server info")
                .color(TextColor.color(255, 0, 0)));
        }
        
        return true;
    }
    
    private void sendServerInfo(CommandSender sender) {
        ServerInfoBuilder info = new ServerInfoBuilder()
            .addLine(messages.getString("server-info"))
            .addInfo(messages.getString("java-version"), System.getProperty("java.version"))
            .addInfo(messages.getString("port"), String.valueOf(Bukkit.getServer().getPort()))
            .addInfo(messages.getString("os-name"), System.getProperty("os.name"))
            .addInfo(messages.getString("os-version"), System.getProperty("os.version"))
            .addInfo(messages.getString("cpu-cores"), String.valueOf(Runtime.getRuntime().availableProcessors()))
            .addInfo(messages.getString("cpu-family"), System.getenv().getOrDefault("PROCESSOR_IDENTIFIER", "Unknown"))
            .addInfo(messages.getString("number-of-plugins"), String.valueOf(Bukkit.getPluginManager().getPlugins().length))
            .addInfo(messages.getString("number-of-worlds"), String.valueOf(Bukkit.getWorlds().size()));
            
        sender.sendMessage(info.build());
    }
    
    private static class ServerInfoBuilder {
        private final Builder builder = Component.text();
        
        public ServerInfoBuilder addLine(String message) {
            builder.append(ColorUtil.parseComponent(message + "\n"));
            return this;
        }
        
        public ServerInfoBuilder addInfo(String label, String value) {
            builder.append(ColorUtil.parseComponent(label + value + "\n"));
            return this;
        }
        
        public Component build() {
            return builder.build();
        }
    }
}

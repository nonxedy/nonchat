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

public class ServerCommand implements CommandExecutor {

        private PluginMessages messages;
        private nonchat plugin;
        
        public ServerCommand(PluginMessages messages, nonchat plugin) {
            this.messages = messages;
            this.plugin = plugin;
        }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        plugin.logCommand(command.getName(), args);
        
        if (!sender.hasPermission("nonchat.server")) {
            sender.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            plugin.logError("You don't have permission to show server info.");
            return true;
        }

            int port = Bukkit.getServer().getPort();
            String version = Bukkit.getServer().getVersion();
            String javaVersion = System.getProperty("java.version");
            double cpu = Runtime.getRuntime().availableProcessors();
            String osN = System.getProperty("os.name");
            String osV = System.getProperty("os.version");
            String cpuFamily = System.getenv("PROCESSOR_IDENTIFIER");
            if (cpuFamily == null) { cpuFamily = "Unknown CPU Family"; }
            int numPlugins = Bukkit.getServer().getPluginManager().getPlugins().length;
            int numWorlds = Bukkit.getServer().getWorlds().size();

            Component serverInfo = Component.empty()
                .append(ColorUtil.parseComponent(messages.getString("server-info") + "\n"))
                .append(ColorUtil.parseComponent(messages.getString("java-version") + javaVersion + "\n"))
                .append(ColorUtil.parseComponent(messages.getString("port") + port + "\n"))
                .append(ColorUtil.parseComponent(messages.getString("os-name") + osN + "\n"))
                .append(ColorUtil.parseComponent(messages.getString("os-version") + osV + "\n"))
                .append(ColorUtil.parseComponent(messages.getString("cpu-cores") + cpu + "\n"))
                .append(ColorUtil.parseComponent(messages.getString("cpu-family") + cpuFamily + "\n"))
                .append(ColorUtil.parseComponent(messages.getString("number-of-plugins") + numPlugins + "\n"))
                .append(ColorUtil.parseComponent(messages.getString("number-of-worlds") + numWorlds));

            try {
                sender.sendMessage(serverInfo);
                plugin.logResponse("Server info shown.");
            } catch (Exception e) {
                plugin.logError("There was an error showing server info: " + e.getMessage());
            }
            return true;
    }
}

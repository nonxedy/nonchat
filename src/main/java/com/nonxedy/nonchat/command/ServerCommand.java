package com.nonxedy.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

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
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                    .build());
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

            try {
                sender.sendMessage(Component.text()
                        .append(Component.text(messages.getServerInfo() + "\n", TextColor.fromHexString("#E088FF")))
                        .append(Component.text(messages.getJavaVersion() + javaVersion + "\n", TextColor.fromHexString("#E088FF")))
                        .append(Component.text(messages.getPort() + port + "\n", TextColor.fromHexString("#E088FF")))
                        .append(Component.text(messages.getVersion() + version + "\n", TextColor.fromHexString("#E088FF")))
                        .append(Component.text(messages.getOsName() + osN + "\n", TextColor.fromHexString("#E088FF")))
                        .append(Component.text(messages.getOsVersion() + osV + "\n", TextColor.fromHexString("#E088FF")))
                        .append(Component.text(messages.getCpuCores() + cpu + "\n", TextColor.fromHexString("#E088FF")))
                        .append(Component.text(messages.getCpuFamily() + cpuFamily + "\n", TextColor.fromHexString("#E088FF")))
                        .append(Component.text(messages.getNumberOfPlugins() + numPlugins + "\n", TextColor.fromHexString("#E088FF")))
                        .append(Component.text(messages.getNumberOfWorlds() + numWorlds, TextColor.fromHexString("#E088FF")))
                        .build());

                plugin.logResponse("Server info shown.");
            } catch (Exception e) {
                plugin.logError("There was an error showing server info: " + e.getMessage());
            }
            return true;
    }
}

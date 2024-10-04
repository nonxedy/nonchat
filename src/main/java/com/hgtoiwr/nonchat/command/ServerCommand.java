package com.hgtoiwr.nonchat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.hgtoiwr.config.PluginMessages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class ServerCommand implements CommandExecutor {

        private PluginMessages messages;
        
        public ServerCommand(PluginMessages messages) {
            this.messages = messages;
        }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("nonchat.server")) {
            sender.sendMessage(Component.text()
                    .append(Component.text(messages.getNoPermission(), TextColor.fromHexString("#ADF3FD")))
                    .build());
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
    
            String infoMessage = messages.getServerInfo() + "\n" +
                    messages.getJavaVersion() + javaVersion + "\n" +
                    messages.getPort() + port + "\n" +
                    messages.getVersion() + version + "\n" +
                    messages.getOsName() + osN + "\n" +
                    messages.getOsVersion() + osV + "\n" +
                    messages.getCpuCores() + cpu + "\n" +
                    messages.getCpuFamily() + cpuFamily + "\n" +
                    messages.getNumberOfPlugins() + numPlugins + "\n" +
                    messages.getNumberOfWorlds() + numWorlds;

            sender.sendMessage(Component.text()
                    .append(Component.text(infoMessage, TextColor.fromHexString("#E088FF")))
                    .build());

            return true;
    }
}

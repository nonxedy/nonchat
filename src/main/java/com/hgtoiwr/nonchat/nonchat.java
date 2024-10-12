package com.hgtoiwr.nonchat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.hgtoiwr.config.PluginConfig;
import com.hgtoiwr.config.PluginMessages;
import com.hgtoiwr.listeners.ChatFormatListener;
import com.hgtoiwr.listeners.DeathCoordinates;
import com.hgtoiwr.listeners.DeathListener;
import com.hgtoiwr.nonchat.command.BroadcastCommand;
import com.hgtoiwr.nonchat.command.ClearCommand;
import com.hgtoiwr.nonchat.command.HelpCommand;
import com.hgtoiwr.nonchat.command.IgnoreCommand;
import com.hgtoiwr.nonchat.command.MessageCommand;
import com.hgtoiwr.nonchat.command.NreloadCommand;
import com.hgtoiwr.nonchat.command.ServerCommand;
import com.hgtoiwr.utils.AutoBroadcastSender;
import com.hgtoiwr.utils.BroadcastMessage;
import com.hgtoiwr.utils.Debugger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class nonchat extends JavaPlugin {

    private ChatFormatListener chatFormatListener;
    private DeathListener deathListener;
    private DeathCoordinates deathCoordinates;
    private AutoBroadcastSender autoBroadcastSender;
    private PluginConfig pluginConfig;
    private PluginMessages pluginMessages;
    private BroadcastMessage broadcastMessage;
    private Debugger debugger;

    public Map<UUID, Set<UUID>> ignoredPlayers = new HashMap<>();

    File plugin_directory = new File("plugins/nonchat");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        registerConfigs();
        registerCommands();
        registerListeners();
        registerUtils();

        Bukkit.getConsoleSender().sendMessage(Component.text()
            .append(Component.text("[nonchat] ", TextColor.fromHexString("#E088FF")))
            .append(Component.text("plugin enabled", TextColor.fromHexString("#52FFA6"))));
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(Component.text()
            .append(Component.text("[nonchat] ", TextColor.fromHexString("#E088FF")))
            .append(Component.text("nonchat disabled", TextColor.fromHexString("#FF5252"))));
    }

    public void registerCommands() {
        getCommand("message").setExecutor(new MessageCommand(this, pluginConfig, pluginMessages));
        getCommand("broadcast").setExecutor(new BroadcastCommand(pluginMessages, this));
        getCommand("server").setExecutor(new ServerCommand(pluginMessages, this));
        getCommand("help").setExecutor(new HelpCommand(pluginMessages, this));
        getCommand("nreload").setExecutor(new NreloadCommand(this, pluginMessages));
        getCommand("clear").setExecutor(new ClearCommand(pluginMessages, this));
        getCommand("ignore").setExecutor(new IgnoreCommand(this, pluginMessages));
    }

    public void registerListeners() {
        chatFormatListener = new ChatFormatListener(pluginConfig);
        deathListener = new DeathListener(pluginConfig);
        deathCoordinates = new DeathCoordinates();

        // Register listeners
        Bukkit.getPluginManager().registerEvents(chatFormatListener, this);
        Bukkit.getPluginManager().registerEvents(deathListener, this);
        Bukkit.getPluginManager().registerEvents(deathCoordinates, this);
    }

    public void registerUtils() {
        autoBroadcastSender = new AutoBroadcastSender(pluginConfig);

        // Register utils
        autoBroadcastSender.start();
        
        if (pluginConfig.isDebug()) {
            debugger = new Debugger();
        }
    }

    public void registerConfigs() {
        pluginConfig = new PluginConfig(broadcastMessage);
        pluginMessages = new PluginMessages();
    }

    public void reloadConfig() {
        pluginConfig.reloadConfig();
        pluginMessages.reloadConfig();
    }

    public void stopAutoBroadcastSender() {
        autoBroadcastSender.stop();
    }

    public void log(String message) {
        if (debugger != null) {
            debugger.log(message);
        }
    } 

    public void logCommand(String command, String[] args) {
        if (debugger != null) {
            debugger.log("Command: " + command + " Args: " + java.util.Arrays.toString(args));
        }
    }

    public void logResponse(String response) {
        if (debugger != null) {
            debugger.log("Response: " + response + TextColor.fromHexString("#52FFA6"));
        }
    }

    public void logError(String error) {
        if (debugger != null) {
            debugger.log("Error: " + error + TextColor.fromHexString("#FF5252"));
        }
    }

    public void reloadDebugger() {
        if ( pluginConfig.isDebug()) {
            if (debugger == null) {
                debugger = new Debugger();
            }
        } else {
            debugger = null;
        }
    }
}
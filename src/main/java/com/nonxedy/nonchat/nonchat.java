package com.nonxedy.nonchat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.nonxedy.nonchat.command.BroadcastCommand;
import com.nonxedy.nonchat.command.ClearCommand;
import com.nonxedy.nonchat.command.IgnoreCommand;
import com.nonxedy.nonchat.command.MessageCommand;
import com.nonxedy.nonchat.command.NhelpCommand;
import com.nonxedy.nonchat.command.NreloadCommand;
import com.nonxedy.nonchat.command.ServerCommand;
import com.nonxedy.nonchat.command.SpyCommand;
import com.nonxedy.nonchat.command.StaffChatCommand;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.listeners.ChatBubbleListener;
import com.nonxedy.nonchat.listeners.ChatFormatListener;
import com.nonxedy.nonchat.listeners.DeathCoordinates;
import com.nonxedy.nonchat.listeners.DeathListener;
import com.nonxedy.nonchat.utils.AutoBroadcastSender;
import com.nonxedy.nonchat.utils.BroadcastMessage;
import com.nonxedy.nonchat.utils.Debugger;

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
    private SpyCommand spyCommand;
    private Debugger debugger;
    private ChatBubbleListener chatBubbleListener;

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
        getCommand("message").setExecutor(new MessageCommand(this, pluginConfig, pluginMessages, spyCommand));
        getCommand("broadcast").setExecutor(new BroadcastCommand(pluginMessages, this));
        getCommand("server").setExecutor(new ServerCommand(pluginMessages, this));
        getCommand("nhelp").setExecutor(new NhelpCommand(pluginMessages, this));
        getCommand("nreload").setExecutor(new NreloadCommand(this, pluginMessages));
        getCommand("clear").setExecutor(new ClearCommand(pluginMessages, this));
        getCommand("ignore").setExecutor(new IgnoreCommand(this, pluginMessages));
        getCommand("sc").setExecutor(new StaffChatCommand(this, pluginMessages, pluginConfig));
        getCommand("spy").setExecutor(new SpyCommand(this, pluginMessages, pluginConfig));
    }

    public void registerListeners() {
        chatFormatListener = new ChatFormatListener(pluginConfig, pluginMessages);
        deathListener = new DeathListener(pluginConfig);
        deathCoordinates = new DeathCoordinates(pluginConfig);
        chatBubbleListener = new ChatBubbleListener(this, pluginConfig);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(chatFormatListener, this);
        Bukkit.getPluginManager().registerEvents(deathListener, this);
        Bukkit.getPluginManager().registerEvents(deathCoordinates, this);
        Bukkit.getPluginManager().registerEvents(chatBubbleListener, this);
    }

    public void registerUtils() {
        autoBroadcastSender = new AutoBroadcastSender(this, pluginConfig);

        // Register utils
        autoBroadcastSender.start();
        
        if (pluginConfig.isDebug()) {
            debugger = new Debugger(this);
            debugger.log("Plugin started successfully!");
        }
    }

    public void registerConfigs() {
        pluginConfig = new PluginConfig();
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
                debugger = new Debugger(this);
            }
        } else {
            debugger = null;
        }
    }
}
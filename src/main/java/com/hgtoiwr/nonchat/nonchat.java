package com.hgtoiwr.nonchat;

import java.io.File;

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
  
  File plugin_directory = new File("plugins/nonchat");

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

  public void onDisable() {
    Bukkit.getConsoleSender().sendMessage(Component.text()
        .append(Component.text("[nonchat] ", TextColor.fromHexString("#E088FF")))
        .append(Component.text("nonchat disabled", TextColor.fromHexString("#FF5252"))));
  }

  public void registerCommands() {
    getCommand("message").setExecutor(new MessageCommand(pluginConfig, pluginMessages));
    getCommand("broadcast").setExecutor(new BroadcastCommand(pluginMessages));
    getCommand("server").setExecutor(new ServerCommand(pluginMessages));
    getCommand("help").setExecutor(new HelpCommand(pluginMessages));
    getCommand("nreload").setExecutor(new NreloadCommand(this, pluginMessages));
    getCommand("clear").setExecutor(new ClearCommand(pluginMessages));
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
      debugger.log("Команда: " + command + " Аргументы: " + java.util.Arrays.toString(args));
    }
  }

  public void logResponse(String response) {
    if (debugger != null) {
      debugger.log("Ответ: " + response);
    }
  }

  public void logError(String error) {
    if (debugger != null) {
        debugger.log("Ошибка: " + error);
    }
  }

  public void reloadDebugger() {
    if (pluginConfig.isDebug()) {
        if (debugger == null) {
            debugger = new Debugger();
        }
    } else {
        debugger = null;
    }
  }
}

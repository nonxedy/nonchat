package com.hgtoiwr.nonchat;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.hgtoiwr.config.PluginConfig;
import com.hgtoiwr.listeners.ChatFormatListener;
import com.hgtoiwr.nonchat.command.BroadcastCommand;
import com.hgtoiwr.nonchat.command.MessageCommand;
import com.hgtoiwr.nonchat.command.NreloadCommand;
import com.hgtoiwr.nonchat.command.ServerCommand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class nonchat extends JavaPlugin {

  private ChatFormatListener chatFormatListener;
  private PluginConfig pluginConfig;
  
  File plugin_directory = new File("plugins/nonchat");

  public void onEnable() {
    registerConfigs();
    registerCommands();
    registerListeners();

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
    getCommand("message").setExecutor(new MessageCommand());
    getCommand("broadcast").setExecutor(new BroadcastCommand());
    getCommand("server").setExecutor(new ServerCommand());
    getCommand("nreload").setExecutor(new NreloadCommand(this));
  }

  public void registerListeners() {
    chatFormatListener = new ChatFormatListener(pluginConfig);

    Bukkit.getPluginManager().registerEvents(chatFormatListener, this);
  }

  public void registerConfigs() {
    pluginConfig = new PluginConfig();
  }

  public void reloadConfig() {
    pluginConfig.reloadConfig();
  }
}

package com.hgtoiwr.nonchat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.hgtoiwr.listeners.ChatFormatListener;
import com.hgtoiwr.nonchat.command.BroadcastCommand;
import com.hgtoiwr.nonchat.command.MessageCommand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class nonchat extends JavaPlugin {

  private ChatFormatListener chatFormatListener;

  public void onEnable() {
    registerCommands();
    registerListeners();

    Bukkit.getConsoleSender().sendMessage(Component.text()
        .append(Component.text("nonchat ", TextColor.fromHexString("#E088FF")))
        .append(Component.text("plugin enabled", TextColor.fromHexString("#52FFA6"))));
  }

  public void onDisable() {
    Bukkit.getConsoleSender().sendMessage(Component.text()
        .append(Component.text("nonchat ", TextColor.fromHexString("#E088FF")))
        .append(Component.text("nonchat disabled", TextColor.fromHexString("#FF5252"))));
  }

  public void registerCommands() {
    getCommand("message").setExecutor(new MessageCommand());
    getCommand("broadcast").setExecutor(new BroadcastCommand());
  }

  public void registerListeners() {
    chatFormatListener = new ChatFormatListener();

    Bukkit.getPluginManager().registerEvents(chatFormatListener, this);
  }
}

package com.hgtoiwr.nonchat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.hgtoiwr.listeners.ChatFormatListener;
import com.hgtoiwr.nonchat.command.BroadcastCommand;
import com.hgtoiwr.nonchat.command.MessageCommand;

import net.md_5.bungee.api.ChatColor;

public class nonchat extends JavaPlugin {

  private ChatFormatListener chatFormatListener;

  public void onEnable() {
    registerCommands();
    registerListeners();

    Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "[nonchat]" + ChatColor.GREEN + " nonchat enabled");
  }

  public void onDisable() {
    Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "[nonchat]" + ChatColor.RED + " nonchat disabled");
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

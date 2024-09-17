package com.hgtoiwr.nonchat;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.hgtoiwr.nonchat.command.MessageCommand;

public class nonchat extends JavaPlugin {
  private static final Logger LOGGER = Bukkit.getLogger();

  public void onEnable() {
    getCommand("message").setExecutor(new MessageCommand());

    LOGGER.info("nonchat enabled");
  }

  public void onDisable() {
    LOGGER.info("nonchat disabled");
  }
}

package com.hgtoiwr.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;

@SuppressWarnings("deprecation")
public class ChatFormatListener implements Listener {
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        String prefix = user.getCachedData().getMetaData().getPrefix();
        String suffix = user.getCachedData().getMetaData().getSuffix();
        String message = event.getMessage();

        event.setFormat(prefix + ChatColor.WHITE + player.getName() + ChatColor.RESET + suffix + "§7: §f" + message);
        Bukkit.getConsoleSender().sendMessage(prefix + ChatColor.WHITE + player.getName() + ChatColor.RESET + suffix + ChatColor.GRAY + ": " + ChatColor.WHITE + message);
    }
}

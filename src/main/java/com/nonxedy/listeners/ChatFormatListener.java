package com.nonxedy.listeners;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.nonxedy.config.PluginConfig;
import com.nonxedy.nonchat.nonchat;
import com.nonxedy.utils.WordBlocker;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;

@SuppressWarnings("deprecation")
public class ChatFormatListener implements Listener {
    
    private PluginConfig config;

    public ChatFormatListener(PluginConfig config) {
        this.config = config;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        String prefix = user.getCachedData().getMetaData().getPrefix();
        String suffix = user.getCachedData().getMetaData().getSuffix();
        String message = event.getMessage();
    
        prefix = prefix == null ? "" : prefix;
        suffix = suffix == null ? "" : suffix;
    
        WordBlocker wordBlocker = config.getWordBlocker();
    
        // Check if the message contains any banned words
        if (!wordBlocker.isMessageAllowed(message)) {
            player.sendMessage(ChatColor.RED + "Вы отправляете запрещенное слово!");
            event.setCancelled(true);
            return;
        }
    
        String chatFormat = config.getChatFormat();
        chatFormat = chatFormat.replace("{prefix}", prefix);
        chatFormat = chatFormat.replace("{suffix}", suffix);
        chatFormat = chatFormat.replace("{sender}", player.getName());
        chatFormat = chatFormat.replace("{message}", hex(message));
    
        nonchat plugin = (nonchat) Bukkit.getPluginManager().getPlugin("nonchat");
        plugin.log("Player " + event.getPlayer().getName() + " sent message: " + event.getMessage());
    
        event.setFormat(chatFormat);
    }

    private String hex(String message) {
        Pattern pattern = Pattern.compile("(#[a-fA-F0-9]{6})");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String hexCode = message.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');

            char[] ch = replaceSharp.toCharArray();
            StringBuilder builder = new StringBuilder("");
            for (char c : ch) {
                builder.append("&" + c);
            }

            message = message.replace(hexCode, builder.toString());
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message).replace('&', '§');
    }
}
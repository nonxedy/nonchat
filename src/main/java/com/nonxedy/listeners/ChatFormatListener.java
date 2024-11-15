package com.nonxedy.listeners;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.nonxedy.config.PluginConfig;
import com.nonxedy.config.PluginMessages;
import com.nonxedy.nonchat.nonchat;
import com.nonxedy.utils.WordBlocker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;

@SuppressWarnings("deprecation")
public class ChatFormatListener implements Listener {
    
    private PluginConfig config;
    private PluginMessages messages;

    public ChatFormatListener(PluginConfig config, PluginMessages messages) {
        this.config = config;
        this.messages = messages;
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

        if (!player.hasPermission("nonchat.antiblockedwords")) {
            // Check if the message contains any banned words
            if (!wordBlocker.isMessageAllowed(message)) {
                player.sendMessage(Component.text()
                        .append(Component.text(messages.getBlockedWords(), TextColor.fromHexString("#ADF3FD"))));
                event.setCancelled(true);
                return;
            }
        }

        // Check for mentions
        Pattern mentionPattern = Pattern.compile("@(\\w+)");
        Matcher mentionMatcher = mentionPattern.matcher(message);
    
        while (mentionMatcher.find()) {
            String mentionedPlayerName = mentionMatcher.group(1);
            Player mentionedPlayer = Bukkit.getPlayer(mentionedPlayerName);
            String mentionedMessages = messages.getMentioned();
        
            if (mentionedPlayer != null && mentionedPlayer.isOnline()) {
                // Send notification to the mentioned player
                mentionedPlayer.sendMessage(Component.text()
                        .append(Component.text(mentionedMessages.replace("{player}", player.getName()), TextColor.fromHexString("#52FFA6")))
                        .build());
                // Send sound to the mentioned player
                mentionedPlayer.playSound(mentionedPlayer.getLocation(), "minecraft:entity.experience_orb.pickup", 1.0F, 1.0F);
            }
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
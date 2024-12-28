package com.nonxedy.nonchat.listeners;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.utils.ColorUtil;
import com.nonxedy.nonchat.utils.WordBlocker;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

public class ChatFormatListener implements Listener {
    
    private final PluginConfig config;
    private final PluginMessages messages;
    private final Pattern mentionPattern = Pattern.compile("@(\\w+)");

    public ChatFormatListener(PluginConfig config, PluginMessages messages) {
        this.config = config;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        String messageContent = getLegacyContent(event.message());
        
        if (handleBlockedWords(player, messageContent)) {
            return;
        }

        handleMentions(player, messageContent);
        Component formattedMessage = formatMessage(player, messageContent);
        
        Bukkit.broadcast(formattedMessage);
    }

    private String getLegacyContent(Component message) {
        return LegacyComponentSerializer.legacySection().serialize(message);
    }

    private boolean handleBlockedWords(Player player, String message) {
        if (!player.hasPermission("nonchat.antiblockedwords")) {
            WordBlocker wordBlocker = config.getWordBlocker();
            if (!wordBlocker.isMessageAllowed(message)) {
                player.sendMessage(ColorUtil.parseComponent(messages.getString("blocked-words")));
                return true;
            }
        }
        return false;
    }

    private void handleMentions(Player sender, String message) {
        Matcher mentionMatcher = mentionPattern.matcher(message);
        while (mentionMatcher.find()) {
            String mentionedPlayerName = mentionMatcher.group(1);
            Player mentionedPlayer = Bukkit.getPlayer(mentionedPlayerName);
            
            if (mentionedPlayer != null && mentionedPlayer.isOnline()) {
                notifyMentionedPlayer(mentionedPlayer, sender);
            }
        }
    }

    private void notifyMentionedPlayer(Player mentioned, Player sender) {
        mentioned.sendMessage(ColorUtil.parseComponent(
            messages.getString("mentioned")
                .replace("{player}", sender.getName())
        ));
        mentioned.playSound(mentioned.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }

    private Component formatMessage(Player player, String message) {
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        String prefix = user.getCachedData().getMetaData().getPrefix();
        String suffix = user.getCachedData().getMetaData().getSuffix();
        
        prefix = prefix == null ? "" : ColorUtil.parseColor(prefix);
        suffix = suffix == null ? "" : ColorUtil.parseColor(suffix);

        String chatFormat = config.getChatFormat()
            .replace("{prefix}", prefix)
            .replace("{suffix}", suffix)
            .replace("{sender}", player.getName())
            .replace("{message}", ColorUtil.parseColor(message));

        return ColorUtil.parseComponent(chatFormat);
    }
}

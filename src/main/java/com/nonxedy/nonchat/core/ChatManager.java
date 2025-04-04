package com.nonxedy.nonchat.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.util.BubblePacketUtil;
import com.nonxedy.nonchat.util.CapsFilter;
import com.nonxedy.nonchat.util.ChatTypeUtil;
import com.nonxedy.nonchat.util.ColorUtil;
import com.nonxedy.nonchat.util.WordBlocker;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

public class ChatManager {
    private final Plugin plugin;
    private final PluginConfig config;
    private final PluginMessages messages;
    private final Pattern mentionPattern = Pattern.compile("@(\\w+)");
    private final Map<Player, List<ArmorStand>> bubbles = new HashMap<>();

    public ChatManager(Plugin plugin, PluginConfig config, PluginMessages messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        startBubbleUpdater();
    }

    public void processChat(Player player, String messageContent) {
        if (handleBlockedWords(player, messageContent)) {
            return;
        }
    
        CapsFilter capsFilter = config.getCapsFilter();
        if (capsFilter.shouldFilter(messageContent)) {
            player.sendMessage(ColorUtil.parseComponent(messages.getString("caps-filter")
                .replace("{percentage}", String.valueOf(config.getMaxCapsPercentage()))));
            return;
        }
    
        Map<String, ChatTypeUtil> chats = config.getChats();
        ChatTypeUtil chatTypeUtil = determineChat(messageContent, chats);
    
        if (!chatTypeUtil.isEnabled()) {
            player.sendMessage(ColorUtil.parseComponent(messages.getString("chat-disabled")));
            return;
        }
        
        if (chatTypeUtil.hasPermission() && !player.hasPermission(chatTypeUtil.getPermission())) {
            player.sendMessage(ColorUtil.parseComponent(messages.getString("no-permission")));
            return;
        }
    
        if (config.isChatBubblesEnabled() && player.hasPermission("nonchat.chatbubbles")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                removeBubble(player);
                createBubble(player, messageContent);
            });
        }
    
        String finalMessage = chatTypeUtil.getChatChar() != '\0' ? 
            messageContent.substring(1) : messageContent;
    
        handleMentions(player, finalMessage);
        Component formattedMessage = formatMessage(player, finalMessage, chatTypeUtil);
        broadcastMessage(player, formattedMessage, chatTypeUtil);
    }

    private void startBubbleUpdater() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<Player, List<ArmorStand>> entry : bubbles.entrySet()) {
                Player player = entry.getKey();
                List<ArmorStand> playerBubbles = entry.getValue();
                
                if (player.isOnline() && !playerBubbles.isEmpty()) {
                    Location newLoc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);
                    BubblePacketUtil.updateBubblesLocation(playerBubbles, newLoc);
                }
            }
        }, 1L, 1L);
    }

    private void createBubble(Player player, String message) {
        Location loc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);
        
        List<ArmorStand> playerBubbles = BubblePacketUtil.spawnMultilineBubble(player, message, loc);
        bubbles.put(player, playerBubbles);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeBubble(player);
        }, config.getChatBubblesDuration() * 20L);
    }

    private void removeBubble(Player player) {
        List<ArmorStand> playerBubbles = bubbles.remove(player);
        if (playerBubbles != null) {
            BubblePacketUtil.removeBubbles(playerBubbles);
        }
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

    private Component formatMessage(Player player, String message, ChatTypeUtil chatTypeUtil) {
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        String prefix = user.getCachedData().getMetaData().getPrefix();
        String suffix = user.getCachedData().getMetaData().getSuffix();
    
        prefix = prefix == null ? "" : ColorUtil.parseColor(prefix);
        suffix = suffix == null ? "" : ColorUtil.parseColor(suffix);

        // Process the message format with PlaceholderAPI first
        String baseFormat = chatTypeUtil.getFormat();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                baseFormat = PlaceholderAPI.setPlaceholders(player, baseFormat);
            } catch (Exception e) {
                // Log but continue with original format
                Bukkit.getLogger().warning("[nonchat] Error processing format placeholders: " + e.getMessage());
            }
        }
    
        baseFormat = baseFormat
            .replace("{prefix}", prefix)
            .replace("{suffix}", suffix)
            .replace("{message}", ColorUtil.parseColor(message));

        String[] parts = baseFormat.split("\\{sender\\}");

        Component finalMessage = ColorUtil.parseComponent(parts[0])
            .append(config.getHoverTextUtil().createHoverablePlayerName(player, player.getName()));

        if (parts.length > 1) {
            finalMessage = finalMessage.append(ColorUtil.parseComponent(parts[1]));
        }

        return finalMessage;
    }

    private void broadcastMessage(Player sender, Component message, ChatTypeUtil chatTypeUtil) {
        if (chatTypeUtil.isGlobal()) {
            if (chatTypeUtil.hasPermission()) {
                // If permission is specified, only send to players with that permission
                for (Player recipient : Bukkit.getOnlinePlayers()) {
                    if (recipient.hasPermission(chatTypeUtil.getPermission())) {
                        recipient.sendMessage(message);
                    }
                }
            } else {
                // If no permission is specified, send to everyone
                Bukkit.broadcast(message);
            }
        } else {
            // For local chats
            for (Player recipient : Bukkit.getOnlinePlayers()) {
                // Check range first
                if (isInRange(sender, recipient, chatTypeUtil.getRadius())) {
                    // Then check permission if specified
                    if (!chatTypeUtil.hasPermission() || recipient.hasPermission(chatTypeUtil.getPermission())) {
                        recipient.sendMessage(message);
                    }
                }
            }
        }
    }

    private boolean isInRange(Player sender, Player recipient, int radius) {
        return sender.getWorld() == recipient.getWorld() && 
                (radius == -1 || sender.getLocation().distance(recipient.getLocation()) <= radius);
    }

    private ChatTypeUtil determineChat(String message, Map<String, ChatTypeUtil> chats) {
        if (message.length() > 0) {
            char firstChar = message.charAt(0);
            return config.getChatTypeByChar(firstChar);
        }
        return config.getDefaultChatType();
    }
}
package com.nonxedy.nonchat.listeners;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.utils.ColorUtil;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

@SuppressWarnings("deprecation")
public class ChatBubbleListener implements Listener {
    
    private final Map<Player, ArmorStand> bubbles = new HashMap<>();
    private final nonchat plugin;
    private final PluginConfig config;

    public ChatBubbleListener(nonchat plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        startBubbleUpdater();
    }

    private void startBubbleUpdater() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<Player, ArmorStand> entry : bubbles.entrySet()) {
                Player player = entry.getKey();
                ArmorStand bubble = entry.getValue();
                if (player.isOnline() && !bubble.isDead()) {
                    Location newLoc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);
                    bubble.teleport(newLoc);
                }
            }
        }, 1L, 1L);
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Player player = e.getPlayer();

        if (!player.hasPermission("nonchat.chatbubbles")) {
            return;
        }

        String message = PlainTextComponentSerializer.plainText().serialize(e.message());

        Bukkit.getScheduler().runTask(plugin, () -> {
            removeBubble(player);
            createBubble(player, message);
        });
    }

    private void createBubble(Player player, String message) {
        Location loc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);
        ArmorStand bubble = player.getWorld().spawn(loc, ArmorStand.class);

        bubble.setCustomName(ColorUtil.parseColor(message));
        bubble.setCustomNameVisible(true);
        bubble.setInvisible(true);
        bubble.setGravity(false);
        bubble.setMarker(true);

        bubbles.put(player, bubble);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeBubble(player);
        }, config.getChatBubblesDuration() * 20L);
    }

    private void removeBubble(Player player) {
        ArmorStand existing = bubbles.remove(player);
        if (existing != null && !existing.isDead()) {
            existing.remove();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        removeBubble(e.getPlayer());
    }
}

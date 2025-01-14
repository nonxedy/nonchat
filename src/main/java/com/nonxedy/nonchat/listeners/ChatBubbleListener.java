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
    
    // Map to store active chat bubbles for each player
    private final Map<Player, ArmorStand> bubbles = new HashMap<>();
    // Main plugin instance
    private final nonchat plugin;
    // Configuration instance
    private final PluginConfig config;

    // Constructor initializes the listener and starts the bubble updater
    public ChatBubbleListener(nonchat plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        startBubbleUpdater();
    }

    // Starts a repeating task that updates bubble positions above players' heads
    private void startBubbleUpdater() {
        // Run task every tick (20 ticks = 1 second)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Iterate through all active bubbles
            for (Map.Entry<Player, ArmorStand> entry : bubbles.entrySet()) {
                Player player = entry.getKey();
                ArmorStand bubble = entry.getValue();
                // Update bubble position if player is online and bubble exists
                if (player.isOnline() && !bubble.isDead()) {
                    Location newLoc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);
                    bubble.teleport(newLoc);
                }
            }
        }, 1L, 1L);
    }

    // Handles chat events to create bubbles
    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Player player = e.getPlayer();

        // Check if player has permission to use chat bubbles
        if (!player.hasPermission("nonchat.chatbubbles")) {
            return;
        }

        // Convert chat message to plain text
        String message = PlainTextComponentSerializer.plainText().serialize(e.message());

        // Run bubble creation in main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            removeBubble(player);
            createBubble(player, message);
        });
    }

    // Creates a new chat bubble for a player
    private void createBubble(Player player, String message) {
        // Calculate bubble position above player
        Location loc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);
        // Spawn invisible armor stand as bubble
        ArmorStand bubble = player.getWorld().spawn(loc, ArmorStand.class);

        // Configure armor stand properties
        bubble.setCustomName(ColorUtil.parseColor(message));
        bubble.setCustomNameVisible(true);
        bubble.setInvisible(true);
        bubble.setGravity(false);
        bubble.setMarker(true);

        // Store bubble in map
        bubbles.put(player, bubble);

        // Schedule bubble removal after configured duration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeBubble(player);
        }, config.getChatBubblesDuration() * 20L);
    }

    // Removes an existing chat bubble for a player
    private void removeBubble(Player player) {
        ArmorStand existing = bubbles.remove(player);
        if (existing != null && !existing.isDead()) {
            existing.remove();
        }
    }

    // Removes chat bubble when player leaves the server
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        removeBubble(e.getPlayer());
    }
}

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
import com.nonxedy.nonchat.utils.BubblePacketUtil;
import com.nonxedy.nonchat.utils.CapsFilter;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

// Main listener class for handling chat bubble functionality
public class ChatBubbleListener implements Listener {
    
    // Map to store active chat bubbles for each player
    private final Map<Player, ArmorStand> bubbles = new HashMap<>();
    // Reference to main plugin instance
    private final nonchat plugin;
    // Reference to plugin configuration
    private final PluginConfig config;

    // Constructor initializes the listener and starts bubble updater
    public ChatBubbleListener(nonchat plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        startBubbleUpdater();
    }

    // Starts a repeating task to update bubble positions
    private void startBubbleUpdater() {
        // Run task every tick (20 ticks = 1 second)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Iterate through all active bubbles
            for (Map.Entry<Player, ArmorStand> entry : bubbles.entrySet()) {
                Player player = entry.getKey();
                ArmorStand bubble = entry.getValue();
                // Update bubble position if player is online and bubble exists
                if (player.isOnline() && !bubble.isDead()) {
                    // Calculate new location above player's head
                    Location newLoc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);
                    // Update bubble position using packets
                    BubblePacketUtil.updateBubbleLocation(bubble, newLoc);
                }
            }
        }, 1L, 1L);
    }

    // Handle chat events to create bubbles
    @EventHandler
    public void onChat(AsyncChatEvent e) {
        // Check if bubbles are enabled and player has permission
        if (!config.isChatBubblesEnabled() || !e.getPlayer().hasPermission("nonchat.chatbubbles")) {
            return;
        }

        // Add caps filter check before creating bubble
        String message = PlainTextComponentSerializer.plainText().serialize(e.message());
        CapsFilter capsFilter = config.getCapsFilter();

        // Return early if message should be blocked
        if (capsFilter.shouldFilter(message)) {
            return;
        }

        Player player = e.getPlayer();

        // Run bubble creation in main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Remove existing bubble if present
            removeBubble(player);
            // Create new bubble with message
            createBubble(player, message);
        });
    }

    // Creates a new chat bubble for a player
    private void createBubble(Player player, String message) {
        // Calculate bubble position above player
        Location loc = player.getLocation().add(0, config.getChatBubblesHeight(), 0);
        // Spawn new bubble armor stand
        ArmorStand bubble = BubblePacketUtil.spawnBubble(player, message, loc);
        // Store bubble in map
        bubbles.put(player, bubble);

        // Schedule bubble removal after configured duration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeBubble(player);
        }, config.getChatBubblesDuration() * 20L);
    }

    // Removes a player's chat bubble
    private void removeBubble(Player player) {
        // Get and remove bubble from map
        ArmorStand bubble = bubbles.remove(player);
        if (bubble != null) {
            // Remove bubble entity using packets
            BubblePacketUtil.removeBubble(bubble);
        }
    }

    // Handle player quit events
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        // Remove bubble when player leaves
        removeBubble(e.getPlayer());
    }
}

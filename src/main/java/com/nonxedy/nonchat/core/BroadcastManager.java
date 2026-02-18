package com.nonxedy.nonchat.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.util.chat.filters.LinkDetector;
import com.nonxedy.nonchat.util.core.broadcast.BroadcastMessage;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

import net.kyori.adventure.text.Component;

public class BroadcastManager {
    private final Nonchat plugin;
    private final PluginConfig config;
    private final List<BukkitTask> activeTasks;
    private List<BroadcastMessage> messageSequence;

    public BroadcastManager(Nonchat plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.activeTasks = new ArrayList<>();
        this.messageSequence = new ArrayList<>();
        start();
    }

    public void start() {
        stop();
        Map<String, BroadcastMessage> configuredMessages = config.getBroadcastMessages();

        List<BroadcastMessage> enabledMessages = configuredMessages.values().stream()
            .filter(BroadcastMessage::isEnabled)
            .collect(Collectors.toList());

        if (enabledMessages.isEmpty()) return;

        messageSequence = new ArrayList<>(enabledMessages);

        if (config.isRandomBroadcastEnabled()) {
            Collections.shuffle(messageSequence);
        } else {
            // Keep the order from the config
        }

        long delay = 0;
        long totalPeriod = messageSequence.stream().mapToLong(BroadcastMessage::getInterval).sum() * 20L;

        for (BroadcastMessage message : messageSequence) {
            BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                broadcast(Bukkit.getConsoleSender(), message.getMessage());
            }, delay, totalPeriod);
            activeTasks.add(task);
            delay += message.getInterval() * 20L;
        }
    }

    @SuppressWarnings("deprecation") // broadcastMessage() is deprecated but required for legacy server compatibility
    public void broadcast(CommandSender sender, String message) {
        try {
            Component formatted;

            // Check if message contains MiniMessage tags
            if (ColorUtil.containsMiniMessageTags(message)) {
                // Parse with MiniMessage for full tag support (including click events)
                formatted = ColorUtil.parseMiniMessageComponent(message);
            } else {
                // Use LinkDetector to make links clickable for legacy messages
                formatted = LinkDetector.makeLinksClickable(message);
            }

            // Try to use Adventure API first
            Bukkit.broadcast(formatted);
        } catch (NoSuchMethodError e) {
            // Fall back to traditional Bukkit broadcast if Adventure API is not available
            plugin.logError("Adventure API isn't available: " + e.getMessage());
            String legacyMessage = ColorUtil.parseColor(message);
            Bukkit.broadcastMessage(legacyMessage);
        }
    }

    public void stop() {
        activeTasks.forEach(BukkitTask::cancel);
        activeTasks.clear();
        messageSequence.clear();
    }

    public void reload() {
        stop();
        start();
    }
}

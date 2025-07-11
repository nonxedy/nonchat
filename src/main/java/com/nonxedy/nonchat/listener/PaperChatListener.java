package com.nonxedy.nonchat.listener;

import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import com.nonxedy.nonchat.Nonchat;
import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.service.ChatService;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class PaperChatListener extends ChatListener {

    private final Nonchat plugin;

    public PaperChatListener(Nonchat plugin, ChatManager chatManager, ChatService chatService) {
        super(chatManager, chatService);
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        CompletableFuture.runAsync(() -> {
            try {
                if (chatService != null) {
                    chatService.handleChat(player, message);
                } else if (chatManager != null) {
                    chatManager.processChat(player, message);
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Async chat processing failed: " + ex.getMessage());
            }
        });
    }
}

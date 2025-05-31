package com.nonxedy.nonchat.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.util.BubblePacketUtil;
import com.nonxedy.nonchat.util.ColorUtil;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

/**
 * Handles player join and quit events
 * Displays customizable messages when players join or leave the server
 */
public class JoinQuitListener implements Listener {
    
    private final PluginConfig config;
    private final Map<Player, List<ArmorStand>> bubbles = new HashMap<>();
    
    public JoinQuitListener(PluginConfig config) {
        this.config = config;
    }
    
    /**
     * Handles player join events
     * @param event Join event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.isJoinMessageEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        String prefix = "";
        String suffix = "";
        
        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                String lpSuffix = user.getCachedData().getMetaData().getSuffix();
                
                prefix = lpPrefix == null ? "" : ColorUtil.parseColor(lpPrefix);
                suffix = lpSuffix == null ? "" : ColorUtil.parseColor(lpSuffix);
            }
        } catch (Exception e) {
            // If LuckPerms is not available or there's an error, continue with empty prefix/suffix
        }
        
        event.joinMessage(ColorUtil.parseComponent(config.getJoinFormat()
            .replace("{prefix}", prefix)
            .replace("{player}", player.getName())
            .replace("{suffix}", suffix)));
    }
    
    /**
     * Handles player quit events
     * @param event Quit event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        List<ArmorStand> playerBubbles = bubbles.remove(event.getPlayer());
        if (playerBubbles != null) {
            BubblePacketUtil.removeBubbles(playerBubbles);
        }

        if (!config.isQuitMessageEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        String prefix = "";
        String suffix = "";
        
        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                String lpSuffix = user.getCachedData().getMetaData().getSuffix();
                
                prefix = lpPrefix == null ? "" : ColorUtil.parseColor(lpPrefix);
                suffix = lpSuffix == null ? "" : ColorUtil.parseColor(lpSuffix);
            }
        } catch (Exception e) {
            // If LuckPerms is not available or there's an error, continue with empty prefix/suffix
        }
        
        event.quitMessage(ColorUtil.parseComponent(config.getQuitFormat()
            .replace("{prefix}", prefix)
            .replace("{player}", player.getName())
            .replace("{suffix}", suffix)));
    }
}

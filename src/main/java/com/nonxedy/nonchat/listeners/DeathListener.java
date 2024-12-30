package com.nonxedy.nonchat.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.utils.ColorUtil;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

public class DeathListener implements Listener {

    private final PluginConfig config;

    public DeathListener(PluginConfig config) {
        this.config = config;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        
        if (user == null || config.getDeathFormat() == null) {
            return;
        }

        String prefix = user.getCachedData().getMetaData().getPrefix();
        String suffix = user.getCachedData().getMetaData().getSuffix();

        String deathMessage = config.getDeathFormat()
            .replace("{prefix}", prefix != null ? prefix : "")
            .replace("{suffix}", suffix != null ? suffix : "")
            .replace("{player}", player.getName());

        event.deathMessage(ColorUtil.parseComponent(deathMessage));
    }
}

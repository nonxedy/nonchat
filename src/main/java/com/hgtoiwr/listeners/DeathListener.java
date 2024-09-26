package com.hgtoiwr.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.hgtoiwr.config.PluginConfig;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

@SuppressWarnings("deprecation")
public class DeathListener implements Listener {

    private PluginConfig config;

    public DeathListener(PluginConfig config) {
        this.config = config;
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity
        ();
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        String prefix = user.getCachedData().getMetaData().getPrefix();
        String suffix = user.getCachedData().getMetaData().getSuffix();


        String deathFormat = config.getDeathFormat();
        if (deathFormat != null) {
            deathFormat = deathFormat.replace("{prefix}", prefix != null ? prefix : "");
            deathFormat = deathFormat.replace("{suffix}", suffix != null ? suffix : "");
            deathFormat = deathFormat.replace("{player}", player.getName());

            event.setDeathMessage(deathFormat);
        }
    }
}

package com.nonxedy.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class DeathCoordinates implements Listener {
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation();

        Component deathMessage = Component.text("You died on ", TextColor.fromHexString("#FFFFFF"))
                .append(Component.text(deathLocation.getWorld().getEnvironment().toString(), TextColor.fromHexString("#E088FF")))
                .append(Component.text(", location: x:", TextColor.fromHexString("#FFFFFF")))
                .append(Component.text(String.valueOf(deathLocation.getBlockX()), TextColor.fromHexString("#E088FF")))
                .append(Component.text(" y:", TextColor.fromHexString("#FFFFFF")))
                .append(Component.text(String.valueOf(deathLocation.getBlockY()), TextColor.fromHexString("#E088FF")))
                .append(Component.text(" z:", TextColor.fromHexString("#FFFFFF")))
                .append(Component.text(String.valueOf(deathLocation.getBlockZ()), TextColor.fromHexString("#E088FF")));
                
        player.sendMessage(deathMessage);
    }
}

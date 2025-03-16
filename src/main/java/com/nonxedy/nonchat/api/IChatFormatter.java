package com.nonxedy.nonchat.api;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

public interface IChatFormatter {
    Component format(String message, Player player);
    Component formatBroadcast(String message);
    Component formatPrivateMessage(Player sender, Player receiver, String message);
}

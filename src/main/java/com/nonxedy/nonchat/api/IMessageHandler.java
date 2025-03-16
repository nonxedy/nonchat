package com.nonxedy.nonchat.api;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface IMessageHandler {
    void handleChat(Player player, String message);
    void handlePrivateMessage(Player sender, Player receiver, String message);
    void handleBroadcast(CommandSender sender, String message);
    void handleStaffChat(Player sender, String message);
}

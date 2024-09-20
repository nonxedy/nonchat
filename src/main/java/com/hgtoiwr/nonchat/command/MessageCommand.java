package com.hgtoiwr.nonchat.command;


import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.md_5.bungee.api.ChatColor;

public class MessageCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("message") ||
            command.getName().equalsIgnoreCase("msg") ||
            command.getName().equalsIgnoreCase("tell") ||
            command.getName().equalsIgnoreCase("w") ||
            command.getName().equalsIgnoreCase("m") ||
            command.getName().equalsIgnoreCase("whisper")) {
            
            if (!sender.hasPermission("nonchat.message")) {
                sender.sendMessage("§x§A§D§F§3§F§DНедостаточно прав.");
                return true;
            }
            
            if (args.length < 2) {
                sender.sendMessage("§x§A§D§F§3§F§DИспользуйте: /msg <игрок> <сообщение>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§x§A§D§F§3§F§DИгрок не найден.");
                return true;
            }

            StringBuilder message = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                message.append(args[i]).append(" ");
            }

            if (sender instanceof Player) {
                Player senderPlayer = (Player) sender;
                senderPlayer.sendMessage("§x§E§0§8§8§F§FВы -> " + target.getName() + "§x§E§0§8§8§F§F: " + message.toString().trim());
                Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "[nonchat] " + ChatColor.DARK_PURPLE + sender.getName() + ChatColor.GRAY + " -> " + ChatColor.DARK_PURPLE + target.getName() + ChatColor.GRAY + ": " + ChatColor.WHITE + message.toString().trim());
            }

            target.sendMessage("§x§E§0§8§8§F§F" + sender.getName() + "§x§E§0§8§8§F§F -> Вы: " + message.toString().trim());
            Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "[nonchat] " + ChatColor.DARK_PURPLE + sender.getName() + ChatColor.GRAY + " -> " + ChatColor.DARK_PURPLE + target.getName() + ChatColor.GRAY + ": " + ChatColor.WHITE + message.toString().trim());
            return true;
        }
        return false;
    }
}
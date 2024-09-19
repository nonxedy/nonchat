package com.hgtoiwr.nonchat.command;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public class BroadcastCommand implements CommandExecutor {

    private Logger logger = Bukkit.getLogger();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("broadcast") ||
            command.getName().equalsIgnoreCase("bc")) {

            if (!sender.hasPermission("nonchat.broadcast")) {
                sender.sendMessage("§x§A§D§F§3§F§DНедостаточно прав.");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage("§x§A§D§F§3§F§DИспользуйте: /bc <сообщение>");
                return true;
            }

            StringBuilder message = new StringBuilder();
            for (String arg : args) {
                message.append(arg).append(" ");
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage("§x§E§0§8§8§F§F{");
                player.sendMessage("§x§E§0§8§8§F§F{ Оповещение§f: " + message.toString().trim());
                player.sendMessage("§x§E§0§8§8§F§F{");

                logger.info("{ Оповещение: " + message.toString().trim() + "}");
            }

            return true;
        }

        return false;

    }

}

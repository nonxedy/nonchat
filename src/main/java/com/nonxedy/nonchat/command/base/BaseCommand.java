package com.nonxedy.nonchat.command.base;

import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nonxedy.nonchat.service.ChatService;
import com.nonxedy.nonchat.service.ConfigService;
import com.nonxedy.nonchat.util.ColorUtil;

public abstract class BaseCommand implements CommandExecutor {
    protected final ChatService chatService;
    protected final ConfigService configService;

    public BaseCommand(ChatService chatService, ConfigService configService) {
        this.chatService = chatService;
        this.configService = configService;
    }

    protected void validatePermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(ColorUtil.parseComponent(configService.getMessage("no-permission")));
            throw new CommandException("No permission");
        }
    }

    protected void validatePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.parseComponent(configService.getMessage("player-only")));
            throw new CommandException("Not a player");
        }
    }

    protected void validateArgs(String[] args, int minLength) {
        if (args.length < minLength) {
            throw new CommandException("Invalid arguments");
        }
    }

    protected void logCommand(String command, String[] args) {
        configService.logCommand(command, args);
    }

    protected void logError(String error) {
        configService.logError(error);
    }

    protected String getMessage(String key) {
        return configService.getMessage(key);
    }
}

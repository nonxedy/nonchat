package com.nonxedy.nonchat.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.config.PluginMessages;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;

public class DiscordCommandHandler extends ListenerAdapter {
    
    private final nonchat plugin;
    private final String prefix;
    private final ConsoleCommandSender console;
    private final PluginMessages messages;

    public DiscordCommandHandler(nonchat plugin, PluginMessages messages) {
        this.plugin = plugin;
        this.prefix = plugin.getConfig().getString("discord.commands.prefix", "!");
        this.console = Bukkit.getConsoleSender();
        this.messages = messages;
    }

    @Subscribe
    public void onDiscordMessage(DiscordGuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        
        String message = event.getMessage().getContentRaw();
        if (!message.startsWith(prefix)) return;

        String fullCommand = message.substring(prefix.length());
        
        if (hasPermission(event)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean success = Bukkit.dispatchCommand(console, fullCommand);
                event.getChannel().sendMessage(
                    messages.getString(success ? 
                    "discord.command.success" : 
                    "discord.command.failed")).queue();
            });
        }
    }

    private boolean hasPermission(DiscordGuildMessageReceivedEvent event) {
        return event.getMember() != null && 
                event.getMember().hasPermission(Permission.ADMINISTRATOR);
    }
}

package com.nonxedy.nonchat.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.command.impl.BroadcastCommand;
import com.nonxedy.nonchat.command.impl.ClearCommand;
import com.nonxedy.nonchat.command.impl.IgnoreCommand;
import com.nonxedy.nonchat.command.impl.MeCommand;
import com.nonxedy.nonchat.command.impl.MessageCommand;
import com.nonxedy.nonchat.command.impl.NhelpCommand;
import com.nonxedy.nonchat.command.impl.NreloadCommand;
import com.nonxedy.nonchat.command.impl.RollCommand;
import com.nonxedy.nonchat.command.impl.ServerCommand;
import com.nonxedy.nonchat.command.impl.SpyCommand;
import com.nonxedy.nonchat.command.impl.StaffChatCommand;

public class CommandService {
    private final nonchat plugin;
    private final ChatService chatService;
    private final ConfigService configService;
    private final Map<String, CommandExecutor> commands;

    public CommandService(nonchat plugin, ChatService chatService, ConfigService configService) {
        this.plugin = plugin;
        this.chatService = chatService;
        this.configService = configService;
        this.commands = new HashMap<>();
        registerCommands();
    }

    private void registerCommands() {
        // Get the SpyCommand from the plugin to ensure we use the same instance
        SpyCommand spyCommand = plugin.getSpyCommand();
        IgnoreCommand ignoreCommand = plugin.getIgnoreCommand();
        
        // Private messaging commands
        MessageCommand messageCommand = new MessageCommand(plugin, configService.getConfig(), configService.getMessages(), spyCommand);
        registerCommand("msg", messageCommand);
        registerCommand("tell", messageCommand);
        registerCommand("w", messageCommand);
        registerCommand("message", messageCommand);
    
        // Chat management commands
        registerCommand("broadcast", new BroadcastCommand(configService.getMessages(), plugin));
        registerCommand("sc", new StaffChatCommand(plugin, configService.getMessages(), configService.getConfig()));
        registerCommand("spy", spyCommand); // Use the existing instance
        registerCommand("clear", new ClearCommand(configService.getMessages(), plugin));
        registerCommand("ignore", ignoreCommand); // Use the existing instance
    
        // Roleplay commands
        registerCommand("me", new MeCommand(plugin, configService.getConfig(), configService.getMessages()));
        registerCommand("roll", new RollCommand(plugin, configService.getConfig(), configService.getMessages()));
    
        // Utility commands
        registerCommand("server", new ServerCommand(configService.getMessages(), plugin));
        registerCommand("nhelp", new NhelpCommand(configService.getMessages(), plugin));
        registerCommand("nreload", new NreloadCommand(plugin, configService));
    }

    public void registerCommand(String name, CommandExecutor executor) {
        commands.put(name.toLowerCase(), executor);
        PluginCommand pluginCommand = plugin.getCommand(name);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(executor);
            if (executor instanceof TabCompleter) {
                pluginCommand.setTabCompleter((TabCompleter) executor);
            }
        }
    }

    public CommandExecutor getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    public void reloadCommands() {
        commands.clear();
        registerCommands();
    }

    public Set<String> getRegisteredCommands() {
        return Collections.unmodifiableSet(commands.keySet());
    }

    public void unregisterAll() {
        commands.forEach((name, executor) -> {
            PluginCommand pluginCommand = plugin.getCommand(name);
            if (pluginCommand != null) {
                pluginCommand.setExecutor(null);
                pluginCommand.setTabCompleter(null);
            }
        });
        commands.clear();
    }
}



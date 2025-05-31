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
import com.nonxedy.nonchat.command.impl.ChannelCommand;
import com.nonxedy.nonchat.command.impl.ClearCommand;
import com.nonxedy.nonchat.command.impl.IgnoreCommand;
import com.nonxedy.nonchat.command.impl.MeCommand;
import com.nonxedy.nonchat.command.impl.MessageCommand;
import com.nonxedy.nonchat.command.impl.NonchatCommand;
import com.nonxedy.nonchat.command.impl.RollCommand;
import com.nonxedy.nonchat.command.impl.SpyCommand;

/**
 * Service for registering and managing plugin commands.
 */
public class CommandService {
    private final nonchat plugin;
    private final ChatService chatService;
    private final ConfigService configService;
    private final Map<String, CommandExecutor> commands;

    /**
     * Creates new command service.
     *
     * @param plugin Plugin instance
     * @param chatService Chat service
     * @param configService Configuration service
     */
    public CommandService(nonchat plugin, ChatService chatService, ConfigService configService) {
        this.plugin = plugin;
        this.chatService = chatService;
        this.configService = configService;
        this.commands = new HashMap<>();
        registerCommands();
    }

    /**
     * Registers all plugin commands.
     */
    private void registerCommands() {
        plugin.logResponse("Registering commands...");
        
        // Get command instances from plugin
        SpyCommand spyCommand = plugin.getSpyCommand();
        IgnoreCommand ignoreCommand = plugin.getIgnoreCommand();
        
        // Private messaging commands
        MessageCommand messageCommand = new MessageCommand(plugin, configService.getConfig(), 
                                                          configService.getMessages(), spyCommand);
        registerCommand("msg", messageCommand);
        registerCommand("tell", messageCommand);
        registerCommand("w", messageCommand);
        registerCommand("message", messageCommand);
    
        // Chat management commands
        registerCommand("broadcast", new BroadcastCommand(configService.getMessages(), plugin));
        registerCommand("spy", spyCommand);
        registerCommand("clear", new ClearCommand(configService.getMessages(), plugin));
        registerCommand("ignore", ignoreCommand);
        registerCommand("channel", new ChannelCommand(plugin, plugin.getChatManager(), configService.getMessages()));
    
        // Roleplay commands
        registerCommand("me", new MeCommand(plugin, configService.getConfig(), configService.getMessages()));
        registerCommand("roll", new RollCommand(plugin, configService.getConfig(), configService.getMessages()));
    
        // Utility commands
        registerCommand("nonchat", new NonchatCommand(plugin, configService));
        
        plugin.logResponse("Registered " + commands.size() + " traditional commands");
    }

    /**
     * Registers command with Bukkit.
     *
     * @param name Command name
     * @param executor Command executor
     */
    public void registerCommand(String name, CommandExecutor executor) {
        commands.put(name.toLowerCase(), executor);
        PluginCommand pluginCommand = plugin.getCommand(name);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(executor);
            if (executor instanceof TabCompleter) {
                pluginCommand.setTabCompleter((TabCompleter) executor);
            }
            plugin.logResponse("Registered traditional command: " + name);
        } else {
            plugin.logError("Command '" + name + "' not found in plugin.yml");
        }
    }
    
    /**
     * Gets command executor by name.
     *
     * @param name Command name
     * @return Command executor or null if not found
     */
    public CommandExecutor getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    /**
     * Reloads all commands.
     */
    public void reloadCommands() {
        plugin.logResponse("Reloading commands...");
        
        registerCommands();
        
        plugin.logResponse("Commands reloaded successfully");
    }

    /**
     * Gets set of registered command names.
     *
     * @return Set of command names
     */
    public Set<String> getRegisteredCommands() {
        return Collections.unmodifiableSet(commands.keySet());
    }

    /**
     * Unregisters all commands.
     */
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

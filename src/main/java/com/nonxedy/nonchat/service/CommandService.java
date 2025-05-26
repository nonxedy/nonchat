package com.nonxedy.nonchat.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

import com.nonxedy.nonchat.api.registry.CommandRegistry;
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
 * Supports both traditional and annotation-based commands.
 */
public class CommandService {
    private final nonchat plugin;
    private final ChatService chatService;
    private final ConfigService configService;
    private final Map<String, CommandExecutor> commands;
    private final CommandRegistry annotatedRegistry;

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
        this.annotatedRegistry = new CommandRegistry(plugin);
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
     * Registers traditional command with Bukkit.
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
     * Registers annotated command.
     * 
     * @param commandInstance Command instance with annotations
     */
    public void registerAnnotatedCommand(Object commandInstance) {
        annotatedRegistry.registerCommand(commandInstance);
    }
    
    /**
     * Registers multiple annotated commands.
     * 
     * @param commandInstances Command instances to register
     */
    public void registerAnnotatedCommands(Object... commandInstances) {
        annotatedRegistry.registerCommands(commandInstances);
    }

    /**
     * Gets traditional command executor by name.
     *
     * @param name Command name
     * @return Command executor or null if not found
     */
    public CommandExecutor getCommand(String name) {
        return commands.get(name.toLowerCase());
    }
    
    /**
     * Gets annotated command registry.
     * 
     * @return Command registry
     */
    public CommandRegistry getAnnotatedRegistry() {
        return annotatedRegistry;
    }

    /**
     * Reloads all commands.
     */
    public void reloadCommands() {
        plugin.logResponse("Reloading commands...");
        
        // Clear and re-register traditional commands
        unregisterTraditionalCommands();
        registerCommands();
        
        // Annotated commands don't need reloading as they're instance-based
        plugin.logResponse("Commands reloaded successfully");
    }

    /**
     * Gets set of registered traditional command names.
     *
     * @return Set of command names
     */
    public Set<String> getRegisteredCommands() {
        return Collections.unmodifiableSet(commands.keySet());
    }
    
    /**
     * Gets set of registered annotated command names.
     * 
     * @return Set of annotated command names
     */
    public Set<String> getRegisteredAnnotatedCommands() {
        return annotatedRegistry.getRegisteredCommands();
    }

    /**
     * Unregisters all traditional commands.
     */
    private void unregisterTraditionalCommands() {
        commands.forEach((name, executor) -> {
            PluginCommand pluginCommand = plugin.getCommand(name);
            if (pluginCommand != null) {
                pluginCommand.setExecutor(null);
                pluginCommand.setTabCompleter(null);
            }
        });
        commands.clear();
    }

    /**
     * Unregisters all commands (traditional and annotated).
     */
    public void unregisterAll() {
        plugin.logResponse("Unregistering all commands...");
        
        unregisterTraditionalCommands();
        annotatedRegistry.unregisterAll();
        
        plugin.logResponse("All commands unregistered");
    }
    
    /**
     * Gets total command count.
     * 
     * @return Total number of registered commands
     */
    public int getTotalCommandCount() {
        return commands.size() + annotatedRegistry.getCommandCount();
    }
    
    /**
     * Checks if command is registered (traditional or annotated).
     * 
     * @param name Command name
     * @return true if command is registered
     */
    public boolean isCommandRegistered(String name) {
        return commands.containsKey(name.toLowerCase()) || 
               annotatedRegistry.getCommand(name.toLowerCase()) != null;
    }
}

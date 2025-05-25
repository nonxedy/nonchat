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
 * Сервис для регистрации и управления командами плагина.
 */
public class CommandService {
    private final nonchat plugin;
    private final ChatService chatService;
    private final ConfigService configService;
    private final Map<String, CommandExecutor> commands;

    /**
     * Создает новый сервис команд.
     *
     * @param plugin Экземпляр плагина
     * @param chatService Сервис чата
     * @param configService Сервис конфигурации
     */
    public CommandService(nonchat plugin, ChatService chatService, ConfigService configService) {
        this.plugin = plugin;
        this.chatService = chatService;
        this.configService = configService;
        this.commands = new HashMap<>();
        registerCommands();
    }

    /**
     * Регистрирует все команды плагина.
     */
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
        registerCommand("spy", spyCommand); // Use the existing instance
        registerCommand("clear", new ClearCommand(configService.getMessages(), plugin));
        registerCommand("ignore", ignoreCommand); // Use the existing instance
        registerCommand("channel", new ChannelCommand(plugin, plugin.getChatManager(), configService.getMessages()));
    
        // Roleplay commands
        registerCommand("me", new MeCommand(plugin, configService.getConfig(), configService.getMessages()));
        registerCommand("roll", new RollCommand(plugin, configService.getConfig(), configService.getMessages()));
    
        // Utility commands
        registerCommand("nonchat", new NonchatCommand(plugin, configService));
    }

    /**
     * Регистрирует команду в Bukkit.
     *
     * @param name Имя команды
     * @param executor Исполнитель команды
     */
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

    /**
     * Получает исполнителя команды по имени.
     *
     * @param name Имя команды
     * @return Исполнитель команды
     */
    public CommandExecutor getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    /**
     * Перезагружает все команды.
     */
    public void reloadCommands() {
        commands.clear();
        registerCommands();
    }

    /**
     * Получает множество имен зарегистрированных команд.
     *
     * @return Множество имен команд
     */
    public Set<String> getRegisteredCommands() {
        return Collections.unmodifiableSet(commands.keySet());
    }

    /**
     * Отменяет регистрацию всех команд.
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

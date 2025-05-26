package com.nonxedy.nonchat.api.registry;

import com.nonxedy.nonchat.api.adapter.AnnotatedCommandAdapter;
import com.nonxedy.nonchat.api.annotation.Command;
import com.nonxedy.nonchat.nonchat;

import org.bukkit.command.PluginCommand;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Registry for automatic command registration and execution based on annotations.
 * This class scans and registers all classes with @Command annotation.
 */
public class CommandRegistry {
    private final nonchat plugin;
    private final Map<String, AnnotatedCommandAdapter> registeredCommands = new HashMap<>();
    
    /**
     * Creates new command registry.
     *
     * @param plugin Plugin instance
     */
    public CommandRegistry(nonchat plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Scans and registers commands from specified package.
     * 
     * @param packageName Package to scan for commands
     */
    public void scanAndRegisterCommands(String packageName) {
        plugin.logResponse("Scanning for commands in package: " + packageName);
        
        try {
            Reflections reflections = new Reflections(packageName, Scanners.TypesAnnotated);
            Set<Class<?>> commandClasses = reflections.getTypesAnnotatedWith(Command.class);
            
            int registeredCount = 0;
            for (Class<?> commandClass : commandClasses) {
                try {
                    Object commandInstance = createCommandInstance(commandClass);
                    if (commandInstance != null) {
                        registerCommand(commandInstance);
                        registeredCount++;
                    }
                } catch (Exception e) {
                    plugin.logError("Failed to register command class " + commandClass.getName() + ": " + e.getMessage());
                    if (plugin.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                }
            }
            
            plugin.logResponse("Successfully registered " + registeredCount + " commands from package scan");
            
        } catch (Exception e) {
            plugin.logError("Failed to scan package " + packageName + ": " + e.getMessage());
            if (plugin.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Creates command instance using reflection.
     * Tries different constructor patterns.
     */
    private Object createCommandInstance(Class<?> commandClass) {
        try {
            // Try constructor with plugin parameter
            try {
                Constructor<?> pluginConstructor = commandClass.getConstructor(nonchat.class);
                return pluginConstructor.newInstance(plugin);
            } catch (NoSuchMethodException ignored) {}
            
            // Try default constructor
            try {
                Constructor<?> defaultConstructor = commandClass.getConstructor();
                return defaultConstructor.newInstance();
            } catch (NoSuchMethodException ignored) {}
            
            plugin.logError("No suitable constructor found for command class: " + commandClass.getName());
            return null;
            
        } catch (Exception e) {
            plugin.logError("Failed to create instance of command class " + commandClass.getName() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Registers a command instance.
     * 
     * @param commandInstance Command instance with @Command annotation
     */
    public void registerCommand(Object commandInstance) {
        Class<?> commandClass = commandInstance.getClass();
        Command commandAnnotation = commandClass.getAnnotation(Command.class);
        
        if (commandAnnotation == null) {
            plugin.logError("Class " + commandClass.getName() + " is not annotated with @Command");
            return;
        }
        
        try {
            // Create adapter
            AnnotatedCommandAdapter adapter = new AnnotatedCommandAdapter(plugin, commandInstance);
            
            // Register main command
            String commandName = commandAnnotation.name().toLowerCase();
            if (registerCommandAdapter(commandName, adapter)) {
                // Register aliases
                for (String alias : commandAnnotation.aliases()) {
                    registerCommandAdapter(alias.toLowerCase(), adapter);
                }
                
                plugin.logResponse("Successfully registered command: " + commandAnnotation.name() + 
                                 (commandAnnotation.aliases().length > 0 ? 
                                  " (aliases: " + String.join(", ", commandAnnotation.aliases()) + ")" : ""));
            }
            
        } catch (Exception e) {
            plugin.logError("Failed to register command " + commandAnnotation.name() + ": " + e.getMessage());
            if (plugin.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Registers command adapter with Bukkit.
     */
    private boolean registerCommandAdapter(String name, AnnotatedCommandAdapter adapter) {
        PluginCommand pluginCommand = plugin.getCommand(name);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(adapter);
            pluginCommand.setTabCompleter(adapter);
            registeredCommands.put(name, adapter);
            return true;
        } else {
            plugin.logError("Command '" + name + "' not found in plugin.yml - make sure to add it to your plugin.yml file");
            return false;
        }
    }
    
    /**
     * Registers multiple command instances.
     * 
     * @param commandInstances Command instances to register
     */
    public void registerCommands(Object... commandInstances) {
        for (Object commandInstance : commandInstances) {
            registerCommand(commandInstance);
        }
    }
    
    /**
     * Gets registered command adapter by name.
     * 
     * @param name Command name
     * @return Command adapter or null if not found
     */
    public AnnotatedCommandAdapter getCommand(String name) {
        return registeredCommands.get(name.toLowerCase());
    }
    
    /**
     * Gets all registered command names.
     * 
     * @return Set of command names
     */
    public Set<String> getRegisteredCommands() {
        return Collections.unmodifiableSet(registeredCommands.keySet());
    }
    
    /**
     * Unregisters all commands.
     */
    public void unregisterAll() {
        registeredCommands.forEach((name, adapter) -> {
            PluginCommand pluginCommand = plugin.getCommand(name);
            if (pluginCommand != null) {
                pluginCommand.setExecutor(null);
                pluginCommand.setTabCompleter(null);
            }
        });
        registeredCommands.clear();
        plugin.logResponse("Unregistered all annotated commands");
    }
    
    /**
     * Gets count of registered commands.
     * 
     * @return Number of registered commands
     */
    public int getCommandCount() {
        return registeredCommands.size();
    }
    
    /**
     * Gets detailed information about registered commands.
     * 
     * @return Map of command names to their descriptions
     */
    public Map<String, String> getCommandInfo() {
        Map<String, String> info = new HashMap<>();
        registeredCommands.forEach((name, adapter) -> {
            String description = adapter.getDescription();
            info.put(name, description.isEmpty() ? "No description" : description);
        });
        return info;
    }
}

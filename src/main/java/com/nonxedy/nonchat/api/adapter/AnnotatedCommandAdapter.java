package com.nonxedy.nonchat.api.adapter;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.nonxedy.nonchat.api.annotation.CommandHandler;
import com.nonxedy.nonchat.api.annotation.Parameter;
import com.nonxedy.nonchat.nonchat;
import com.nonxedy.nonchat.util.ColorUtil;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Adapter for annotated commands implementing CommandExecutor.
 * Allows using new annotation-based API for commands.
 */
public class AnnotatedCommandAdapter implements CommandExecutor, TabCompleter {
    
    private final Object commandInstance;
    private final List<HandlerMethod> handlers;
    private final com.nonxedy.nonchat.api.annotation.Command commandAnnotation;
    private final nonchat plugin;
    
    /**
     * Creates new adapter for annotated command.
     *
     * @param plugin Plugin instance
     * @param commandInstance Command class instance with annotations
     */
    public AnnotatedCommandAdapter(nonchat plugin, Object commandInstance) {
        this.plugin = plugin;
        this.commandInstance = commandInstance;
        
        // Get @Command annotation from class
        this.commandAnnotation = commandInstance.getClass().getAnnotation(
            com.nonxedy.nonchat.api.annotation.Command.class);
        
        if (this.commandAnnotation == null) {
            throw new IllegalArgumentException("Class " + commandInstance.getClass().getName() 
                                              + " is not annotated with @Command");
        }
        
        // Find all methods with @CommandHandler annotation
        this.handlers = new ArrayList<>();
        for (Method method : commandInstance.getClass().getDeclaredMethods()) {
            CommandHandler handlerAnnotation = method.getAnnotation(CommandHandler.class);
            if (handlerAnnotation != null) {
                method.setAccessible(true);
                handlers.add(new HandlerMethod(method, handlerAnnotation));
            }
        }
        
        // Sort handlers by priority
        handlers.sort(Comparator.comparingInt(h -> h.annotation.priority()));
        
        plugin.logResponse("Registered command: " + commandAnnotation.name() + 
                          " with " + handlers.size() + " handlers");
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, String[] args) {
        // Check player only restriction
        if (commandAnnotation.playerOnly() && !(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.parseColor("&cThis command is only available for players!"));
            return true;
        }
        
        // Check permission
        if (!commandAnnotation.permission().isEmpty() && !sender.hasPermission(commandAnnotation.permission())) {
            sender.sendMessage(ColorUtil.parseColor("&cYou don't have permission to use this command!"));
            return true;
        }
        
        // Find suitable handler
        for (HandlerMethod handler : handlers) {
            CommandHandler annotation = handler.annotation;
            
            // Check argument count
            if (args.length < annotation.minArgs()) continue;
            if (annotation.maxArgs() != -1 && args.length > annotation.maxArgs()) continue;
            
            try {
                // Prepare method arguments
                Object[] methodArgs = prepareArguments(sender, args, handler.method);
                
                // Invoke handler method
                handler.method.invoke(commandInstance, methodArgs);
                return true;
            } catch (Exception e) {
                plugin.logError("Error executing command handler: " + e.getMessage());
                if (plugin.isDebugEnabled()) {
                    e.printStackTrace();
                }
                sender.sendMessage(ColorUtil.parseColor("&cAn error occurred while executing the command!"));
                return true;
            }
        }
        
        // No suitable handler found
        if (!handlers.isEmpty()) {
            CommandHandler firstHandler = handlers.get(0).annotation;
            String usage = firstHandler.usage();
            if (!usage.isEmpty()) {
                sender.sendMessage(ColorUtil.parseColor("&cIncorrect command usage:"));
                sender.sendMessage(ColorUtil.parseColor("&7" + usage));
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                     @NotNull String alias, String[] args) {
        // Basic tab completion - return online player names for first argument
        if (args.length == 1) {
            return null; // Let Bukkit handle player name completion
        }
        return Collections.emptyList();
    }
    
    /**
     * Prepares arguments for method invocation.
     */
    private Object[] prepareArguments(CommandSender sender, String[] args, Method method) {
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Object[] result = new Object[parameters.length];
        int argIndex = 0;
        
        for (int i = 0; i < parameters.length; i++) {
            java.lang.reflect.Parameter param = parameters[i];
            
            // Handle @Parameter annotation
            if (param.isAnnotationPresent(Parameter.class)) {
                Parameter annotation = param.getAnnotation(Parameter.class);
                result[i] = processParameter(sender, args, annotation, argIndex);
                
                // Increment arg index for non-sender parameters
                if (annotation.type() != Parameter.ParameterType.SENDER) {
                    if (annotation.joined()) {
                        argIndex = args.length; // Consume all remaining args
                    } else {
                        argIndex++;
                    }
                }
            } else {
                // Handle parameters without annotation
                Class<?> paramType = param.getType();
                if (CommandSender.class.isAssignableFrom(paramType)) {
                    result[i] = sender;
                } else if (paramType == String[].class) {
                    result[i] = args;
                } else {
                    throw new IllegalArgumentException("Unsupported parameter type: " + paramType.getName());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Processes parameter based on annotation.
     */
    private Object processParameter(CommandSender sender, String[] args, Parameter annotation, int argIndex) {
        switch (annotation.type()) {
            case SENDER:
                return sender;
                
            case PLAYER:
                if (argIndex >= args.length) {
                    if (annotation.optional()) {
                        return null;
                    }
                    throw new IllegalArgumentException("Missing required player parameter");
                }
                Player player = Bukkit.getPlayer(args[argIndex]);
                if (player == null || !player.isOnline()) {
                    throw new IllegalArgumentException("Player not found: " + args[argIndex]);
                }
                return player;
                
            case STRING:
                if (argIndex >= args.length) {
                    if (annotation.optional()) {
                        return annotation.defaultValue();
                    }
                    throw new IllegalArgumentException("Missing required string parameter");
                }
                
                if (annotation.joined()) {
                    StringBuilder joined = new StringBuilder(args[argIndex]);
                    for (int i = argIndex + 1; i < args.length; i++) {
                        joined.append(" ").append(args[i]);
                    }
                    return joined.toString();
                } else {
                    return args[argIndex];
                }
                
            case INTEGER:
                if (argIndex >= args.length) {
                    if (annotation.optional()) {
                        return annotation.defaultValue().isEmpty() ? 0 : Integer.parseInt(annotation.defaultValue());
                    }
                    throw new IllegalArgumentException("Missing required integer parameter");
                }
                try {
                    return Integer.parseInt(args[argIndex]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid integer: " + args[argIndex]);
                }
                
            case DOUBLE:
                if (argIndex >= args.length) {
                    if (annotation.optional()) {
                        return annotation.defaultValue().isEmpty() ? 0.0 : Double.parseDouble(annotation.defaultValue());
                    }
                    throw new IllegalArgumentException("Missing required double parameter");
                }
                try {
                    return Double.parseDouble(args[argIndex]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid double: " + args[argIndex]);
                }
                
            case BOOLEAN:
                if (argIndex >= args.length) {
                    if (annotation.optional()) {
                        return annotation.defaultValue().isEmpty() ? false : Boolean.parseBoolean(annotation.defaultValue());
                    }
                    throw new IllegalArgumentException("Missing required boolean parameter");
                }
                return Boolean.parseBoolean(args[argIndex]);
                
            default:
                throw new IllegalArgumentException("Unsupported parameter type: " + annotation.type());
        }
    }
    
    /**
     * Handler method information.
     */
    private static class HandlerMethod {
        private final Method method;
        private final CommandHandler annotation;
        
        public HandlerMethod(Method method, CommandHandler annotation) {
            this.method = method;
            this.annotation = annotation;
        }
    }

    /**
     * Gets command name from annotation.
     */
    public String getName() {
        return commandAnnotation.name();
    }
    
    /**
     * Gets command aliases from annotation.
     */
    public String[] getAliases() {
        return commandAnnotation.aliases();
    }
    
    /**
     * Gets command description from annotation.
     */
    public String getDescription() {
        return commandAnnotation.description();
    }
}

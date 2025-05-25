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
import com.nonxedy.nonchat.api.annotation.Sender;
import com.nonxedy.nonchat.nonchat;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Адаптер для аннотированных команд, реализующий CommandExecutor.
 * Позволяет использовать новый API с аннотациями для команд.
 */
public class AnnotatedCommandAdapter implements CommandExecutor, TabCompleter {
    
    private final Object commandInstance;
    private final List<HandlerInfo> handlers;
    private final com.nonxedy.nonchat.api.annotation.Command commandAnnotation;
    private final nonchat plugin;
    
    /**
     * Создает новый адаптер для аннотированной команды.
     *
     * @param plugin Экземпляр плагина
     * @param commandInstance Экземпляр класса команды с аннотациями
     */
    public AnnotatedCommandAdapter(nonchat plugin, Object commandInstance) {
        this.plugin = plugin;
        this.commandInstance = commandInstance;
        
        // Получаем аннотацию @Command из класса
        this.commandAnnotation = commandInstance.getClass().getAnnotation(
            com.nonxedy.nonchat.api.annotation.Command.class);
        
        if (this.commandAnnotation == null) {
            throw new IllegalArgumentException("Class " + commandInstance.getClass().getName() 
                                              + " is not annotated with @Command");
        }
        
        // Находим все методы с аннотацией @CommandHandler
        this.handlers = new ArrayList<>();
        for (Method method : commandInstance.getClass().getDeclaredMethods()) {
            CommandHandler handlerAnnotation = method.getAnnotation(CommandHandler.class);
            if (handlerAnnotation != null) {
                handlers.add(new HandlerInfo(method, handlerAnnotation));
            }
        }
        
        // Сортируем обработчики по приоритету
        handlers.sort(Comparator.comparingInt(h -> h.annotation.priority()));
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, String[] args) {
        // Проверка на Player Only
        if (commandAnnotation.playerOnly() && !(sender instanceof Player)) {
            sender.sendMessage("§cЭта команда доступна только для игроков!");
            return true;
        }
        
        // Проверка разрешения
        if (!commandAnnotation.permission().isEmpty() && !sender.hasPermission(commandAnnotation.permission())) {
            sender.sendMessage("§cУ вас нет разрешения на использование этой команды!");
            return true;
        }
        
        // Поиск подходящего обработчика
        for (HandlerInfo handler : handlers) {
            CommandHandler annotation = handler.annotation;
            
            // Проверка минимального и максимального количества аргументов
            if (args.length < annotation.minArgs()) continue;
            if (annotation.maxArgs() != -1 && args.length > annotation.maxArgs()) continue;
            
            try {
                // Подготавливаем аргументы для вызова метода
                Object[] methodArgs = prepareArguments(sender, args, handler.method);
                
                // Вызываем метод обработчика
                handler.method.invoke(commandInstance, methodArgs);
                return true;
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка выполнения обработчика команды: " + e.getMessage());
                e.printStackTrace();
                sender.sendMessage("§cПроизошла ошибка при выполнении команды!");
                return true;
            }
        }
        
        // Если подходящий обработчик не найден
        if (!handlers.isEmpty()) {
            CommandHandler firstHandler = handlers.get(0).annotation;
            String usage = firstHandler.usage();
            if (!usage.isEmpty()) {
                sender.sendMessage("§cНеправильное использование команды:");
                sender.sendMessage("§7" + usage);
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                     @NotNull String alias, String[] args) {
        // Для примера выводим онлайн-игроков для первого аргумента
        if (args.length == 1) {
            return null; // Стандартное автодополнение имен игроков
        }
        return Collections.emptyList();
    }
    
    /**
     * Подготавливает аргументы для вызова метода обработчика команды.
     */
    private Object[] prepareArguments(CommandSender sender, String[] args, Method method) {
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Object[] result = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            java.lang.reflect.Parameter param = parameters[i];
            
            // Обработка аннотации @Sender
            if (param.isAnnotationPresent(Sender.class)) {
                Class<?> paramType = param.getType();
                if (paramType.isInstance(sender)) {
                    result[i] = sender;
                } else {
                    throw new IllegalArgumentException("Sender type mismatch");
                }
                continue;
            }
            
            // Обработка аннотации @Parameter
            if (param.isAnnotationPresent(Parameter.class)) {
                Parameter annotation = param.getAnnotation(Parameter.class);
                
                // Определяем индекс аргумента
                int index = annotation.index();
                if (index == -1) {
                    // Если индекс не указан, используем порядковый номер параметра
                    // (пропуская параметры с @Sender)
                    index = getParameterIndex(parameters, i);
                }
                
                // Проверяем, есть ли аргумент с таким индексом
                if (index >= args.length) {
                    if (annotation.required()) {
                        throw new IllegalArgumentException("Required parameter missing");
                    } else {
                        result[i] = annotation.defaultValue();
                        continue;
                    }
                }
                
                // Получаем значение аргумента
                String argValue = args[index];
                
                // Если параметр joined, объединяем все оставшиеся аргументы
                if (annotation.joined() && index < args.length - 1) {
                    StringBuilder joinedValue = new StringBuilder(argValue);
                    for (int j = index + 1; j < args.length; j++) {
                        joinedValue.append(" ").append(args[j]);
                    }
                    argValue = joinedValue.toString();
                }
                
                // Конвертируем значение в нужный тип
                result[i] = convertArgument(argValue, param.getType());
                continue;
            }
            
            // Если параметр без аннотаций, используем порядковый номер
            int index = getParameterIndex(parameters, i);
            if (index < args.length) {
                result[i] = convertArgument(args[index], param.getType());
            } else {
                throw new IllegalArgumentException("Missing argument");
            }
        }
        
        return result;
    }
    
    /**
     * Определяет индекс параметра с учетом параметров с @Sender.
     */
    private int getParameterIndex(java.lang.reflect.Parameter[] parameters, int currentIndex) {
        int argIndex = 0;
        for (int i = 0; i < currentIndex; i++) {
            if (!parameters[i].isAnnotationPresent(Sender.class)) {
                argIndex++;
            }
        }
        return argIndex;
    }
    
    /**
     * Конвертирует строковое значение в указанный тип.
     */
    private Object convertArgument(String value, Class<?> targetType) {
        if (targetType == String.class) {
            return value;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == Player.class) {
            Player player = Bukkit.getPlayer(value);
            if (player == null || !player.isOnline()) {
                throw new IllegalArgumentException("Player not found: " + value);
            }
            return player;
        }
        
        throw new IllegalArgumentException("Unsupported parameter type: " + targetType.getName());
    }
    
    /**
     * Класс, представляющий информацию об обработчике команды.
     */
    private static class HandlerInfo {
        private final Method method;
        private final CommandHandler annotation;
        
        public HandlerInfo(Method method, CommandHandler annotation) {
            this.method = method;
            this.annotation = annotation;
        }
    }

    /**
     * Получает имя команды из аннотации.
     */
    public String getName() {
        return commandAnnotation.name();
    }
    
    /**
     * Получает псевдонимы команды из аннотации.
     */
    public String[] getAliases() {
        return commandAnnotation.aliases();
    }
    
    /**
     * Получает описание команды из аннотации.
     */
    public String getDescription() {
        return commandAnnotation.description();
    }
}

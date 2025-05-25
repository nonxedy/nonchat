package com.nonxedy.nonchat.api.registry;

import com.nonxedy.nonchat.api.annotation.Command;
import com.nonxedy.nonchat.api.annotation.CommandHandler;
import com.nonxedy.nonchat.api.annotation.Sender;
import com.nonxedy.nonchat.nonchat;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Реестр команд для автоматической регистрации и выполнения команд на основе аннотаций.
 * Этот класс сканирует и регистрирует все классы с аннотацией @Command и их методы с @CommandHandler.
 */
public class CommandRegistry {
    private final JavaPlugin plugin;
    private final Map<String, CommandInfo> registeredCommands = new HashMap<>();
    
    /**
     * Создает новый реестр команд.
     *
     * @param plugin Экземпляр плагина
     */
    public CommandRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Регистрирует классы команд по указанному пути пакета.
     * 
     * @param packageName Имя пакета для сканирования
     */
    public void registerCommands(String packageName) {
        plugin.getLogger().info("Сканирование команд в пакете: " + packageName);
        
        // В реальном плагине здесь был бы код для сканирования пакетов
        // и поиска классов с аннотацией @Command
        // Для примера мы просто покажем, как бы работал процесс регистрации
        
        // Пример регистрации команды
        // commandInstance - объект команды, полученный через reflection или DI
        // commandClass - класс команды с аннотацией @Command
        
        // Object commandInstance = ...
        // Class<?> commandClass = commandInstance.getClass();
        // Command commandAnnotation = commandClass.getAnnotation(Command.class);
        //
        // registerCommand(commandAnnotation, commandInstance, commandClass);
        
        plugin.getLogger().info("Зарегистрированы команды: " + registeredCommands.keySet());
    }
    
    /**
     * Регистрирует команду на основе аннотаций.
     * 
     * @param commandAnnotation Аннотация команды
     * @param instance Экземпляр класса команды
     * @param clazz Класс команды
     */
    private void registerCommand(Command commandAnnotation, Object instance, Class<?> clazz) {
        String commandName = commandAnnotation.name();
        List<String> aliases = Arrays.asList(commandAnnotation.aliases());
        
        // Находим все методы с аннотацией @CommandHandler
        List<HandlerInfo> handlers = new ArrayList<>();
        
        for (Method method : clazz.getDeclaredMethods()) {
            CommandHandler handlerAnnotation = method.getAnnotation(CommandHandler.class);
            if (handlerAnnotation != null) {
                handlers.add(new HandlerInfo(method, handlerAnnotation));
            }
        }
        
        // Сортируем обработчики по приоритету
        handlers.sort(Comparator.comparingInt(h -> h.annotation.priority()));
        
        // Создаем информацию о команде
        CommandInfo commandInfo = new CommandInfo(
            commandName,
            aliases,
            commandAnnotation.description(),
            commandAnnotation.permission(),
            commandAnnotation.playerOnly(),
            instance,
            handlers
        );
        
        // Сохраняем информацию о команде
        registeredCommands.put(commandName.toLowerCase(), commandInfo);
        
        // Регистрируем команду и ее псевдонимы в Bukkit
        TabExecutor executor = createExecutor(commandInfo);
        plugin.getCommand(commandName).setExecutor(executor);
        plugin.getCommand(commandName).setTabCompleter(executor);
        
        plugin.getLogger().info("Зарегистрирована команда: " + commandName 
            + (aliases.isEmpty() ? "" : " с псевдонимами: " + String.join(", ", aliases)));
    }
    
    /**
     * Создает исполнителя команды.
     * 
     * @param commandInfo Информация о команде
     * @return Исполнитель команды
     */
    private TabExecutor createExecutor(CommandInfo commandInfo) {
        return new TabExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
                // Проверка на Player Only
                if (commandInfo.playerOnly && !(sender instanceof Player)) {
                    sender.sendMessage("§cЭта команда доступна только для игроков!");
                    return true;
                }
                
                // Проверка разрешения
                if (!commandInfo.permission.isEmpty() && !sender.hasPermission(commandInfo.permission)) {
                    sender.sendMessage("§cУ вас нет разрешения на использование этой команды!");
                    return true;
                }
                
                // Поиск подходящего обработчика
                for (HandlerInfo handler : commandInfo.handlers) {
                    CommandHandler annotation = handler.annotation;
                    
                    // Проверка минимального и максимального количества аргументов
                    if (args.length < annotation.minArgs()) continue;
                    if (annotation.maxArgs() != -1 && args.length > annotation.maxArgs()) continue;
                    
                    try {
                        // Подготавливаем аргументы для вызова метода
                        Object[] methodArgs = prepareArguments(sender, args, handler.method);
                        
                        // Вызываем метод обработчика
                        handler.method.invoke(commandInfo.instance, methodArgs);
                        return true;
                    } catch (ReflectiveOperationException e) {
                        plugin.getLogger().severe("Ошибка выполнения обработчика команды: " + e.getMessage());
                        e.printStackTrace();
                        sender.sendMessage("§cПроизошла ошибка при выполнении команды!");
                        return true;
                    } catch (IllegalArgumentException e) {
                        // Несовпадение параметров или ошибка конвертации, продолжаем поиск
                        continue;
                    }
                }
                
                // Если подходящий обработчик не найден, выводим сообщение о неправильном использовании
                if (!commandInfo.handlers.isEmpty()) {
                    CommandHandler firstHandler = commandInfo.handlers.get(0).annotation;
                    String usage = firstHandler.usage();
                    if (!usage.isEmpty()) {
                        sender.sendMessage("§cНеправильное использование команды:");
                        sender.sendMessage("§7" + usage);
                    }
                }
                
                return true;
            }
            
            @Override
            public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
                // Для примера выводим онлайн-игроков для первого аргумента
                if (args.length == 1) {
                    return null; // Вернуть null для стандартного дополнения имен игроков
                }
                return Collections.emptyList();
            }
        };
    }
    
    /**
     * Подготавливает аргументы для вызова метода обработчика команды.
     * 
     * @param sender Отправитель команды
     * @param args Аргументы команды
     * @param method Метод обработчика
     * @return Массив аргументов для вызова метода
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
            if (param.isAnnotationPresent(com.nonxedy.nonchat.api.annotation.Parameter.class)) {
                com.nonxedy.nonchat.api.annotation.Parameter annotation = 
                    param.getAnnotation(com.nonxedy.nonchat.api.annotation.Parameter.class);
                
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
     * 
     * @param parameters Список параметров метода
     * @param currentIndex Текущий индекс параметра
     * @return Индекс аргумента команды для данного параметра
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
     * 
     * @param value Строковое значение
     * @param targetType Целевой тип
     * @return Сконвертированное значение
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
                throw new IllegalArgumentException("Player not found");
            }
            return player;
        }
        
        throw new IllegalArgumentException("Unsupported parameter type: " + targetType.getName());
    }
    
    /**
     * Класс, представляющий информацию о команде.
     */
    private static class CommandInfo {
        private final String name;
        private final List<String> aliases;
        private final String description;
        private final String permission;
        private final boolean playerOnly;
        private final Object instance;
        private final List<HandlerInfo> handlers;
        
        public CommandInfo(String name, List<String> aliases, String description, 
                          String permission, boolean playerOnly, Object instance, 
                          List<HandlerInfo> handlers) {
            this.name = name;
            this.aliases = aliases;
            this.description = description;
            this.permission = permission;
            this.playerOnly = playerOnly;
            this.instance = instance;
            this.handlers = handlers;
        }
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
}

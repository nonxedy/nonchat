package com.nonxedy.nonchat.api.annotation;

import java.lang.annotation.*;

/**
 * Аннотация для методов обработчиков команд.
 * Позволяет определить метод, который будет вызываться при выполнении команды.
 * 
 * Пример использования:
 * 
 * <pre>
 * {@code 
 * @Command(name = "message", aliases = {"msg", "m"})
 * public class MessageCommand {
 * 
 *     @CommandHandler(minArgs = 2, usage = "/msg <player> <message>")
 *     public void onMessage(
 *         @Sender Player sender, 
 *         @Parameter(name = "player") Player target, 
 *         @Parameter(name = "message", joined = true) String message
 *     ) {
 *         // Implementation
 *     }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandHandler {
    /**
     * Минимальное количество аргументов
     */
    int minArgs() default 0;
    
    /**
     * Максимальное количество аргументов (-1 = без ограничений)
     */
    int maxArgs() default -1;
    
    /**
     * Инструкция по использованию команды
     */
    String usage() default "";
    
    /**
     * Приоритет обработчика (если несколько обработчиков подходят по условиям)
     */
    int priority() default 0;
}

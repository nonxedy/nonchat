package com.nonxedy.nonchat.api.annotation;

import java.lang.annotation.*;

/**
 * Аннотация для классов команд.
 * 
 * Пример использования:
 * 
 * <pre>
 * {@code 
 * @Command(
 *     name = "message", 
 *     aliases = {"msg", "m", "tell"}, 
 *     description = "Send a private message",
 *     permission = "nonchat.message",
 *     playerOnly = true
 * )
 * public class MessageCommand {
 *     // Command implementation
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {
    /**
     * Имя команды
     */
    String name();
    
    /**
     * Альтернативные названия команды
     */
    String[] aliases() default {};
    
    /**
     * Описание команды для help
     */
    String description() default "";
    
    /**
     * Требуемое разрешение
     */
    String permission() default "";
    
    /**
     * Ограничение команды только для игроков
     */
    boolean playerOnly() default false;
}

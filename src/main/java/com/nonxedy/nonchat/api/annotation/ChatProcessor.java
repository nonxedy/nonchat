package com.nonxedy.nonchat.api.annotation;

import java.lang.annotation.*;

/**
 * Аннотация для процессоров сообщений чата.
 * Классы с этой аннотацией будут обрабатывать сообщения в указанных каналах.
 * 
 * Пример использования:
 * 
 * <pre>
 * {@code 
 * @ChatProcessor(
 *     channels = {"global", "staff"}, 
 *     priority = 100,
 *     permission = "nonchat.format"
 * )
 * public class ColorCodeProcessor implements MessageProcessor {
 *     @Override
 *     public String process(Player sender, String message) {
 *         // Обработка сообщения, например замена цветовых кодов
 *         return message.replace("&", "§");
 *     }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ChatProcessor {
    /**
     * Каналы, к которым применяется процессор.
     * Пустой массив означает применение ко всем каналам.
     */
    String[] channels() default {};
    
    /**
     * Приоритет процессора. Чем ниже значение, тем раньше выполняется.
     */
    int priority() default 0;
    
    /**
     * Разрешение, необходимое для применения процессора.
     * Пустая строка означает, что разрешение не требуется.
     */
    String permission() default "";
    
    /**
     * Если true, процессор будет работать только если отправитель имеет указанное разрешение.
     * Если false, процессор будет работать для всех, но функциональность может отличаться
     * в зависимости от наличия разрешения.
     */
    boolean permissionRequired() default false;
}

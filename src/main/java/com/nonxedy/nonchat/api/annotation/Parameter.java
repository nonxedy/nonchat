package com.nonxedy.nonchat.api.annotation;

import java.lang.annotation.*;

/**
 * Аннотация для параметров команды.
 * Определяет, как аргументы команды должны быть преобразованы в параметры метода.
 * 
 * Пример использования:
 * 
 * <pre>
 * {@code 
 * @CommandHandler(minArgs = 2, usage = "/msg <player> <message>")
 * public void onMessage(
 *     @Sender Player sender, 
 *     @Parameter(name = "player") Player target, 
 *     @Parameter(name = "message", joined = true) String message
 * ) {
 *     // Implementation
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Parameter {
    /**
     * Имя параметра, используется в сообщениях и для поиска
     */
    String name();
    
    /**
     * Является ли параметр обязательным
     */
    boolean required() default true;
    
    /**
     * Если true, все оставшиеся аргументы будут объединены в одну строку
     * и переданны в этот параметр. Полезно для сообщений.
     * 
     * Например: /msg player Hello, this is a message
     * В этом случае "Hello, this is a message" будет передано как один параметр.
     */
    boolean joined() default false;
    
    /**
     * Индекс аргумента в команде (0-based).
     * Если не указан, будет использован порядок параметров метода.
     */
    int index() default -1;
    
    /**
     * Значение по умолчанию (для String параметров)
     */
    String defaultValue() default "";
}

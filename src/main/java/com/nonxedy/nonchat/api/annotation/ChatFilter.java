package com.nonxedy.nonchat.api.annotation;

import java.lang.annotation.*;

/**
 * Аннотация для фильтров сообщений чата.
 * Классы с этой аннотацией будут использоваться для фильтрации сообщений в указанных каналах.
 * 
 * Пример использования:
 * 
 * <pre>
 * {@code 
 * @ChatFilter(
 *     channels = {"global", "local"},
 *     priority = 50,
 *     bypassPermission = "nonchat.bypass.profanity"
 * )
 * public class ProfanityFilter implements MessageFilter {
 *     @Override
 *     public boolean shouldFilter(Player sender, String message) {
 *         // Проверка сообщения на наличие запрещенных слов
 *         return containsProfanity(message);
 *     }
 *     
 *     @Override
 *     public String getReason() {
 *         return "Message contains inappropriate language";
 *     }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ChatFilter {
    /**
     * Каналы, к которым применяется фильтр.
     * Пустой массив означает применение ко всем каналам.
     */
    String[] channels() default {};
    
    /**
     * Приоритет фильтра. Чем ниже значение, тем раньше выполняется.
     */
    int priority() default 0;
    
    /**
     * Если true, игрок не будет получать уведомление о блокировке сообщения.
     */
    boolean silent() default false;
    
    /**
     * Разрешение для обхода фильтра.
     * Игрок с этим разрешением не будет подвергаться фильтрации.
     */
    String bypassPermission() default "";
    
    /**
     * Если true, сообщение будет заменено на кастомное, 
     * а не полностью заблокировано
     */
    boolean replace() default false;
    
    /**
     * Если replace=true, указывает текст для замены отфильтрованного сообщения.
     * Если не указан, используется значение по умолчанию из конфигурации.
     */
    String replacementText() default "";
}

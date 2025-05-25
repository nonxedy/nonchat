package com.nonxedy.nonchat.api.annotation;

import java.lang.annotation.*;

/**
 * Аннотация, помечающая параметр метода как отправителя команды.
 * Этот параметр будет автоматически заполнен CommandSender'ом, который вызвал команду.
 * 
 * Пример использования:
 * 
 * <pre>
 * {@code 
 * @CommandHandler
 * public void execute(@Sender Player sender) {
 *     // Implementation, sender будет автоматически заполнен
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Sender {
    // Аннотация-маркер без атрибутов
}

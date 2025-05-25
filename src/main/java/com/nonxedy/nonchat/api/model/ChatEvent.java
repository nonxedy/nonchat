package com.nonxedy.nonchat.api.model;

import org.bukkit.entity.Player;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Рекорд представляющий событие чата.
 * Иммутабельный объект, содержащий информацию о сообщении, отправителе и состоянии.
 */
public record ChatEvent(
    // Игрок, отправивший сообщение
    Player sender,
    
    // Оригинальное сообщение без изменений
    String originalMessage,
    
    // Обработанное сообщение (после применения форматирования, цензуры и т.д.)
    String processedMessage,
    
    // Идентификатор канала, в который отправлено сообщение
    String channelId,
    
    // Флаг, указывающий, было ли сообщение отменено
    boolean cancelled,
    
    // Время создания события
    Instant timestamp,
    
    // Список получателей сообщения (может быть пустым)
    List<Player> recipients,
    
    // Список извлеченных упоминаний в сообщении
    List<Mention> mentions
) {
    /**
     * Компактный конструктор для установки значений по умолчанию.
     */
    public ChatEvent {
        if (processedMessage == null) {
            processedMessage = originalMessage;
        }
        
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        
        if (recipients == null) {
            recipients = Collections.emptyList();
        }
        
        if (mentions == null) {
            mentions = Collections.emptyList();
        }
    }
    
    /**
     * Базовый конструктор для создания события чата.
     * 
     * @param sender Игрок, отправивший сообщение
     * @param originalMessage Оригинальное сообщение
     * @param channelId Идентификатор канала
     */
    public ChatEvent(Player sender, String originalMessage, String channelId) {
        this(sender, originalMessage, originalMessage, channelId, false, Instant.now(), 
             Collections.emptyList(), Collections.emptyList());
    }
    
    /**
     * Создает новую версию события с измененным обработанным сообщением.
     * 
     * @param newMessage Новое обработанное сообщение
     * @return Новый объект ChatEvent с обновленным сообщением
     */
    public ChatEvent withProcessedMessage(String newMessage) {
        return new ChatEvent(sender, originalMessage, newMessage, channelId, cancelled, 
                            timestamp, recipients, mentions);
    }
    
    /**
     * Создает новую версию события, отмеченную как отмененную.
     * 
     * @return Новый объект ChatEvent с флагом cancelled = true
     */
    public ChatEvent cancel() {
        return new ChatEvent(sender, originalMessage, processedMessage, channelId, true, 
                            timestamp, recipients, mentions);
    }
    
    /**
     * Создает новую версию события с обновленным списком получателей.
     * 
     * @param newRecipients Новый список получателей
     * @return Новый объект ChatEvent с обновленными получателями
     */
    public ChatEvent withRecipients(List<Player> newRecipients) {
        return new ChatEvent(sender, originalMessage, processedMessage, channelId, cancelled, 
                            timestamp, newRecipients, mentions);
    }
    
    /**
     * Создает новую версию события с обновленным списком упоминаний.
     * 
     * @param newMentions Новый список упоминаний
     * @return Новый объект ChatEvent с обновленными упоминаниями
     */
    public ChatEvent withMentions(List<Mention> newMentions) {
        return new ChatEvent(sender, originalMessage, processedMessage, channelId, cancelled, 
                            timestamp, recipients, newMentions);
    }
    
    /**
     * Проверяет, содержит ли сообщение упоминания.
     * 
     * @return true, если есть упоминания, иначе false
     */
    public boolean hasMentions() {
        return !mentions.isEmpty();
    }
    
    /**
     * Получает имя отправителя сообщения.
     * 
     * @return Имя отправителя
     */
    public String getSenderName() {
        return sender.getName();
    }
    
    /**
     * Получает UUID отправителя сообщения.
     * 
     * @return UUID отправителя
     */
    public UUID getSenderId() {
        return sender.getUniqueId();
    }
    
    /**
     * Статический метод-фабрика для создания базового события чата.
     * 
     * @param sender Игрок, отправивший сообщение
     * @param message Сообщение
     * @param channelId Идентификатор канала
     * @return Новый объект ChatEvent
     */
    public static ChatEvent create(Player sender, String message, String channelId) {
        return new ChatEvent(sender, message, channelId);
    }
}

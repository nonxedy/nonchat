package com.nonxedy.nonchat.api.model;

import org.bukkit.entity.Player;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Рекорд представляющий упоминание в чате (@player, @group, @all и т.д.).
 * Иммутабельный объект, содержащий информацию о типе упоминания, целях и т.д.
 */
public record Mention(
    // Оригинальный текст упоминания (как написал пользователь)
    String originalText,
    
    // Имя цели упоминания (без @ и других символов)
    String targetName,
    
    // Тип упоминания (игрок, группа, все и т.д.)
    MentionType type,
    
    // Список игроков, которые упомянуты
    List<Player> targetPlayers,
    
    // Форматированный текст упоминания (с цветами и т.д.)
    String formattedText,
    
    // Разрешено ли упоминание (может быть отклонено фильтрами)
    boolean allowed
) {
    /**
     * Компактный конструктор для установки значений по умолчанию.
     */
    public Mention {
        if (targetPlayers == null) {
            targetPlayers = Collections.emptyList();
        }
        
        if (formattedText == null) {
            formattedText = originalText;
        }
    }
    
    /**
     * Базовый конструктор для создания упоминания игрока.
     * 
     * @param originalText Оригинальный текст упоминания
     * @param player Упомянутый игрок
     */
    public Mention(String originalText, Player player) {
        this(originalText, player.getName(), MentionType.PLAYER, 
             Collections.singletonList(player), null, true);
    }
    
    /**
     * Создает упоминание группы игроков.
     * 
     * @param originalText Оригинальный текст упоминания
     * @param groupName Название группы
     * @param players Список игроков в группе
     * @return Новый объект Mention для группы
     */
    public static Mention group(String originalText, String groupName, List<Player> players) {
        return new Mention(originalText, groupName, MentionType.GROUP, players, null, true);
    }
    
    /**
     * Создает упоминание всех игроков.
     * 
     * @param originalText Оригинальный текст упоминания (обычно "@all" или "@everyone")
     * @param players Список всех игроков онлайн
     * @return Новый объект Mention для всех
     */
    public static Mention all(String originalText, List<Player> players) {
        return new Mention(originalText, "all", MentionType.ALL, players, null, true);
    }
    
    /**
     * Создает новую версию упоминания с указанным форматированным текстом.
     * 
     * @param newFormattedText Новый форматированный текст
     * @return Новый объект Mention с обновленным форматированным текстом
     */
    public Mention withFormattedText(String newFormattedText) {
        return new Mention(originalText, targetName, type, targetPlayers, newFormattedText, allowed);
    }
    
    /**
     * Создает новую версию упоминания, отмеченную как неразрешенную.
     * 
     * @return Новый объект Mention с флагом allowed = false
     */
    public Mention deny() {
        return new Mention(originalText, targetName, type, targetPlayers, formattedText, false);
    }
    
    /**
     * Проверяет, упоминается ли конкретный игрок.
     * 
     * @param player Игрок для проверки
     * @return true, если игрок упоминается, иначе false
     */
    public boolean mentions(Player player) {
        return targetPlayers.contains(player);
    }
    
    /**
     * Проверяет, упоминается ли игрок с указанным именем.
     * 
     * @param playerName Имя игрока для проверки
     * @return true, если игрок упоминается, иначе false
     */
    public boolean mentionsByName(String playerName) {
        return targetPlayers.stream()
            .map(Player::getName)
            .anyMatch(name -> name.equalsIgnoreCase(playerName));
    }
    
    /**
     * Типы упоминаний.
     */
    public enum MentionType {
        /** Упоминание конкретного игрока (@player) */
        PLAYER,
        
        /** Упоминание группы игроков (@group, @admin, @mod и т.д.) */
        GROUP,
        
        /** Упоминание всех игроков (@all, @everyone) */
        ALL,
        
        /** Другие типы упоминаний */
        OTHER
    }
}

package com.nonxedy.nonchat.util.chat.packets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import com.nonxedy.nonchat.util.core.colors.ColorUtil;

import net.kyori.adventure.text.Component;

/**
 * Manages chat bubble display using armor stands with object pooling
 * Handles creation, removal and updating of floating text displays
 */
public class BubblePacketUtil {
    private static final int MAX_LINE_LENGTH = 40;
    private static final double LINE_SPACING = 0.25;
    private static final ObjectPool<ArmorStand> armorStandPool = new ObjectPool<>(50);
    
    /**
     * Creates and configures multiple chat bubble armor stands for multiline text using pooled armor stands
     * @param player The player to create bubbles for
     * @param text The text to display in bubbles
     * @param location The base location to spawn bubbles at
     * @return List of configured armor stand entities
     */
    public static List<ArmorStand> spawnMultilineBubble(Player player, String text, Location location) {
        List<String> lines = splitTextIntoLines(text, MAX_LINE_LENGTH);
        List<ArmorStand> bubbleStands = new ArrayList<>();
        
        for (int i = 0; i < lines.size(); i++) {
            Location lineLocation = location.clone().add(0, (lines.size() - 1 - i) * LINE_SPACING, 0);
            
            ArmorStand bubble = armorStandPool.acquire(() -> {
                ArmorStand as = (ArmorStand) lineLocation.getWorld().spawnEntity(lineLocation, EntityType.ARMOR_STAND);
                configureArmorStand(as);
                return as;
            });
            
            bubble.teleport(lineLocation);
            
            Component component = ColorUtil.parseComponent(lines.get(i));
            bubble.customName(component);
            
            bubbleStands.add(bubble);
        }
        
        return bubbleStands;
    }
    
    private static void configureArmorStand(ArmorStand as) {
        as.setCustomNameVisible(true);
        as.setInvisible(true);
        as.setGravity(false);
        as.setMarker(true);
        as.setSmall(true);
        // Clear any equipment that might have been set
        as.getEquipment().clear();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            as.setDisabledSlots(slot);
        }
    }
    
    public static ArmorStand spawnBubble(Player player, String text, Location location) {
        ArmorStand bubble = armorStandPool.acquire(() -> {
            ArmorStand as = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
            configureArmorStand(as);
            return as;
        });
        
        Component component = ColorUtil.parseComponent(text);
        bubble.customName(component);
        bubble.teleport(location);
        
        return bubble;
    }
    
    public static void removeBubble(ArmorStand bubble) {
        if (bubble != null && !bubble.isDead()) {
            bubble.remove(); // Принудительно удаляем из мира
            armorStandPool.release(bubble);
        }
    }
    
    public static void removeBubbles(List<ArmorStand> bubbles) {
        if (bubbles != null) {
            bubbles.forEach(BubblePacketUtil::removeBubble);
            bubbles.clear(); // Очищаем список после удаления
        }
    }

    /**
     * Updates the position of a single chat bubble
     * @param bubble The armor stand to move
     * @param location New location for the bubble
     */
    public static void updateBubbleLocation(ArmorStand bubble, Location location) {
        if (bubble != null && !bubble.isDead()) {
            bubble.teleport(location);
        }
    }
    
    /**
     * Updates the positions of multiple chat bubbles
     * @param bubbles List of armor stands to move
     * @param baseLocation Base location for the bubbles
     */
    public static void updateBubblesLocation(List<ArmorStand> bubbles, Location baseLocation) {
        if (bubbles != null) {
            // Удаляем мертвые armor stands из списка
            bubbles.removeIf(bubble -> bubble == null || bubble.isDead());
            
            for (int i = 0; i < bubbles.size(); i++) {
                ArmorStand bubble = bubbles.get(i);
                if (bubble != null && !bubble.isDead()) {
                    Location lineLocation = baseLocation.clone().add(0, (bubbles.size() - 1 - i) * LINE_SPACING, 0);
                    bubble.teleport(lineLocation);
                }
            }
        }
    }

    private static List<String> splitTextIntoLines(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        Pattern colorPattern = Pattern.compile("§[0-9a-fk-or]|&#[0-9a-fA-F]{6}");
        String currentColor = "";
        String processedText = ColorUtil.parseColor(text);
        String[] words = processedText.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String wordWithoutColors = word.replaceAll("§[0-9a-fk-or]|&#[0-9a-fA-F]{6}", "");
            int visibleLength = wordWithoutColors.length();
            String lineWithoutColors = currentLine.toString().replaceAll("§[0-9a-fk-or]|&#[0-9a-fA-F]{6}", "");
            int currentVisibleLength = lineWithoutColors.length();

            if (currentVisibleLength + visibleLength + (currentVisibleLength > 0 ? 1 : 0) <= maxLength) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    Matcher matcher = colorPattern.matcher(currentLine.toString());
                    currentColor = "";
                    while (matcher.find()) {
                        currentColor = matcher.group();
                    }
                    currentLine = new StringBuilder(currentColor);
                }

                if (visibleLength > maxLength) {
                    StringBuilder remainingWord = new StringBuilder(word);
                    while (remainingWord.length() > 0) {
                        StringBuilder colorCodes = new StringBuilder();
                        int i = 0;
                        while (i < remainingWord.length()) {
                            if (remainingWord.charAt(i) == '§' && i + 1 < remainingWord.length()) {
                                colorCodes.append(remainingWord.charAt(i)).append(remainingWord.charAt(i + 1));
                                i += 2;
                            } else if (i + 2 < remainingWord.length() && 
                                      remainingWord.charAt(i) == '&' && 
                                      remainingWord.charAt(i+1) == '#') {
                                int endIndex = Math.min(i + 8, remainingWord.length());
                                colorCodes.append(remainingWord.substring(i, endIndex));
                                i = endIndex;
                            } else {
                                break;
                            }
                        }

                        if (i > 0) {
                            remainingWord.delete(0, i);
                        }

                        String visiblePart = remainingWord.toString().replaceAll("§[0-9a-fk-or]|&#[0-9a-fA-F]{6}", "");
                        int charsToTake = Math.min(visiblePart.length(), maxLength);

                        StringBuilder linePart = new StringBuilder(currentColor).append(colorCodes);
                        int charsTaken = 0;
                        i = 0;

                        while (i < remainingWord.length() && charsTaken < charsToTake) {
                            if (remainingWord.charAt(i) == '§' && i + 1 < remainingWord.length()) {
                                linePart.append(remainingWord.charAt(i)).append(remainingWord.charAt(i + 1));
                                i += 2;
                            } else if (i + 2 < remainingWord.length() && 
                                      remainingWord.charAt(i) == '&' && 
                                      remainingWord.charAt(i+1) == '#') {
                                int endIndex = Math.min(i + 8, remainingWord.length());
                                linePart.append(remainingWord.substring(i, endIndex));
                                i = endIndex;
                            } else {
                                linePart.append(remainingWord.charAt(i));
                                i++;
                                charsTaken++;
                            }
                        }

                        lines.add(linePart.toString());
                        Matcher matcher = colorPattern.matcher(linePart.toString());
                        while (matcher.find()) {
                            currentColor = matcher.group();
                        }

                        remainingWord.delete(0, i);

                        if (remainingWord.length() > 0) {
                            remainingWord.insert(0, currentColor);
                        }
                    }

                    currentLine = new StringBuilder();
                } else {
                    currentLine.append(word);
                }
            }
        }
    
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        if (lines.isEmpty()) {
            lines.add(text);
        }

        return lines;
    }
    
    /**
     * Simple object pool implementation for ArmorStand entities
     */
    private static class ObjectPool<T> {
        private final List<T> pool = new ArrayList<>();
        private final int maxSize;
        
        public ObjectPool(int maxSize) {
            this.maxSize = maxSize;
        }
        
        public T acquire(Supplier<T> creator) {
            synchronized (pool) {
                if (!pool.isEmpty()) {
                    T obj = pool.remove(pool.size() - 1);
                    // Проверяем, что объект все еще валиден
                    if (obj instanceof ArmorStand) {
                        ArmorStand as = (ArmorStand) obj;
                        if (as.isDead()) {
                            // Если объект мертв, создаем новый
                            return creator.get();
                        }
                    }
                    return obj;
                }
            }
            return creator.get();
        }
        
        public void release(T obj) {
            if (obj instanceof ArmorStand) {
                ArmorStand as = (ArmorStand) obj;
                // Не добавляем в пул мертвые объекты
                if (as.isDead()) {
                    return;
                }
                
                // Сбрасываем состояние armor stand перед возвратом в пул
                as.setCustomNameVisible(false);
                as.customName(null);
                
                synchronized (pool) {
                    if (pool.size() < maxSize) {
                        pool.add(obj);
                    } else {
                        // Если пул полон, удаляем объект
                        as.remove();
                    }
                }
            }
        }
        
        /**
         * Очищает пул и удаляет все объекты
         */
        public void clear() {
            synchronized (pool) {
                pool.forEach(obj -> {
                    if (obj instanceof ArmorStand) {
                        ((ArmorStand) obj).remove();
                    }
                });
                pool.clear();
            }
        }
    }
    
    /**
     * Очищает пул armor stands (вызывается при отключении плагина)
     */
    public static void clearPool() {
        armorStandPool.clear();
    }
}

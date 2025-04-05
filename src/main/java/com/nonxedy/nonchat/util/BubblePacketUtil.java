package com.nonxedy.nonchat.util;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages chat bubble display using armor stands
 * Handles creation, removal and updating of floating text displays
 */
public class BubblePacketUtil {
    
    // Максимальная длина строки в символах
    private static final int MAX_LINE_LENGTH = 40;
    // Вертикальное расстояние между строками
    private static final double LINE_SPACING = 0.25;
    
    /**
     * Creates and configures multiple chat bubble armor stands for multiline text
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
            
            ArmorStand bubble = (ArmorStand) lineLocation.getWorld().spawnEntity(lineLocation, EntityType.ARMOR_STAND);
            
            Component component = ColorUtil.parseComponent(lines.get(i));
            
            bubble.customName(component);
            bubble.setCustomNameVisible(true);
            bubble.setInvisible(true);
            bubble.setGravity(false);
            bubble.setMarker(true);
            bubble.setSmall(true);
            
            bubbleStands.add(bubble);
        }
        
        return bubbleStands;
    }
    
    /**
     * Legacy method for single-line bubbles (for backward compatibility)
     */
    public static ArmorStand spawnBubble(Player player, String text, Location location) {
        ArmorStand bubble = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        
        Component component = ColorUtil.parseComponent(text);
        
        bubble.customName(component);
        bubble.setCustomNameVisible(true);
        bubble.setInvisible(true);
        bubble.setGravity(false);
        bubble.setMarker(true);
        bubble.setSmall(true);
        
        return bubble;
    }
    
    /**
     * Splits text into lines of specified maximum length
     * @param text Text to split
     * @param maxLength Maximum length of each line
     * @return List of text lines
     */
    private static List<String> splitTextIntoLines(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        
        String currentColor = "";
        Pattern colorPattern = Pattern.compile("§[0-9a-fk-or]|#[0-9a-fA-F]{6}");
        
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxLength) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    
                    Matcher matcher = colorPattern.matcher(currentLine.toString());
                    while (matcher.find()) {
                        currentColor = matcher.group();
                    }
                    
                    currentLine = new StringBuilder(currentColor);
                }
                
                if (word.length() > maxLength) {
                    int startIndex = 0;
                    while (startIndex < word.length()) {
                        int endIndex = Math.min(startIndex + maxLength, word.length());
                        String part = word.substring(startIndex, endIndex);
                        lines.add(currentColor + part);
                        startIndex = endIndex;
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
     * Removes a single chat bubble from the world
     * @param bubble The armor stand to remove
     */
    public static void removeBubble(ArmorStand bubble) {
        if (bubble != null && !bubble.isDead()) {
            bubble.remove();
        }
    }
    
    /**
     * Removes multiple chat bubbles from the world
     * @param bubbles List of armor stands to remove
     */
    public static void removeBubbles(List<ArmorStand> bubbles) {
        if (bubbles != null) {
            for (ArmorStand bubble : bubbles) {
                removeBubble(bubble);
            }
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
            for (int i = 0; i < bubbles.size(); i++) {
                ArmorStand bubble = bubbles.get(i);
                if (bubble != null && !bubble.isDead()) {
                    Location lineLocation = baseLocation.clone().add(0, (bubbles.size() - 1 - i) * LINE_SPACING, 0);
                    bubble.teleport(lineLocation);
                }
            }
        }
    }
}

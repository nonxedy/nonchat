package com.nonxedy.nonchat.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

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

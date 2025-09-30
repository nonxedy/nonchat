package com.nonxedy.nonchat.util.chat.packets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import com.nonxedy.nonchat.util.core.colors.ColorUtil;

import net.kyori.adventure.text.Component;

/**
 * Manages chat bubble display using TextDisplay entities for better performance
 * Handles creation, removal and updating of floating text displays
 */
public class DisplayEntityUtil {
    private static final int MAX_LINE_LENGTH = 40;
    private static final double LINE_SPACING = 0.25;
    private static final ObjectPool<TextDisplay> displayPool = new ObjectPool<>(50);
    
    /**
     * Creates and configures multiple chat bubble TextDisplay entities for multiline text using pooled entities
     * @param player The player to create bubbles for
     * @param text The text to display in bubbles
     * @param location The base location to spawn bubbles at
     * @param scale The scale multiplier for the bubbles
     * @return List of configured TextDisplay entities
     */
    public static List<TextDisplay> spawnMultilineBubble(Player player, String text, Location location, double scale) {
        return spawnMultilineBubble(player, text, location, scale, scale, scale, scale);
    }

    /**
     * Creates and configures multiple chat bubble TextDisplay entities for multiline text using pooled entities with individual axis scales
     * @param player The player to create bubbles for
     * @param text The text to display in bubbles
     * @param location The base location to spawn bubbles at
     * @param overallScale The overall scale multiplier
     * @param scaleX The X axis scale multiplier
     * @param scaleY The Y axis scale multiplier
     * @param scaleZ The Z axis scale multiplier
     * @return List of configured TextDisplay entities
     */
    public static List<TextDisplay> spawnMultilineBubble(Player player, String text, Location location, double overallScale, double scaleX, double scaleY, double scaleZ) {
        List<String> lines = splitTextIntoLines(text, MAX_LINE_LENGTH);
        List<TextDisplay> bubbleDisplays = new ArrayList<>();
        
        for (int i = 0; i < lines.size(); i++) {
            Location lineLocation = location.clone().add(0, (lines.size() - 1 - i) * LINE_SPACING, 0);
            
            TextDisplay bubble = displayPool.acquire(() -> {
                try {
                    // Use the player's world to spawn the entity to ensure proper region handling
                    World world = player.getWorld();
                    TextDisplay display = (TextDisplay) world.spawnEntity(lineLocation, EntityType.TEXT_DISPLAY);
                    configureTextDisplay(display, overallScale, scaleX, scaleY, scaleZ);
                    return display;
                } catch (Exception e) {
                    // Log error and return null if entity creation fails
                    Bukkit.getLogger().log(Level.WARNING, "[nonchat] Failed to spawn text display: {0}", e.getMessage());
                    return null;
                }
            });
            
            // Skip if bubble creation failed
            if (bubble == null) {
                continue;
            }
            
            try {
                bubble.teleport(lineLocation);
                
                // Use parseComponent to handle all color formats (legacy, hex, minimessage)
                // Don't strip colors in splitTextIntoLines, let parseComponent handle it
                Component component = ColorUtil.parseComponent(lines.get(i));
                bubble.text(component);
                
                bubbleDisplays.add(bubble);
            } catch (Exception e) {
                // Remove the bubble if configuration fails
                if (bubble != null && !bubble.isDead()) {
                    bubble.remove();
                }
                Bukkit.getLogger().log(Level.WARNING, "[nonchat] Failed to configure text display: {0}", e.getMessage());
            }
        }
        
        return bubbleDisplays;
    }
    
    /**
     * Configures a TextDisplay entity with chat bubble properties
     * @param display The TextDisplay to configure
     * @param scale The scale multiplier
     */
    private static void configureTextDisplay(TextDisplay display, double scale) {
        configureTextDisplay(display, scale, scale, scale, scale);
    }

    /**
     * Configures a TextDisplay entity with chat bubble properties using individual axis scales
     * @param display The TextDisplay to configure
     * @param overallScale The overall scale multiplier (for backward compatibility)
     * @param scaleX The X axis scale multiplier
     * @param scaleY The Y axis scale multiplier
     * @param scaleZ The Z axis scale multiplier
     */
    private static void configureTextDisplay(TextDisplay display, double overallScale, double scaleX, double scaleY, double scaleZ) {
        try {
            // Set display properties
            display.setCustomNameVisible(false);
            display.setGravity(false);
            display.setInvulnerable(true);
            display.setSilent(true);
            
            // Set scale using proper transformation with individual axis control
            Vector3f scaleVector = new Vector3f(
                (float) (overallScale * scaleX), 
                (float) (overallScale * scaleY), 
                (float) (overallScale * scaleZ)
            );
            Transformation transformation = display.getTransformation();
            transformation.getScale().set(scaleVector.x, scaleVector.y, scaleVector.z);
            display.setTransformation(transformation);
            
            // Set background color (semi-transparent black) - using Color from Bukkit
            display.setBackgroundColor(Color.BLACK);
            
            // Set text alignment to center
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            
            // Set line width
            display.setLineWidth(200);
            
            // Set shadow
            display.setShadowed(true);
            
            // Set see-through
            display.setSeeThrough(true);
            
            // Set default opacity
            display.setDefaultBackground(false);
            
            // Set billboard mode to always face player
            display.setBillboard(Display.Billboard.CENTER);
            
            // Set view range
            display.setViewRange(48);
            
            // Set interpolation duration
            display.setInterpolationDuration(0);
            
            // Set teleport duration
            display.setTeleportDuration(0);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "[nonchat] Error configuring text display: {0}", e.getMessage());
        }
    }
    
    /**
     * Creates a single chat bubble TextDisplay entity
     * @param player The player to create bubble for
     * @param text The text to display
     * @param location The location to spawn bubble at
     * @param scale The scale multiplier
     * @return Configured TextDisplay entity or null if failed
     */
    public static TextDisplay spawnBubble(Player player, String text, Location location, double scale) {
        TextDisplay bubble = displayPool.acquire(() -> {
            try {
                // Use the player's world to spawn the entity to ensure proper region handling
                World world = player.getWorld();
                TextDisplay display = (TextDisplay) world.spawnEntity(location, EntityType.TEXT_DISPLAY);
                configureTextDisplay(display, scale);
                return display;
            } catch (Exception e) {
                // Log error and return null if entity creation fails
                Bukkit.getLogger().log(Level.WARNING, "[nonchat] Failed to spawn text display: {0}", e.getMessage());
                return null;
            }
        });
        
        // Return null if bubble creation failed
        if (bubble == null) {
            return null;
        }
        
        try {
            // Use parseComponent to handle all color formats (legacy, hex, minimessage)
            // Don't pre-process the text, let parseComponent handle it directly
            Component component = ColorUtil.parseComponent(text);
            bubble.text(component);
            bubble.teleport(location);
        } catch (Exception e) {
            // Remove the bubble if configuration fails
            if (bubble != null && !bubble.isDead()) {
                bubble.remove();
            }
            Bukkit.getLogger().log(Level.WARNING, "[nonchat] Failed to configure text display: {0}", e.getMessage());
            return null;
        }
        
        return bubble;
    }
    
    /**
     * Removes a single chat bubble TextDisplay entity
     * @param bubble The TextDisplay to remove
     */
    public static void removeBubble(TextDisplay bubble) {
        try {
            if (bubble != null && !bubble.isDead()) {
                bubble.remove();
                displayPool.release(bubble);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "[nonchat] Error removing text display: {0}", e.getMessage());
        }
    }
    
    /**
     * Removes multiple chat bubble TextDisplay entities
     * @param bubbles List of TextDisplay entities to remove
     */
    public static void removeBubbles(List<TextDisplay> bubbles) {
        try {
            if (bubbles != null) {
                bubbles.forEach(DisplayEntityUtil::removeBubble);
                bubbles.clear();
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "[nonchat] Error removing text displays: {0}", e.getMessage());
        }
    }

    /**
     * Updates the position of a single chat bubble TextDisplay
     * @param bubble The TextDisplay to move
     * @param location New location for the bubble
     */
    public static void updateBubbleLocation(TextDisplay bubble, Location location) {
        try {
            if (bubble != null && !bubble.isDead()) {
                bubble.teleport(location);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "[nonchat] Error updating text display location: {0}", e.getMessage());
        }
    }
    
    /**
     * Updates the positions of multiple chat bubble TextDisplay entities
     * @param bubbles List of TextDisplay entities to move
     * @param baseLocation Base location for the bubbles
     */
    public static void updateBubblesLocation(List<TextDisplay> bubbles, Location baseLocation) {
        try {
            if (bubbles != null) {
                bubbles.removeIf(bubble -> bubble == null || bubble.isDead());
                
                for (int i = 0; i < bubbles.size(); i++) {
                    try {
                        TextDisplay bubble = bubbles.get(i);
                        if (bubble != null && !bubble.isDead()) {
                            Location lineLocation = baseLocation.clone().add(0, (bubbles.size() - 1 - i) * LINE_SPACING, 0);
                            bubble.teleport(lineLocation);
                        }
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.FINE, "[nonchat] Error updating text display {0} location: {1}", new Object[]{i, e.getMessage()});
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "[nonchat] Error updating text displays location: {0}", e.getMessage());
        }
    }

    /**
     * Splits text into lines with proper length handling and color preservation
     * @param text The text to split
     * @param maxLength Maximum length per line
     * @return List of text lines
     */
    private static List<String> splitTextIntoLines(String text, int maxLength) {
        try {
            List<String> lines = new ArrayList<>();
            // Use ColorUtil for all color processing to ensure consistency
            String[] words = text.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                // Count visible characters using ColorUtil
                String wordWithoutColors = ColorUtil.stripAllColors(word);
                int visibleLength = wordWithoutColors.length();
                String lineWithoutColors = ColorUtil.stripAllColors(currentLine.toString());
                int currentVisibleLength = lineWithoutColors.length();

                if (currentVisibleLength + visibleLength + (currentVisibleLength > 0 ? 1 : 0) <= maxLength) {
                    if (currentLine.length() > 0) {
                        currentLine.append(" ");
                    }
                    currentLine.append(word);
                } else {
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder();
                    }

                    if (visibleLength > maxLength) {
                        // Handle long words by splitting them
                        String remainingWord = word;
                        while (ColorUtil.stripAllColors(remainingWord).length() > maxLength) {
                            // Find the best split point
                            int splitPos = findBestSplitPosition(remainingWord, maxLength);
                            lines.add(remainingWord.substring(0, splitPos));
                            remainingWord = remainingWord.substring(splitPos);
                        }
                        currentLine.append(remainingWord);
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
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[nonchat] Error splitting text into lines: {0}", e.getMessage());
            // Return a simple fallback
            List<String> fallback = new ArrayList<>();
            fallback.add(text);
            return fallback;
        }
    }

    /**
     * Finds the best position to split a word while preserving color codes
     * @param word The word to split
     * @param maxLength Maximum length for the split part
     * @return Best split position
     */
    private static int findBestSplitPosition(String word, int maxLength) {
        try {
            int visibleChars = 0;
            for (int i = 0; i < word.length(); i++) {
                // Check if current character is part of a color code
                if (word.charAt(i) == '§' && i + 1 < word.length()) {
                    i++; // Skip the next character (color code)
                    continue;
                }
                if (i + 2 < word.length() && word.charAt(i) == '&' && word.charAt(i + 1) == '#') {
                    i += 7; // Skip hex color code (#RRGGBB)
                    continue;
                }
                
                visibleChars++;
                if (visibleChars >= maxLength) {
                    return i + 1;
                }
            }
            return word.length();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "[nonchat] Error finding split position: {0}", e.getMessage());
            return Math.min(maxLength, word.length());
        }
    }

    /**
     * Updates the scale of existing chat bubble TextDisplay entities
     * @param bubbles List of TextDisplay entities to scale
     * @param overallScale The overall scale multiplier
     * @param scaleX The X axis scale multiplier
     * @param scaleY The Y axis scale multiplier
     * @param scaleZ The Z axis scale multiplier
     */
    public static void updateBubblesScale(List<TextDisplay> bubbles, double overallScale, double scaleX, double scaleY, double scaleZ) {
        try {
            if (bubbles != null) {
                bubbles.removeIf(bubble -> bubble == null || bubble.isDead());
                
                for (int i = 0; i < bubbles.size(); i++) {
                    try {
                        TextDisplay bubble = bubbles.get(i);
                        if (bubble != null && !bubble.isDead()) {
                            // Update scale using proper transformation with individual axis control
                            Vector3f scaleVector = new Vector3f(
                                (float) (overallScale * scaleX), 
                                (float) (overallScale * scaleY), 
                                (float) (overallScale * scaleZ)
                            );
                            Transformation transformation = bubble.getTransformation();
                            transformation.getScale().set(scaleVector.x, scaleVector.y, scaleVector.z);
                            bubble.setTransformation(transformation);
                        }
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.FINE, "[nonchat] Error updating text display {0} scale: {1}", new Object[]{i, e.getMessage()});
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "[nonchat] Error updating text displays scale: {0}", e.getMessage());
        }
    }

    /**
     * Updates the scale of a single chat bubble TextDisplay entity
     * @param bubble The TextDisplay to scale
     * @param overallScale The overall scale multiplier
     * @param scaleX The X axis scale multiplier
     * @param scaleY The Y axis scale multiplier
     * @param scaleZ The Z axis scale multiplier
     */
    public static void updateBubbleScale(TextDisplay bubble, double overallScale, double scaleX, double scaleY, double scaleZ) {
        try {
            if (bubble != null && !bubble.isDead()) {
                // Update scale using proper transformation with individual axis control
                Vector3f scaleVector = new Vector3f(
                    (float) (overallScale * scaleX), 
                    (float) (overallScale * scaleY), 
                    (float) (overallScale * scaleZ)
                );
                Transformation transformation = bubble.getTransformation();
                transformation.getScale().set(scaleVector.x, scaleVector.y, scaleVector.z);
                bubble.setTransformation(transformation);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "[nonchat] Error updating text display scale: {0}", e.getMessage());
        }
    }
    
    /**
     * Simple object pool implementation for TextDisplay entities
     */
    private static class ObjectPool<T> {
        private final List<T> pool = new ArrayList<>();
        private final int maxSize;
        
        public ObjectPool(int maxSize) {
            this.maxSize = maxSize;
        }
        
        public T acquire(Supplier<T> creator) {
            try {
                synchronized (pool) {
                    if (!pool.isEmpty()) {
                        T obj = pool.remove(pool.size() - 1);
                        if (obj instanceof TextDisplay display) {
                            if (display.isDead()) {
                                return creator.get();
                            }
                        }
                        return obj;
                    }
                }
                return creator.get();
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.FINE, "[nonchat] Error acquiring from pool: {0}", e.getMessage());
                return creator.get();
            }
        }
        
        public void release(T obj) {
            try {
                if (obj instanceof TextDisplay display) {
                    if (display.isDead()) {
                        return;
                    }
                    
                    display.text(Component.empty());
                    display.setCustomNameVisible(false);
                    
                    synchronized (pool) {
                        if (pool.size() < maxSize) {
                            pool.add(obj);
                        } else {
                            display.remove();
                        }
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.FINE, "[nonchat] Error releasing to pool: {0}", e.getMessage());
            }
        }
        
        public void clear() {
            try {
                synchronized (pool) {
                    pool.forEach(obj -> {
                        try {
                            if (obj instanceof TextDisplay display) {
                                display.remove();
                            }
                        } catch (Exception e) {
                            Bukkit.getLogger().log(Level.FINE, "[nonchat] Error removing text display from pool: {0}", e.getMessage());
                        }
                    });
                    pool.clear();
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.FINE, "[nonchat] Error clearing pool: {0}", e.getMessage());
            }
        }
    }
    
    /**
     * Clears the object pool and removes all entities
     */
    public static void clearPool() {
        try {
            displayPool.clear();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "[nonchat] Error clearing text display pool: {0}", e.getMessage());
        }
    }
}

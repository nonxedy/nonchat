package com.nonxedy.nonchat.api.model;

import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a mention in chat (@player, @group, @all, etc.).
 * Immutable object containing information about mention type, targets, etc.
 */
public class Mention {
    private final String originalText;
    private final String targetName;
    private final MentionType type;
    private final List<Player> targetPlayers;
    private final String formattedText;
    private final boolean allowed;
    
    /**
     * Creates a new mention instance.
     * 
     * @param originalText Original mention text
     * @param targetName Target name (without @ symbol)
     * @param type Mention type
     * @param targetPlayers List of mentioned players
     * @param formattedText Formatted mention text
     * @param allowed Whether mention is allowed
     */
    public Mention(String originalText, String targetName, MentionType type, 
                   List<Player> targetPlayers, String formattedText, boolean allowed) {
        this.originalText = originalText;
        this.targetName = targetName;
        this.type = type;
        this.targetPlayers = targetPlayers != null ? new ArrayList<>(targetPlayers) : new ArrayList<>();
        this.formattedText = formattedText != null ? formattedText : originalText;
        this.allowed = allowed;
    }
    
    /**
     * Creates a player mention.
     * 
     * @param originalText Original mention text
     * @param player Mentioned player
     * @return New mention instance
     */
    public static Mention player(String originalText, Player player) {
        return new Mention(originalText, player.getName(), MentionType.PLAYER, 
                          Collections.singletonList(player), null, true);
    }
    
    /**
     * Creates a group mention.
     * 
     * @param originalText Original mention text
     * @param groupName Group name
     * @param players Players in group
     * @return New mention instance
     */
    public static Mention group(String originalText, String groupName, List<Player> players) {
        return new Mention(originalText, groupName, MentionType.GROUP, players, null, true);
    }
    
    /**
     * Creates an everyone mention.
     * 
     * @param originalText Original mention text
     * @param players All online players
     * @return New mention instance
     */
    public static Mention everyone(String originalText, List<Player> players) {
        return new Mention(originalText, "everyone", MentionType.ALL, players, null, true);
    }
    
    /**
     * Creates a copy with new formatted text.
     * 
     * @param newFormattedText New formatted text
     * @return New mention instance
     */
    public Mention withFormattedText(String newFormattedText) {
        return new Mention(originalText, targetName, type, targetPlayers, newFormattedText, allowed);
    }
    
    /**
     * Creates a copy marked as denied.
     * 
     * @return New mention instance with allowed = false
     */
    public Mention deny() {
        return new Mention(originalText, targetName, type, targetPlayers, formattedText, false);
    }
    
    /**
     * Checks if specific player is mentioned.
     * 
     * @param player Player to check
     * @return true if player is mentioned
     */
    public boolean mentions(Player player) {
        return targetPlayers.contains(player);
    }
    
    /**
     * Checks if player with given name is mentioned.
     * 
     * @param playerName Player name to check
     * @return true if player is mentioned
     */
    public boolean mentionsByName(String playerName) {
        return targetPlayers.stream()
            .anyMatch(p -> p.getName().equalsIgnoreCase(playerName));
    }
    
    // Getters
    public String getOriginalText() { return originalText; }
    public String getTargetName() { return targetName; }
    public MentionType getType() { return type; }
    public List<Player> getTargetPlayers() { return Collections.unmodifiableList(targetPlayers); }
    public String getFormattedText() { return formattedText; }
    public boolean isAllowed() { return allowed; }
    
    /**
     * Mention types enumeration.
     */
    public enum MentionType {
        /** Individual player mention (@player) */
        PLAYER,
        
        /** Group mention (@group, @admin, @mod, etc.) */
        GROUP,
        
        /** Everyone mention (@all, @everyone) */
        ALL,
        
        /** Other mention types */
        OTHER
    }
}

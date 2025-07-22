package com.nonxedy.nonchat.util.special.ignore;

import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.nonxedy.nonchat.command.impl.IgnoreCommand;

/**
 * Manages player ignore lists for chat functionality
 * Tracks which players have ignored other players
 */
public class IgnoreManager {
    private final IgnoreCommand ignoreCommand;

    // Constructor to initialize with ignore command
    public IgnoreManager(IgnoreCommand ignoreCommand) {
        this.ignoreCommand = ignoreCommand;
    }
    
    /**
     * Checks if a player is ignoring another player
     * @param sender The player who might be ignoring
     * @param target The player who might be ignored
     * @return true if sender is ignoring target
     */
    public boolean isIgnoring(Player sender, Player target) {
        return ignoreCommand.isIgnoring(sender, target);
    }
    
    /**
     * Retrieves the set of players ignored by a specific player
     * @param player The player whose ignore list to retrieve
     * @return Set of UUIDs representing ignored players, empty set if none
     */
    public Set<UUID> getIgnoredPlayers(Player player) {
        return ignoreCommand.getIgnoredPlayers(player);
    }
    
    /**
     * Checks if a player is ignoring anyone
     * @param player The player to check
     * @return true if the player is ignoring at least one other player
     */
    public boolean isIgnoringAnyone(Player player) {
        return ignoreCommand.isIgnoringAnyone(player);
    }
}

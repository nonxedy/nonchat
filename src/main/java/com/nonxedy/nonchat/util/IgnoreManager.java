package com.nonxedy.nonchat.util;

import org.bukkit.entity.Player;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

/**
 * Manages player ignore lists for chat functionality
 * Tracks which players have ignored other players
 */
public class IgnoreManager {
    /** Map storing ignored player UUIDs for each player */
    private final Map<UUID, Set<UUID>> ignoredPlayers;

    // Constructor to initialize ignored players map
    public IgnoreManager(Map<UUID, Set<UUID>> ignoredPlayers) {
        this.ignoredPlayers = ignoredPlayers;
    }
    
    /**
     * Retrieves the set of players ignored by a specific player
     * @param player The player whose ignore list to retrieve
     * @return Set of UUIDs representing ignored players, empty set if none
     */
    public Set<UUID> getIgnoredPlayers(Player player) {
        return ignoredPlayers.getOrDefault(player.getUniqueId(), new HashSet<>());
    }
}

package com.nonxedy.nonchat.utils;

import org.bukkit.entity.Player;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

public class IgnoreManager {
    private final Map<UUID, Set<UUID>> ignoredPlayers;

    public IgnoreManager(Map<UUID, Set<UUID>> ignoredPlayers) {
        this.ignoredPlayers = ignoredPlayers;
    }

    public Set<UUID> getIgnoredPlayers(Player player) {
        return ignoredPlayers.getOrDefault(player.getUniqueId(), new HashSet<>());
    }
}

package com.nonxedy.nonchat.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nonxedy.nonchat.core.IndirectDeathTracker;

/**
 * Handles player cleanup when they leave the server.
 * Clears indirect death tracking data to prevent memory leaks.
 */
public class PlayerCleanupListener implements Listener {
    
    private final IndirectDeathTracker indirectDeathTracker;
    
    /**
     * Creates a new PlayerCleanupListener
     * @param indirectDeathTracker The indirect death tracker to clean up
     */
    public PlayerCleanupListener(IndirectDeathTracker indirectDeathTracker) {
        this.indirectDeathTracker = indirectDeathTracker;
    }
    
    /**
     * Handles player quit events and clears tracking data
     * Uses MONITOR priority to run after other plugins
     * @param event The player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (indirectDeathTracker != null) {
            indirectDeathTracker.clearPlayer(player);
        }
    }
}

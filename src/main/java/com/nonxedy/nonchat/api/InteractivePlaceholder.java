package com.nonxedy.nonchat.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

/**
 * Interface for interactive placeholders that can be configured through config.yml
 * Interactive placeholders are special text patterns that get replaced with interactive components
 * like clickable/hoverable text, items, or other dynamic content.
 */
public interface InteractivePlaceholder {

    /**
     * Gets the placeholder pattern that triggers this interactive placeholder.
     * For example: "[item]", "[ping]", "[stats]"
     *
     * @return The placeholder pattern as a string
     */
    @NotNull
    String getPlaceholder();

    /**
     * Gets the display name for this placeholder (used in config and documentation)
     *
     * @return The display name
     */
    @NotNull
    String getDisplayName();

    /**
     * Gets the description of what this placeholder does
     *
     * @return The description
     */
    @NotNull
    String getDescription();

    /**
     * Processes the placeholder and returns the interactive component
     *
     * @param player The player who triggered the placeholder
     * @param arguments Optional arguments passed to the placeholder (e.g., [stats:health] would pass "health")
     * @return The interactive component to replace the placeholder
     */
    @NotNull
    Component process(Player player, String... arguments);

    /**
     * Checks if this placeholder is enabled
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Sets the enabled state of this placeholder
     *
     * @param enabled The new enabled state
     */
    void setEnabled(boolean enabled);

    /**
     * Gets the permission required to use this placeholder
     * If empty or null, no permission is required
     *
     * @return The permission string or null/empty
     */
    String getPermission();

    /**
     * Sets the permission required to use this placeholder
     *
     * @param permission The permission string or null/empty
     */
    void setPermission(String permission);
}

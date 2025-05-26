package com.nonxedy.nonchat.api.annotation;

import java.lang.annotation.*;

/**
 * Annotation for chat message filters.
 * Classes with this annotation will be used to filter messages in specified channels.
 * 
 * Example usage:
 * <pre>
 * {@code 
 * @ChatFilter(channels = {"global", "local"}, priority = 50)
 * public class ProfanityFilter implements MessageFilter {
 *     @Override
 *     public boolean shouldFilter(Player sender, String message) {
 *         return containsProfanity(message);
 *     }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ChatFilter {
    /**
     * Channels to apply filter to (empty = all channels)
     */
    String[] channels() default {};
    
    /**
     * Filter priority (lower = higher priority)
     */
    int priority() default 0;
    
    /**
     * Silent filtering (no notification to player)
     */
    boolean silent() default false;
    
    /**
     * Permission to bypass filter
     */
    String bypassPermission() default "";
}

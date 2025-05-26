package com.nonxedy.nonchat.api.annotation;

import java.lang.annotation.*;

/**
 * Annotation for command handler methods.
 * Marks a method as a command execution handler.
 * 
 * Example usage:
 * <pre>
 * {@code 
 * @CommandHandler(usage = "/msg <player> <message>")
 * public void execute(CommandSender sender, String[] args) {
 *     // Implementation
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandHandler {
    /**
     * Minimum arguments required
     */
    int minArgs() default 0;
    
    /**
     * Maximum arguments allowed (-1 for unlimited)
     */
    int maxArgs() default -1;
    
    /**
     * Usage message
     */
    String usage() default "";
    
    /**
     * Handler priority (lower = higher priority)
     */
    int priority() default 0;
}

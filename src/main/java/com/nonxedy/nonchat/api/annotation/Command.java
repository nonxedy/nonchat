package com.nonxedy.nonchat.api.annotation;

import java.lang.annotation.*;

/**
 * Annotation for command classes.
 * Marks a class as a command handler with basic configuration.
 * 
 * Example usage:
 * <pre>
 * {@code 
 * @Command(name = "message", aliases = {"msg", "tell"}, permission = "nonchat.message")
 * public class MessageCommand {
 *     // Command implementation
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {
    /**
     * Command name (required)
     */
    String name();
    
    /**
     * Command aliases
     */
    String[] aliases() default {};
    
    /**
     * Command description
     */
    String description() default "";
    
    /**
     * Required permission
     */
    String permission() default "";
    
    /**
     * Player only command
     */
    boolean playerOnly() default false;
}
